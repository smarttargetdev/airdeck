package br.com.smarttarget.android.controller

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import br.com.smarttarget.android.controller.ui.theme.SmartiesControllerTheme
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
        assertEquals("Smarties Controller", appName)
    }

    @Test
    fun `app switches profiles creates custom profile and toggles power saver mode`() {
        composeTestRule.setContent {
            SmartiesControllerTheme {
                SmartiesControllerApp()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Smarties Controller").assertIsDisplayed()
        composeTestRule.onNodeWithTag("trackpad_surface").assertIsDisplayed()

        // Switch to Smart TV & Sofá profile -> switches to Media & TV D-Pad automatically
        composeTestRule.onNodeWithTag("profile_chip_preset_tv").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("dpad_ok").assertIsDisplayed()

        // Switch back to Produtividade -> Trackpad
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

        // Toggle hybrid keyboard panel
        composeTestRule.onNodeWithTag("toggle_keyboard_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ime_bridge_input").assertIsDisplayed()
    }
}
