package br.com.smarttarget.android.controller

import br.com.smarttarget.android.controller.hid.HidConstants
import br.com.smarttarget.android.controller.hid.HidKeyMapper
import br.com.smarttarget.android.controller.hid.KeyboardLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `hid descriptor contains all three report ids`() {
        val descriptor = HidConstants.HID_REPORT_DESCRIPTOR
        assertTrue(descriptor.isNotEmpty())
        assertTrue(descriptor.contains(HidConstants.REPORT_ID_KEYBOARD.toByte()))
        assertTrue(descriptor.contains(HidConstants.REPORT_ID_MOUSE.toByte()))
        assertTrue(descriptor.contains(HidConstants.REPORT_ID_CONSUMER.toByte()))
    }

    @Test
    fun `abnt2 mapper translates cedilha and accented vowels`() {
        val strokes = HidKeyMapper.mapTextToKeyStrokes("Olá, ação!", KeyboardLayout.ABNT2)
        // O (Shift+O), l, á (acute + a), ',', ' ', a, ç (KEY_SEMICOLON), ã (tilde + a), ! (Shift+1)
        assertTrue(strokes.size >= 10)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[0].modifiers)
        assertEquals(HidConstants.KEY_O, strokes[0].keyCode)
        assertTrue(strokes.any { it.keyCode == HidConstants.KEY_SEMICOLON && it.modifiers == HidConstants.MOD_NONE })
        assertTrue(strokes.any { it.keyCode == HidConstants.KEY_1 && it.modifiers == HidConstants.MOD_LEFT_SHIFT })
    }

    @Test
    fun `us intl mapper translates slash and question mark`() {
        val strokes = HidKeyMapper.mapTextToKeyStrokes("/?", KeyboardLayout.US_INTL)
        assertEquals(2, strokes.size)
        assertEquals(HidConstants.KEY_SLASH, strokes[0].keyCode)
        assertEquals(HidConstants.MOD_NONE, strokes[0].modifiers)
        assertEquals(HidConstants.KEY_SLASH, strokes[1].keyCode)
        assertEquals(HidConstants.MOD_LEFT_SHIFT, strokes[1].modifiers)
    }
}
