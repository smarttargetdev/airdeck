package br.com.smarttarget.android.airdeck

import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import br.com.smarttarget.android.airdeck.data.AndroidVibratorHapticEngine
import br.com.smarttarget.android.airdeck.domain.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndroidVibratorHapticEngineTest {

    @Test
    fun `vibrator system service triggers distinct pulses and throttles step ticks`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator: Vibrator = manager.defaultVibrator
        val shadowVibrator = shadowOf(vibrator)
        shadowVibrator.setHasVibrator(true)

        var clockMs = 1000L
        val engine = AndroidVibratorHapticEngine(
            context = context,
            clockMs = { clockMs },
            stepTickCooldownMs = 65L
        )

        engine.trigger(HapticPattern.CLICK)
        assertTrue(shadowVibrator.isVibrating)
        assertEquals(14L, shadowVibrator.milliseconds)

        engine.trigger(HapticPattern.DRAG_LOCK)
        assertTrue(shadowVibrator.isVibrating)
        assertEquals(listOf(0L, 14L, 28L, 22L), shadowVibrator.pattern.toList())

        engine.trigger(HapticPattern.STEP_TICK)
        assertEquals(7L, shadowVibrator.milliseconds)

        // Subsequent STEP_TICK within cooldown should be ignored; trigger CLICK first to observe change
        engine.trigger(HapticPattern.CLICK)
        assertEquals(14L, shadowVibrator.milliseconds)

        clockMs += 20L
        engine.trigger(HapticPattern.STEP_TICK)
        assertEquals(14L, shadowVibrator.milliseconds)

        clockMs += 60L
        engine.trigger(HapticPattern.STEP_TICK)
        assertEquals(7L, shadowVibrator.milliseconds)
    }
}
