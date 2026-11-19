package br.com.smarttarget.android.airdeck.hid

object HidReportEncoder {

    fun encodeKeyboardReport(modifiers: Int, keyCode: Int): ByteArray = byteArrayOf(
        (modifiers and 0xFF).toByte(),
        0x00,
        (keyCode and 0xFF).toByte(),
        0x00,
        0x00,
        0x00,
        0x00,
        0x00
    )

    fun encodeMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int): ByteArray = byteArrayOf(
        (buttons and 0x07).toByte(),
        dx.coerceIn(-127, 127).toByte(),
        dy.coerceIn(-127, 127).toByte(),
        wheel.coerceIn(-127, 127).toByte()
    )

    fun encodeConsumerReport(mask: Int): ByteArray = byteArrayOf(
        (mask and 0xFF).toByte(),
        ((mask ushr 8) and 0xFF).toByte()
    )

    fun formatHex(bytes: ByteArray): String =
        bytes.joinToString(" ") { "%02X".format(it) }
}
