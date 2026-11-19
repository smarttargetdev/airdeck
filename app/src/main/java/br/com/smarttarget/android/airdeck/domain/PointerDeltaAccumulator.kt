package br.com.smarttarget.android.airdeck.domain

import kotlin.math.hypot
import kotlin.math.roundToInt

data class PointerReportDelta(
    val dx: Int,
    val dy: Int
)

class PointerDeltaAccumulator(
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) {
    private var pendingDx = 0f
    private var pendingDy = 0f
    private var pendingScroll = 0f
    private var lastMoveDispatchMs = 0L
    private var lastScrollDispatchMs = 0L

    fun accumulateMove(
        rawDx: Float,
        rawDy: Float,
        sensitivity: Float,
        minIntervalMs: Long
    ): PointerReportDelta? {
        val magnitude = hypot(rawDx, rawDy)
        val acceleration = 1f + (magnitude * 0.022f).coerceAtMost(1.4f)
        pendingDx += rawDx * sensitivity * acceleration
        pendingDy += rawDy * sensitivity * acceleration

        val now = clockMs()
        if (minIntervalMs > 0L && (now - lastMoveDispatchMs) < minIntervalMs) {
            return null
        }

        val sendX = pendingDx.roundToInt().coerceIn(-127, 127)
        val sendY = pendingDy.roundToInt().coerceIn(-127, 127)
        if (sendX == 0 && sendY == 0) {
            return null
        }

        pendingDx -= sendX
        pendingDy -= sendY
        lastMoveDispatchMs = now
        return PointerReportDelta(sendX, sendY)
    }

    fun accumulateScroll(
        rawDeltaY: Float,
        sensitivity: Float,
        naturalScroll: Boolean,
        minIntervalMs: Long
    ): Int? {
        val direction = if (naturalScroll) 1f else -1f
        pendingScroll += (rawDeltaY / 18f) * sensitivity * direction

        val now = clockMs()
        if (minIntervalMs > 0L && (now - lastScrollDispatchMs) < minIntervalMs) {
            return null
        }

        val wheel = pendingScroll.roundToInt().coerceIn(-127, 127)
        if (wheel == 0) {
            return null
        }

        pendingScroll -= wheel
        lastScrollDispatchMs = now
        return wheel
    }

    fun reset() {
        pendingDx = 0f
        pendingDy = 0f
        pendingScroll = 0f
        lastMoveDispatchMs = 0L
        lastScrollDispatchMs = 0L
    }
}
