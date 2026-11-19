package br.com.smarttarget.android.airdeck.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import br.com.smarttarget.android.airdeck.domain.HapticFeedbackEngine
import br.com.smarttarget.android.airdeck.domain.HapticPattern

class AndroidVibratorHapticEngine(
    context: Context,
    private val clockMs: () -> Long = { System.currentTimeMillis() },
    private val stepTickCooldownMs: Long = 65L
) : HapticFeedbackEngine {

    private val vibrator: Vibrator? = resolveVibrator(context)
    private var lastStepTickAtMs = 0L

    override fun trigger(pattern: HapticPattern) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        if (pattern == HapticPattern.STEP_TICK) {
            val now = clockMs()
            if (now - lastStepTickAtMs < stepTickCooldownMs) {
                return
            }
            lastStepTickAtMs = now
        }

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = buildVibrationEffect(pattern, hasAmplitudeControl(vib))
                vib.vibrate(effect)
            } else {
                vibrateLegacy(vib, pattern)
            }
        }
    }

    private fun buildVibrationEffect(pattern: HapticPattern, amplitudeSupported: Boolean): VibrationEffect {
        return when (pattern) {
            HapticPattern.CLICK -> {
                val amplitude = if (amplitudeSupported) 150 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(14L, amplitude)
            }
            HapticPattern.DRAG_LOCK -> {
                val timings = longArrayOf(0L, 14L, 28L, 22L)
                if (amplitudeSupported) {
                    VibrationEffect.createWaveform(timings, intArrayOf(0, 150, 0, 215), -1)
                } else {
                    VibrationEffect.createWaveform(timings, -1)
                }
            }
            HapticPattern.STEP_TICK -> {
                val amplitude = if (amplitudeSupported) 90 else VibrationEffect.DEFAULT_AMPLITUDE
                VibrationEffect.createOneShot(7L, amplitude)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateLegacy(vib: Vibrator, pattern: HapticPattern) {
        when (pattern) {
            HapticPattern.CLICK -> vib.vibrate(14L)
            HapticPattern.DRAG_LOCK -> vib.vibrate(longArrayOf(0L, 14L, 28L, 22L), -1)
            HapticPattern.STEP_TICK -> vib.vibrate(7L)
        }
    }

    private fun hasAmplitudeControl(vib: Vibrator): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vib.hasAmplitudeControl()
    }

    companion object {
        @Suppress("DEPRECATION")
        private fun resolveVibrator(context: Context): Vibrator? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
}
