package br.com.smarttarget.android.controller.hid

import kotlinx.coroutines.flow.StateFlow

interface HidTransport {
    val connectionState: StateFlow<HidConnectionState>
    val packetStats: StateFlow<HidPacketStats>

    fun start()
    fun stop()
    fun requiredRuntimePermissions(): List<String>
    fun refreshState()
    fun connect(address: String)
    fun disconnect()
    fun setPowerSaverMode(enabled: Boolean)
    fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int)
    fun sendKeyCombo(modifiers: Int, keyCode: Int)
    fun sendTextStrokes(strokes: List<HidKeyStroke>)
    fun sendConsumerPulse(mask: Int)
    fun releaseAll()
}
