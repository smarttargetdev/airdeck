package br.com.smarttarget.android.airdeck

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import br.com.smarttarget.android.airdeck.ui.theme.AirDeckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Air Deck", appName)
    }

    @Test
    fun `app switches profiles creates custom profile and toggles power saver mode`() {
        composeTestRule.setContent {
            AirDeckTheme {
                AirDeckApp()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Air Deck").assertIsDisplayed()
        composeTestRule.onNodeWithText("Geral").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jogos").assertIsDisplayed()
        composeTestRule.onNodeWithText("TV").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_trackpad").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_media").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mode_air").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_keyboard_button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Mouse").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Mídia").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Air").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Teclado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mouse").assertDoesNotExist()
        composeTestRule.onNodeWithText("Mídia").assertDoesNotExist()
        composeTestRule.onNodeWithText("Air").assertDoesNotExist()
        composeTestRule.onNodeWithText("Teclado").assertDoesNotExist()
        composeTestRule.onNodeWithTag("mode_trackpad").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Mouse").assertIsDisplayed()
        composeTestRule.onNodeWithTag("trackpad_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mouse_silhouette_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mouse_left_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mouse_middle_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mouse_right_button").assertIsDisplayed()
        composeTestRule.onNodeWithText("ESQ").assertDoesNotExist()
        composeTestRule.onNodeWithText("MEIO").assertDoesNotExist()
        composeTestRule.onNodeWithText("DIR").assertDoesNotExist()
        composeTestRule.onNodeWithTag("mouse_middle_button").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Botão do Meio (Scroll)").assertIsDisplayed()

        // Switch to TV profile -> switches to Media & TV D-Pad automatically
        composeTestRule.onNodeWithTag("profile_chip_preset_tv").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dpad_ok").assertIsDisplayed()

        // Switch back to Geral -> Trackpad
        composeTestRule.onNodeWithTag("profile_chip_preset_productivity").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("trackpad_surface").assertIsDisplayed()

        // Toggle Power Saver Mode via header ECO chip
        composeTestRule.onNodeWithTag("power_saver_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Modo ECO ativo • Animações e brilhos desativados").assertIsDisplayed()

        // Open New Profile Editor and save a named profile
        composeTestRule.onNodeWithTag("create_profile_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_name_input").performTextReplacement("Edição Vídeo")
        composeTestRule.onNodeWithTag("save_profile_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Edição Vídeo").assertIsDisplayed()

        // Toggle hybrid keyboard panel and verify renamed tabs "Teclado" and "Controle"
        composeTestRule.onNodeWithTag("toggle_keyboard_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ime_bridge_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kb_tab_hybrid_ime").assertIsDisplayed()
        composeTestRule.onNodeWithTag("kb_tab_function_and_numpad").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teclado").assertIsDisplayed()
        composeTestRule.onNodeWithText("Controle").assertIsDisplayed()
        composeTestRule.onNodeWithText("Teclado IME").assertDoesNotExist()
        composeTestRule.onNodeWithText("F1–F12 & Num").assertDoesNotExist()

        // Verify live reconnecting status banner and cancel button
        val host = br.com.smarttarget.android.airdeck.hid.BondedHostDevice(
            name = "Notebook Sala",
            address = "11:22:33:44:55:66",
            type = br.com.smarttarget.android.airdeck.hid.HostDeviceType.COMPUTER
        )
        br.com.smarttarget.android.airdeck.domain.BluetoothReconnectionBridge.updateState(
            br.com.smarttarget.android.airdeck.domain.ReconnectionState.Reconnecting(host, 2, 5)
        )
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("reconnecting_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("cancel_reconnect_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(
            br.com.smarttarget.android.airdeck.domain.ReconnectionState.Idle,
            br.com.smarttarget.android.airdeck.domain.BluetoothReconnectionBridge.state.value
        )
        // Verify SettingsSheet keyboard layout title and renamed layout options
        composeTestRule.onNodeWithTag("settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Layout do Teclado").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Português").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Internacional").performScrollTo().assertIsDisplayed()

        // Open Custom Gesture Profile Editor, map swipe gesture to navigation action, and save
        composeTestRule.onNodeWithTag("open_gesture_editor_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Perfis de Gestos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gesture_pattern_three_finger_left").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("gesture_action_three_finger_left_close_tab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("save_gestures_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Perfis de Gestos").assertDoesNotExist()

        // Verify Ajuda icon button beside Ajustes opens README.md with Mermaid diagram support
        composeTestRule.onNodeWithTag("help_button").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Ajuda").assertIsDisplayed()
        composeTestRule.onNodeWithTag("help_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("help_readme_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Air Deck – Virtual Hardware").assertIsDisplayed()
        composeTestRule.onNodeWithTag("help_section_chip_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("close_help_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("help_readme_sheet").assertDoesNotExist()
    }
}
