package br.com.smarttarget.android.airdeck.domain

enum class HapticPattern {
    /** Crisp single pulse for primary/secondary clicks, buttons, and media keys. */
    CLICK,

    /** Distinct double pulse for drag-lock activation/release and modifier sticky lock. */
    DRAG_LOCK,

    /** Light micro-pulse for scroll ticks, pinch-zoom thresholds, and special/modifier keys. */
    STEP_TICK
}

interface HapticFeedbackEngine {
    fun trigger(pattern: HapticPattern)
}

object NoOpHapticFeedbackEngine : HapticFeedbackEngine {
    override fun trigger(pattern: HapticPattern) = Unit
}
