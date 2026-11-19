package br.com.smarttarget.android.airdeck.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import br.com.smarttarget.android.airdeck.MainActivity
import br.com.smarttarget.android.airdeck.R
import br.com.smarttarget.android.airdeck.domain.BluetoothReconnectionBridge
import br.com.smarttarget.android.airdeck.domain.ReconnectionPolicy
import br.com.smarttarget.android.airdeck.domain.ReconnectionState
import br.com.smarttarget.android.airdeck.hid.BluetoothTransportRegistry
import br.com.smarttarget.android.airdeck.hid.BondedHostDevice
import br.com.smarttarget.android.airdeck.hid.DiscoveryState
import br.com.smarttarget.android.airdeck.hid.HidConnectionState
import br.com.smarttarget.android.airdeck.hid.HidPacketStats
import br.com.smarttarget.android.airdeck.hid.HidTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Bound & foreground-capable Service responsible for orchestrating Bluetooth device discovery,
 * pairing (bonding), HID profile connection state, and automatic exponential-backoff reconnection
 * on unexpected host drops for Air Deck.
 */
class BluetoothConnectionManagerService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothConnectionManagerService = this@BluetoothConnectionManagerService
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var transport: HidTransport
    private var stateObserverJob: Job? = null
    private var reconnectJob: Job? = null
    private var isForegroundActive = false

    private var lastConnectedHost: BondedHostDevice? = null
    private var suppressAutoReconnect = false

    private val _reconnectionState = MutableStateFlow<ReconnectionState>(ReconnectionState.Idle)
    val reconnectionState: StateFlow<ReconnectionState> = _reconnectionState.asStateFlow()

    internal var backoffScheduleMs: List<Long> = ReconnectionPolicy.backoffDelaysMs
    internal var connectAttemptTimeoutMs: Long = 8_000L

    val connectionState: StateFlow<HidConnectionState>
        get() = transport.connectionState

    val discoveryState: StateFlow<DiscoveryState>
        get() = transport.discoveryState

    val packetStats: StateFlow<HidPacketStats>
        get() = transport.packetStats

    fun getTransport(): HidTransport = transport

    override fun onCreate() {
        super.onCreate()
        transport = BluetoothTransportRegistry.getOrCreate(applicationContext)
        transport.start()
        BluetoothReconnectionBridge.registerCancelHandler { cancelAutoReconnect() }
        ensureNotificationChannel()
        observeConnectionAndDiscovery()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> promoteToForegroundIfPermitted()
            ACTION_REFRESH -> transport.refreshState()
            ACTION_START_DISCOVERY -> transport.startDiscovery()
            ACTION_STOP_DISCOVERY -> transport.stopDiscovery()
            ACTION_PAIR_DEVICE -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (!address.isNullOrBlank()) {
                    transport.pairDevice(address)
                }
            }
            ACTION_CONNECT_DEVICE -> {
                val address = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (!address.isNullOrBlank()) {
                    connect(address)
                }
            }
            ACTION_DISCONNECT -> disconnect()
            ACTION_CANCEL_RECONNECT -> cancelAutoReconnect()
            ACTION_STOP_SERVICE -> {
                cancelAutoReconnect()
                demoteForeground()
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        reconnectJob?.cancel()
        reconnectJob = null
        stateObserverJob?.cancel()
        BluetoothReconnectionBridge.registerCancelHandler(null)
        publishReconnectionState(ReconnectionState.Idle)
        demoteForeground()
        transport.stop()
        serviceScope.cancel()
        super.onDestroy()
    }

    fun startDiscovery() = transport.startDiscovery()

    fun stopDiscovery() = transport.stopDiscovery()

    fun pairDevice(address: String) = transport.pairDevice(address)

    fun connect(address: String) {
        suppressAutoReconnect = false
        if (lastConnectedHost?.address != address) {
            reconnectJob?.cancel()
            reconnectJob = null
            lastConnectedHost = null
            publishReconnectionState(ReconnectionState.Idle)
        }
        transport.connect(address)
    }

    fun disconnect() {
        suppressAutoReconnect = true
        lastConnectedHost = null
        reconnectJob?.cancel()
        reconnectJob = null
        publishReconnectionState(ReconnectionState.Idle)
        transport.disconnect()
    }

    fun cancelAutoReconnect() {
        suppressAutoReconnect = false
        lastConnectedHost = null
        reconnectJob?.cancel()
        reconnectJob = null
        publishReconnectionState(ReconnectionState.Idle)
        val currentConn = transport.connectionState.value
        if (currentConn is HidConnectionState.Connecting) {
            transport.refreshState()
        }
        if (currentConn !is HidConnectionState.Connected && !transport.discoveryState.value.isDiscovering) {
            demoteForeground()
        }
    }

    fun refreshState() = transport.refreshState()

    fun setPowerSaverMode(enabled: Boolean) = transport.setPowerSaverMode(enabled)

    private fun observeConnectionAndDiscovery() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            combine(
                transport.connectionState,
                transport.discoveryState,
                _reconnectionState
            ) { conn, discovery, reconnect ->
                Triple(conn, discovery, reconnect)
            }.collect { (conn, discovery, reconnect) ->
                when (conn) {
                    is HidConnectionState.Connected -> {
                        lastConnectedHost = conn.device
                        suppressAutoReconnect = false
                        reconnectJob?.cancel()
                        reconnectJob = null
                        if (reconnect !is ReconnectionState.Idle) {
                            publishReconnectionState(ReconnectionState.Idle)
                        }
                        promoteToForegroundIfPermitted(conn, discovery, ReconnectionState.Idle)
                    }

                    is HidConnectionState.Connecting -> {
                        promoteToForegroundIfPermitted(conn, discovery, reconnect)
                    }

                    is HidConnectionState.Disconnected -> {
                        val userEnded = conn.wasUserInitiated || suppressAutoReconnect
                        if (userEnded) {
                            suppressAutoReconnect = false
                            lastConnectedHost = null
                            reconnectJob?.cancel()
                            reconnectJob = null
                            if (reconnect !is ReconnectionState.Idle) {
                                publishReconnectionState(ReconnectionState.Idle)
                            }
                            handleIdleForeground(conn, discovery, ReconnectionState.Idle)
                        } else {
                            val droppedHost = lastConnectedHost
                            if (droppedHost != null && reconnectJob?.isActive != true) {
                                startAutoReconnect(droppedHost)
                            } else if (reconnect is ReconnectionState.Reconnecting) {
                                promoteToForegroundIfPermitted(conn, discovery, reconnect)
                            } else {
                                handleIdleForeground(conn, discovery, reconnect)
                            }
                        }
                    }

                    is HidConnectionState.Error -> {
                        val droppedHost = lastConnectedHost
                        if (!suppressAutoReconnect && droppedHost != null && reconnectJob?.isActive != true) {
                            startAutoReconnect(droppedHost)
                        } else if (reconnect is ReconnectionState.Reconnecting) {
                            promoteToForegroundIfPermitted(conn, discovery, reconnect)
                        } else {
                            handleIdleForeground(conn, discovery, reconnect)
                        }
                    }

                    is HidConnectionState.BluetoothDisabled,
                    is HidConnectionState.PermissionRequired,
                    is HidConnectionState.Unsupported -> {
                        suppressAutoReconnect = false
                        lastConnectedHost = null
                        reconnectJob?.cancel()
                        reconnectJob = null
                        if (reconnect !is ReconnectionState.Idle) {
                            publishReconnectionState(ReconnectionState.Idle)
                        }
                        demoteForeground()
                    }

                    HidConnectionState.Registering -> {
                        if (reconnect is ReconnectionState.Reconnecting) {
                            promoteToForegroundIfPermitted(conn, discovery, reconnect)
                        }
                    }
                }
            }
        }
    }

    private fun startAutoReconnect(target: BondedHostDevice) {
        reconnectJob?.cancel()
        reconnectJob = serviceScope.launch {
            for (attempt in 1..ReconnectionPolicy.MAX_ATTEMPTS) {
                if (!isActive || suppressAutoReconnect || lastConnectedHost?.address != target.address) {
                    return@launch
                }

                val state = ReconnectionState.Reconnecting(
                    device = target,
                    attempt = attempt,
                    maxAttempts = ReconnectionPolicy.MAX_ATTEMPTS
                )
                publishReconnectionState(state)
                promoteToForegroundIfPermitted(
                    transport.connectionState.value,
                    transport.discoveryState.value,
                    state
                )

                val waitMs = ReconnectionPolicy.delayForAttempt(attempt, backoffScheduleMs)
                delay(waitMs)

                if (!isActive || suppressAutoReconnect || lastConnectedHost?.address != target.address) {
                    return@launch
                }

                when (transport.connectionState.value) {
                    is HidConnectionState.Connected -> {
                        publishReconnectionState(ReconnectionState.Idle)
                        return@launch
                    }
                    is HidConnectionState.BluetoothDisabled,
                    is HidConnectionState.PermissionRequired,
                    is HidConnectionState.Unsupported -> {
                        lastConnectedHost = null
                        publishReconnectionState(ReconnectionState.Idle)
                        demoteForeground()
                        return@launch
                    }
                    else -> Unit
                }

                transport.connect(target.address)

                val outcome = withTimeoutOrNull(connectAttemptTimeoutMs) {
                    transport.connectionState.first { it !is HidConnectionState.Connecting }
                }

                when (outcome) {
                    is HidConnectionState.Connected -> {
                        publishReconnectionState(ReconnectionState.Idle)
                        return@launch
                    }
                    is HidConnectionState.BluetoothDisabled,
                    is HidConnectionState.PermissionRequired,
                    is HidConnectionState.Unsupported -> {
                        lastConnectedHost = null
                        publishReconnectionState(ReconnectionState.Idle)
                        demoteForeground()
                        return@launch
                    }
                    is HidConnectionState.Disconnected -> {
                        if (outcome.wasUserInitiated) {
                            lastConnectedHost = null
                            publishReconnectionState(ReconnectionState.Idle)
                            demoteForeground()
                            return@launch
                        }
                    }
                    else -> Unit
                }
            }

            // All 5 attempts exhausted without re-establishing connection
            if (transport.connectionState.value !is HidConnectionState.Connected) {
                lastConnectedHost = null
                publishReconnectionState(ReconnectionState.Idle)
                if (!transport.discoveryState.value.isDiscovering) {
                    demoteForeground()
                }
            }
        }
    }

    private fun handleIdleForeground(
        conn: HidConnectionState,
        discovery: DiscoveryState,
        reconnect: ReconnectionState
    ) {
        if (!isForegroundActive) return
        if (discovery.isDiscovering) {
            updateNotification(conn, discovery, reconnect)
        } else {
            demoteForeground()
        }
    }

    private fun publishReconnectionState(newState: ReconnectionState) {
        _reconnectionState.value = newState
        BluetoothReconnectionBridge.updateState(newState)
    }

    private fun promoteToForegroundIfPermitted(
        conn: HidConnectionState = transport.connectionState.value,
        discovery: DiscoveryState = transport.discoveryState.value,
        reconnect: ReconnectionState = _reconnectionState.value
    ) {
        if (conn is HidConnectionState.PermissionRequired ||
            conn is HidConnectionState.BluetoothDisabled ||
            conn is HidConnectionState.Unsupported
        ) {
            return
        }
        val notification = buildNotification(conn, discovery, reconnect)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundActive = true
        } catch (_: Exception) {
            // Gracefully stay bound without foreground promotion if OS disallows FGS in background/test
            isForegroundActive = false
        }
    }

    private fun updateNotification(
        conn: HidConnectionState,
        discovery: DiscoveryState,
        reconnect: ReconnectionState
    ) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            manager.notify(NOTIFICATION_ID, buildNotification(conn, discovery, reconnect))
        }
    }

    private fun demoteForeground() {
        if (!isForegroundActive) return
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        isForegroundActive = false
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.service_channel_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(
        conn: HidConnectionState,
        discovery: DiscoveryState,
        reconnect: ReconnectionState
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            100,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = when {
            reconnect is ReconnectionState.Reconnecting ->
                getString(
                    R.string.service_notification_reconnecting,
                    reconnect.device.name,
                    reconnect.attempt,
                    reconnect.maxAttempts
                )
            conn is HidConnectionState.Connected ->
                getString(R.string.service_notification_connected, conn.device.name)
            conn is HidConnectionState.Connecting ->
                getString(R.string.service_notification_connecting, conn.device.name)
            discovery.isDiscovering ->
                getString(R.string.service_notification_discovering)
            else ->
                getString(R.string.service_notification_ready)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (reconnect is ReconnectionState.Reconnecting) {
            val cancelIntent = Intent(this, BluetoothConnectionManagerService::class.java).apply {
                action = ACTION_CANCEL_RECONNECT
            }
            val cancelPendingIntent = PendingIntent.getService(
                this,
                102,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                0,
                getString(R.string.service_notification_cancel),
                cancelPendingIntent
            )
        } else if (conn is HidConnectionState.Connected) {
            val disconnectIntent = Intent(this, BluetoothConnectionManagerService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            val disconnectPendingIntent = PendingIntent.getService(
                this,
                101,
                disconnectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                0,
                getString(R.string.service_notification_disconnect),
                disconnectPendingIntent
            )
        }

        return builder.build()
    }

    companion object {
        const val CHANNEL_ID = "air_deck_bt_hid"
        const val NOTIFICATION_ID = 4201

        const val ACTION_START_FOREGROUND = "br.com.smarttarget.android.airdeck.action.START_FOREGROUND"
        const val ACTION_REFRESH = "br.com.smarttarget.android.airdeck.action.REFRESH"
        const val ACTION_START_DISCOVERY = "br.com.smarttarget.android.airdeck.action.START_DISCOVERY"
        const val ACTION_STOP_DISCOVERY = "br.com.smarttarget.android.airdeck.action.STOP_DISCOVERY"
        const val ACTION_PAIR_DEVICE = "br.com.smarttarget.android.airdeck.action.PAIR_DEVICE"
        const val ACTION_CONNECT_DEVICE = "br.com.smarttarget.android.airdeck.action.CONNECT_DEVICE"
        const val ACTION_DISCONNECT = "br.com.smarttarget.android.airdeck.action.DISCONNECT"
        const val ACTION_CANCEL_RECONNECT = "br.com.smarttarget.android.airdeck.action.CANCEL_RECONNECT"
        const val ACTION_STOP_SERVICE = "br.com.smarttarget.android.airdeck.action.STOP_SERVICE"

        const val EXTRA_DEVICE_ADDRESS = "extra_device_address"

        fun createBindIntent(context: Context): Intent =
            Intent(context, BluetoothConnectionManagerService::class.java)

        fun createActionIntent(context: Context, action: String, address: String? = null): Intent =
            Intent(context, BluetoothConnectionManagerService::class.java).apply {
                this.action = action
                if (address != null) {
                    putExtra(EXTRA_DEVICE_ADDRESS, address)
                }
            }
    }
}
