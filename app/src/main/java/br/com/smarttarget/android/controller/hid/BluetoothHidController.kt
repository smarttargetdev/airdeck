package br.com.smarttarget.android.controller.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import br.com.smarttarget.android.controller.domain.PowerSaverPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class BluetoothHidController(private val context: Context) : HidTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var isAppRegistered = false
    private var isReceiverRegistered = false
    private var powerSaverEnabled = false
    private var reportCounter = 0L

    private val _connectionState = MutableStateFlow<HidConnectionState>(HidConnectionState.Registering)
    override val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    private val _packetStats = MutableStateFlow(HidPacketStats())
    override val packetStats: StateFlow<HidPacketStats> = _packetStats.asStateFlow()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED,
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refreshState()
            }
        }
    }

    override fun start() {
        registerReceiverIfNeeded()
        refreshState()
    }

    override fun stop() {
        if (isReceiverRegistered) {
            runCatching { context.unregisterReceiver(bluetoothReceiver) }
            isReceiverRegistered = false
        }
        unregisterHidProfile()
    }

    override fun requiredRuntimePermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            emptyList()
        }
    }

    fun missingPermissions(): List<String> {
        return requiredRuntimePermissions().filter { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
    }

    override fun setPowerSaverMode(enabled: Boolean) {
        if (powerSaverEnabled == enabled) return
        powerSaverEnabled = enabled
        // Re-register SDP/QoS when idle to apply low-power Bluetooth QoS latency parameters
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            connectedDevice == null &&
            hidDevice != null &&
            isAppRegistered &&
            missingPermissions().isEmpty()
        ) {
            reregisterForQosChange()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private fun reregisterForQosChange() {
        val proxy = hidDevice ?: return
        runCatching {
            proxy.unregisterApp()
            isAppRegistered = false
            registerHidAppOnProxy(proxy)
        }
    }

    override fun refreshState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            _connectionState.value = HidConnectionState.Unsupported(
                "O perfil Bluetooth HID nativo exige Android 9 (API 28) ou superior."
            )
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionState.value = HidConnectionState.Unsupported(
                "Este dispositivo ou emulador não possui hardware Bluetooth disponível."
            )
            return
        }

        val missing = missingPermissions()
        if (missing.isNotEmpty()) {
            _connectionState.value = HidConnectionState.PermissionRequired(missing)
            return
        }

        if (!adapter.isEnabled) {
            connectedDevice = null
            _connectionState.value = HidConnectionState.BluetoothDisabled
            return
        }

        if (hidDevice == null || !isAppRegistered) {
            _connectionState.value = HidConnectionState.Registering
            registerHidProfile()
            return
        }

        val currentHost = connectedDevice
        if (currentHost != null) {
            _connectionState.value = HidConnectionState.Connected(currentHost.toBondedHostDevice())
        } else {
            _connectionState.value = HidConnectionState.Disconnected(
                bondedDevices = queryBondedDevices(),
                isDiscoverable = isDiscoverableSafely(adapter)
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun connect(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (missingPermissions().isNotEmpty()) {
            refreshState()
            return
        }
        val adapter = bluetoothAdapter ?: return
        val proxy = hidDevice
        if (proxy == null || !isAppRegistered) {
            refreshState()
            return
        }

        val targetDevice = try {
            adapter.bondedDevices?.firstOrNull { it.address == address }
                ?: adapter.getRemoteDevice(address)
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: SecurityException) {
            refreshState()
            return
        }

        if (targetDevice == null) {
            _connectionState.value = HidConnectionState.Error(
                message = "Dispositivo não encontrado na lista de pareados.",
                bondedDevices = queryBondedDevices()
            )
            return
        }

        _connectionState.value = HidConnectionState.Connecting(targetDevice.toBondedHostDevice())
        try {
            val initiated = proxy.connect(targetDevice)
            if (!initiated) {
                _connectionState.value = HidConnectionState.Error(
                    message = "Não foi possível iniciar a conexão HID. Tente conectar pelo dispositivo remoto.",
                    bondedDevices = queryBondedDevices()
                )
            }
        } catch (_: SecurityException) {
            refreshState()
        }
    }

    @SuppressLint("MissingPermission")
    override fun disconnect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val proxy = hidDevice ?: return
        val host = connectedDevice ?: return
        try {
            proxy.disconnect(host)
        } catch (_: SecurityException) {
            refreshState()
        }
    }

    override fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int) {
        val report = HidReportEncoder.encodeMouseReport(buttons, dx, dy, wheel)
        sendReport(HidConstants.REPORT_ID_MOUSE, report, "Mouse")
    }

    override fun sendKeyCombo(modifiers: Int, keyCode: Int) {
        scope.launch {
            sendKeyboardReport(modifiers, keyCode)
            delay(if (powerSaverEnabled) 18 else 14)
            sendKeyboardReport(HidConstants.MOD_NONE, HidConstants.KEY_NONE)
        }
    }

    override fun sendTextStrokes(strokes: List<HidKeyStroke>) {
        if (strokes.isEmpty()) return
        scope.launch {
            val pressMs = if (powerSaverEnabled) 16L else 12L
            val gapMs = if (powerSaverEnabled) 14L else 10L
            for (stroke in strokes) {
                sendKeyboardReport(stroke.modifiers, stroke.keyCode)
                delay(pressMs)
                sendKeyboardReport(HidConstants.MOD_NONE, HidConstants.KEY_NONE)
                delay(gapMs)
            }
        }
    }

    override fun sendConsumerPulse(mask: Int) {
        scope.launch {
            sendConsumerReport(mask)
            delay(20)
            sendConsumerReport(HidConstants.CONSUMER_NONE)
        }
    }

    override fun releaseAll() {
        sendMouseReport(HidConstants.MOUSE_NONE, 0, 0, 0)
        sendKeyboardReport(HidConstants.MOD_NONE, HidConstants.KEY_NONE)
        sendConsumerReport(HidConstants.CONSUMER_NONE)
    }

    private fun sendKeyboardReport(modifiers: Int, keyCode: Int) {
        val report = HidReportEncoder.encodeKeyboardReport(modifiers, keyCode)
        sendReport(HidConstants.REPORT_ID_KEYBOARD, report, "Teclado")
    }

    private fun sendConsumerReport(mask: Int) {
        val report = HidReportEncoder.encodeConsumerReport(mask)
        sendReport(HidConstants.REPORT_ID_CONSUMER, report, "Mídia")
    }

    @SuppressLint("MissingPermission")
    private fun sendReport(reportId: Int, data: ByteArray, label: String) {
        var delivered = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            missingPermissions().isEmpty()
        ) {
            val proxy = hidDevice
            val host = connectedDevice
            if (proxy != null && host != null && isAppRegistered) {
                delivered = try {
                    proxy.sendReport(host, reportId, data)
                } catch (_: SecurityException) {
                    false
                }
            }
        }
        reportCounter += 1
        val policy = PowerSaverPolicy.resolve(powerSaverEnabled)
        if (reportCounter == 1L || reportCounter % policy.telemetryUiBatchSize == 0L) {
            _packetStats.value = HidPacketStats(
                totalReportsSent = reportCounter,
                lastReportLabel = "$label (ID $reportId)",
                lastBytesHex = HidReportEncoder.formatHex(data),
                wasDeliveredToHost = delivered
            )
        }
    }

    private fun registerReceiverIfNeeded() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(bluetoothReceiver, filter)
        }
        isReceiverRegistered = true
    }

    private fun registerHidProfile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val adapter = bluetoothAdapter ?: return
        if (hidDevice != null) {
            registerHidAppOnProxy(hidDevice!!)
            return
        }
        adapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    if (profile == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                        hidDevice = proxy
                        registerHidAppOnProxy(proxy)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidDevice = null
                        isAppRegistered = false
                        connectedDevice = null
                        refreshState()
                    }
                }
            },
            BluetoothProfile.HID_DEVICE
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    private fun registerHidAppOnProxy(proxy: BluetoothHidDevice) {
        if (missingPermissions().isNotEmpty()) {
            refreshState()
            return
        }
        val policy = PowerSaverPolicy.resolve(powerSaverEnabled)
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Smarties Controller",
            "Mouse, Teclado e Controle de Mídia Bluetooth",
            "Smarties",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            HidConstants.HID_REPORT_DESCRIPTOR
        )
        val outQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            policy.qosTokenRateBytesPerSec,
            9,
            0,
            policy.qosLatencyMicros,
            BluetoothHidDeviceAppQosSettings.MAX
        )

        val callback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                scope.launch {
                    isAppRegistered = registered
                    if (!registered) {
                        connectedDevice = null
                    }
                    refreshState()
                }
            }

            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                scope.launch {
                    when (state) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            connectedDevice = device
                            refreshState()
                        }
                        BluetoothProfile.STATE_CONNECTING -> {
                            if (device != null) {
                                _connectionState.value =
                                    HidConnectionState.Connecting(device.toBondedHostDevice())
                            }
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            if (connectedDevice?.address == device?.address) {
                                connectedDevice = null
                            }
                            refreshState()
                        }
                    }
                }
            }
        }

        try {
            proxy.registerApp(sdp, null, outQos, callbackExecutor, callback)
        } catch (_: SecurityException) {
            refreshState()
        }
    }

    @SuppressLint("MissingPermission")
    private fun unregisterHidProfile() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val proxy = hidDevice ?: return
        try {
            if (isAppRegistered && missingPermissions().isEmpty()) {
                proxy.unregisterApp()
            }
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
        } catch (_: SecurityException) {
            // Ignore when permission already revoked
        } finally {
            hidDevice = null
            isAppRegistered = false
            connectedDevice = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun queryBondedDevices(): List<BondedHostDevice> {
        if (missingPermissions().isNotEmpty()) return emptyList()
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices
                ?.map { it.toBondedHostDevice() }
                ?.sortedBy { it.name.lowercase() }
                ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDiscoverableSafely(adapter: BluetoothAdapter): Boolean {
        if (missingPermissions().isNotEmpty()) return false
        return try {
            adapter.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
        } catch (_: SecurityException) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.toBondedHostDevice(): BondedHostDevice {
        val deviceName = try {
            name?.takeIf { it.isNotBlank() } ?: address
        } catch (_: SecurityException) {
            address
        }
        val majorClass = try {
            bluetoothClass?.majorDeviceClass
        } catch (_: SecurityException) {
            null
        }
        val type = when (majorClass) {
            BluetoothClass.Device.Major.COMPUTER -> HostDeviceType.COMPUTER
            BluetoothClass.Device.Major.AUDIO_VIDEO -> HostDeviceType.TV_OR_MEDIA
            BluetoothClass.Device.Major.PHONE -> HostDeviceType.PHONE_OR_TABLET
            else -> HostDeviceType.OTHER
        }
        return BondedHostDevice(
            name = deviceName,
            address = address,
            type = type
        )
    }
}
