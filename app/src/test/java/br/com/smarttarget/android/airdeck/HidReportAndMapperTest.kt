package br.com.smarttarget.android.airdeck

import br.com.smarttarget.android.airdeck.domain.ImeTextDiffer
import br.com.smarttarget.android.airdeck.hid.HidConstants
import br.com.smarttarget.android.airdeck.hid.HidKeyMapper
import br.com.smarttarget.android.airdeck.hid.HidReportEncoder
import br.com.smarttarget.android.airdeck.hid.KeyboardLayout
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HidReportAndMapperTest {

    @Test
    fun `keyboard report encodes modifiers and keycode in 8 bytes`() {
        val report = HidReportEncoder.encodeKeyboardReport(
            modifiers = HidConstants.MOD_LEFT_CTRL or HidConstants.MOD_LEFT_SHIFT,
            keyCode = HidConstants.KEY_C
        )
        assertEquals(8, report.size)
        assertArrayEquals(
            byteArrayOf(0x03, 0x00, HidConstants.KEY_C.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00),
            report
        )
    }

    @Test
    fun `mouse report clamps deltas to signed byte range and masks buttons`() {
        val report = HidReportEncoder.encodeMouseReport(
            buttons = 0xFF,
            dx = 250,
            dy = -300,
            wheel = 5
        )
        assertEquals(4, report.size)
        assertEquals(0x07.toByte(), report[0])
        assertEquals(127.toByte(), report[1])
        assertEquals((-127).toByte(), report[2])
        assertEquals(5.toByte(), report[3])
    }

    @Test
    fun `consumer report encodes 16 bit little endian mask`() {
        val report = HidReportEncoder.encodeConsumerReport(HidConstants.CONSUMER_AC_BACK)
        assertEquals(2, report.size)
        assertEquals(0x00.toByte(), report[0])
        assertEquals(0x01.toByte(), report[1])
    }

    @Test
    fun `abnt2 mapper encodes cedilha, accents and punctuation accurately`() {
        val strokes = HidKeyMapper.mapTextToKeyStrokes("Olá, ação!", KeyboardLayout.ABNT2)
        assertTrue(strokes.size >= 10)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[0].modifiers)
        assertEquals(HidConstants.KEY_O, strokes[0].keyCode)
        assertTrue(strokes.any { it.keyCode == HidConstants.KEY_SEMICOLON && it.modifiers == HidConstants.MOD_NONE })
        assertTrue(strokes.any { it.keyCode == HidConstants.KEY_1 && it.modifiers == HidConstants.MOD_LEFT_SHIFT })
    }

    @Test
    fun `ime differ computes deletions and insertions cleanly`() {
        val diff = ImeTextDiffer.computeDiff(
            oldText = "teste",
            newText = "tesar",
            layout = KeyboardLayout.ABNT2,
            activeModifierMask = HidConstants.MOD_NONE
        )
        assertEquals(2, diff.deletions)
        assertEquals("ar", diff.insertions)
        assertEquals(4, diff.strokes.size)
        assertEquals(HidConstants.KEY_BACKSPACE, diff.strokes[0].keyCode)
        assertEquals(HidConstants.KEY_BACKSPACE, diff.strokes[1].keyCode)
        assertEquals(HidConstants.KEY_A, diff.strokes[2].keyCode)
        assertEquals(HidConstants.KEY_R, diff.strokes[3].keyCode)
        assertFalse(diff.consumedActiveModifier)
    }

    @Test
    fun `ime differ applies active modifier to single typed character`() {
        val diff = ImeTextDiffer.computeDiff(
            oldText = "",
            newText = "c",
            layout = KeyboardLayout.ABNT2,
            activeModifierMask = HidConstants.MOD_LEFT_CTRL
        )
        assertEquals(1, diff.strokes.size)
        assertEquals(HidConstants.MOD_LEFT_CTRL, diff.strokes[0].modifiers)
        assertEquals(HidConstants.KEY_C, diff.strokes[0].keyCode)
        assertTrue(diff.consumedActiveModifier)
    }
}
