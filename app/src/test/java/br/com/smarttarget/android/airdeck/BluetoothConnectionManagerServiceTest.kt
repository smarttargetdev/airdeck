package br.com.smarttarget.android.airdeck

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import br.com.smarttarget.android.airdeck.domain.BluetoothReconnectionBridge
import br.com.smarttarget.android.airdeck.domain.ReconnectionPolicy
import br.com.smarttarget.android.airdeck.domain.ReconnectionState
import br.com.smarttarget.android.airdeck.hid.BluetoothTransportRegistry
import br.com.smarttarget.android.airdeck.hid.BondedHostDevice
import br.com.smarttarget.android.airdeck.hid.DeviceBondState
import br.com.smarttarget.android.airdeck.hid.DiscoveredBluetoothDevice
import br.com.smarttarget.android.airdeck.hid.DiscoveryState
import br.com.smarttarget.android.airdeck.hid.HidConnectionState
import br.com.smarttarget.android.airdeck.hid.HidKeyStroke
import br.com.smarttarget.android.airdeck.hid.HidPacketStats
import br.com.smarttarget.android.airdeck.hid.HidTransport
import br.com.smarttarget.android.airdeck.hid.HostDeviceType
import br.com.smarttarget.android.airdeck.service.BluetoothConnectionManagerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BluetoothConnectionManagerServiceTest {

    private lateinit var fakeTransport: FakeBluetoothHidTransport
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        fakeTransport = FakeBluetoothHidTransport()
        BluetoothTransportRegistry.setForTesting(fakeTransport)
        BluetoothReconnectionBridge.resetForTesting()
    }

    @After
    fun tearDown() {
        BluetoothReconnectionBridge.resetForTesting()
        BluetoothTransportRegistry.setForTesting(null)
    }

    @Test
    fun `service binds cleanly and manages discovery pairing and hid connection`() {
        val controller = Robolectric.buildService(BluetoothConnectionManagerService::class.java)
        val service = controller.create().get()
        assertTrue(fakeTransport.started)

        val binder = service.onBind(BluetoothConnectionManagerService.createBindIntent(context))
            as BluetoothConnectionManagerService.LocalBinder
        assertEquals(service, binder.getService())

        service.startDiscovery()
        assertTrue(service.discoveryState.value.isDiscovering)

        val targetAddress = "AA:BB:CC:DD:EE:FF"
        fakeTransport.simulateDeviceFound(
            DiscoveredBluetoothDevice(
                name = "Desktop Studio",
                address = targetAddress,
                type = HostDeviceType.COMPUTER,
                bondState = DeviceBondState.NONE,
                rssi = -54
            )
        )
        assertEquals(1, service.discoveryState.value.discoveredDevices.size)

        service.pairDevice(targetAddress)
        assertFalse(service.discoveryState.value.isDiscovering)
        assertEquals(targetAddress, service.discoveryState.value.pairingDeviceAddress)

        fakeTransport.simulateBonded(targetAddress, "Desktop Studio")
        service.connect(targetAddress)
        assertTrue(service.connectionState.value is HidConnectionState.Connected)

        service.disconnect()
        assertTrue(service.connectionState.value is HidConnectionState.Disconnected)
        assertEquals(ReconnectionState.Idle, service.reconnectionState.value)

        controller.destroy()
        assertTrue(fakeTransport.stopped)
    }

    @Test
    fun `start commands route discovery, pairing, connect, disconnect, and cancel reconnect actions`() {
        val controller = Robolectric.buildService(BluetoothConnectionManagerService::class.java)
        val service = controller.create().get()

        service.onStartCommand(
            BluetoothConnectionManagerService.createActionIntent(
                context,
                BluetoothConnectionManagerService.ACTION_START_DISCOVERY
            ),
            0,
            1
        )
        assertTrue(service.discoveryState.value.isDiscovering)

        service.onStartCommand(
            BluetoothConnectionManagerService.createActionIntent(
                context,
                BluetoothConnectionManagerService.ACTION_PAIR_DEVICE,
                "11:22:33:44:55:66"
            ),
            0,
            2
        )
        assertEquals("11:22:33:44:55:66", fakeTransport.lastPairedAddress)

        service.onStartCommand(
            BluetoothConnectionManagerService.createActionIntent(
                context,
                BluetoothConnectionManagerService.ACTION_CONNECT_DEVICE,
                "11:22:33:44:55:66"
            ),
            0,
            3
        )
        assertEquals("11:22:33:44:55:66", fakeTransport.lastConnectedAddress)

        service.onStartCommand(
            BluetoothConnectionManagerService.createActionIntent(
                context,
                BluetoothConnectionManagerService.ACTION_DISCONNECT
            ),
            0,
            4
        )
        assertEquals(1, fakeTransport.disconnectCalls)
    }

    @Test
    fun `unexpected drop triggers exponential backoff auto-reconnect and recovers on retry`() {
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 30_000L), ReconnectionPolicy.backoffDelaysMs)

        val controller = Robolectric.buildService(BluetoothConnectionManagerService::class.java)
        val service = controller.create().get()
        service.backoffScheduleMs = listOf(10L, 20L, 40L, 80L, 100L)

        val hostAddress = "AA:BB:CC:11:22:33"
        service.connect(hostAddress)
        shadowOf(Looper.getMainLooper()).idle()
        assertTrue(service.connectionState.value is HidConnectionState.Connected)

        // First reconnect attempt fails, second succeeds
        fakeTransport.failNextConnectCount = 1
        fakeTransport.connectCalls = 0
        fakeTransport.simulateUnexpectedDrop()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5))
        assertTrue(service.reconnectionState.value is ReconnectionState.Reconnecting)
        assertTrue(BluetoothReconnectionBridge.state.value is ReconnectionState.Reconnecting)

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals(2, fakeTransport.connectCalls)
        assertTrue(service.connectionState.value is HidConnectionState.Connected)
        assertEquals(ReconnectionState.Idle, service.reconnectionState.value)
        assertEquals(ReconnectionState.Idle, BluetoothReconnectionBridge.state.value)
    }

    @Test
    fun `exhausted reconnection attempts and user cancel both stop cleanly`() {
        val controller = Robolectric.buildService(BluetoothConnectionManagerService::class.java)
        val service = controller.create().get()
        service.backoffScheduleMs = listOf(10L, 10L, 10L, 10L, 10L)

        val hostAddress = "AA:BB:CC:44:55:66"
        service.connect(hostAddress)
        shadowOf(Looper.getMainLooper()).idle()

        // Exhaust all 5 attempts
        fakeTransport.failNextConnectCount = 10
        fakeTransport.connectCalls = 0
        fakeTransport.simulateUnexpectedDrop()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
        assertEquals(ReconnectionPolicy.MAX_ATTEMPTS, fakeTransport.connectCalls)
        assertEquals(ReconnectionState.Idle, service.reconnectionState.value)
        assertTrue(service.connectionState.value !is HidConnectionState.Connected)

        // Reconnect again, then cancel via ACTION_CANCEL_RECONNECT mid-flight
        fakeTransport.failNextConnectCount = 0
        service.connect(hostAddress)
        shadowOf(Looper.getMainLooper()).idle()
        fakeTransport.failNextConnectCount = 10
        fakeTransport.connectCalls = 0
        fakeTransport.simulateUnexpectedDrop()

        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5))
        assertTrue(service.reconnectionState.value is ReconnectionState.Reconnecting)

        service.onStartCommand(
            BluetoothConnectionManagerService.createActionIntent(
                context,
                BluetoothConnectionManagerService.ACTION_CANCEL_RECONNECT
            ),
            0,
            5
        )
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals(ReconnectionState.Idle, service.reconnectionState.value)
        assertEquals(0, fakeTransport.connectCalls)

        // User-initiated disconnect must never start auto-reconnect
        service.connect(hostAddress)
        shadowOf(Looper.getMainLooper()).idle()
        service.disconnect()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        assertEquals(ReconnectionState.Idle, service.reconnectionState.value)
    }

    private class FakeBluetoothHidTransport : HidTransport {
        private val _connectionState = MutableStateFlow<HidConnectionState>(
            HidConnectionState.Disconnected(bondedDevices = emptyList(), isDiscoverable = false)
        )
        override val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

        private val _discoveryState = MutableStateFlow(DiscoveryState())
        override val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

        private val _packetStats = MutableStateFlow(HidPacketStats())
        override val packetStats: StateFlow<HidPacketStats> = _packetStats.asStateFlow()

        var started = false
        var stopped = false
        var lastPairedAddress: String? = null
        var lastConnectedAddress: String? = null
        var connectCalls = 0
        var disconnectCalls = 0
        var failNextConnectCount = 0
        private val bondedList = mutableListOf<BondedHostDevice>()

        override fun start() {
            started = true
        }

        override fun stop() {
            stopped = true
        }

        override fun requiredRuntimePermissions(): List<String> = emptyList()

        override fun refreshState() {
            _connectionState.value = HidConnectionState.Disconnected(bondedList.toList(), false)
        }

        override fun startDiscovery() {
            _discoveryState.value = _discoveryState.value.copy(isDiscovering = true)
        }

        override fun stopDiscovery() {
            _discoveryState.value = _discoveryState.value.copy(isDiscovering = false)
        }

        override fun pairDevice(address: String) {
            stopDiscovery()
            lastPairedAddress = address
            _discoveryState.value = _discoveryState.value.copy(pairingDeviceAddress = address)
        }

        override fun connect(address: String) {
            connectCalls++
            lastConnectedAddress = address
            if (failNextConnectCount > 0) {
                failNextConnectCount--
                _connectionState.value = HidConnectionState.Error("Falha simulada", bondedList.toList())
                return
            }
            val host = bondedList.firstOrNull { it.address == address }
                ?: BondedHostDevice(name = "Host $address", address = address, type = HostDeviceType.COMPUTER)
            _connectionState.value = HidConnectionState.Connected(host)
        }

        override fun disconnect() {
            disconnectCalls++
            _connectionState.value = HidConnectionState.Disconnected(
                bondedDevices = bondedList.toList(),
                isDiscoverable = false,
                wasUserInitiated = true
            )
        }

        fun simulateUnexpectedDrop() {
            _connectionState.value = HidConnectionState.Disconnected(
                bondedDevices = bondedList.toList(),
                isDiscoverable = false,
                wasUserInitiated = false
            )
        }

        fun simulateDeviceFound(device: DiscoveredBluetoothDevice) {
            _discoveryState.value = _discoveryState.value.copy(
                discoveredDevices = _discoveryState.value.discoveredDevices + device
            )
        }

        fun simulateBonded(address: String, name: String) {
            bondedList += BondedHostDevice(name = name, address = address, type = HostDeviceType.COMPUTER)
            _discoveryState.value = _discoveryState.value.copy(
                pairingDeviceAddress = null,
                discoveredDevices = _discoveryState.value.discoveredDevices.map {
                    if (it.address == address) it.copy(bondState = DeviceBondState.BONDED) else it
                }
            )
            refreshState()
        }

        override fun setPowerSaverMode(enabled: Boolean) = Unit
        override fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int) = Unit
        override fun sendZoomScroll(wheelDelta: Int) = Unit
        override fun sendKeyCombo(modifiers: Int, keyCode: Int) = Unit
        override fun sendTextStrokes(strokes: List<HidKeyStroke>) = Unit
        override fun sendConsumerPulse(mask: Int) = Unit
        override fun releaseAll() = Unit
    }
}
