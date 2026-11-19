package br.com.smarttarget.android.airdeck.hid

import kotlinx.coroutines.flow.StateFlow

interface HidReportSender {
    val packetStats: StateFlow<HidPacketStats>

    fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int)
    fun sendZoomScroll(wheelDelta: Int)
    fun sendKeyCombo(modifiers: Int, keyCode: Int)
    fun sendTextStrokes(strokes: List<HidKeyStroke>)
    fun sendConsumerPulse(mask: Int)
    fun releaseAll()
}

interface BluetoothConnectionControl {
    val connectionState: StateFlow<HidConnectionState>
    val discoveryState: StateFlow<DiscoveryState>

    fun start()
    fun stop()
    fun requiredRuntimePermissions(): List<String>
    fun refreshState()
    fun startDiscovery()
    fun stopDiscovery()
    fun pairDevice(address: String)
    fun connect(address: String)
    fun disconnect()
    fun setPowerSaverMode(enabled: Boolean)
}

interface HidTransport : HidReportSender, BluetoothConnectionControl
