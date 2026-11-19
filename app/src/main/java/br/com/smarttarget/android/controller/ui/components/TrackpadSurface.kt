package br.com.smarttarget.android.controller.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import br.com.smarttarget.android.controller.hid.HidConstants
import kotlin.math.hypot

@Composable
fun TrackpadSurface(
    tapToClick: Boolean,
    twoFingerRightClick: Boolean,
    showMouseButtons: Boolean,
    swapMouseButtons: Boolean,
    hapticEnabled: Boolean,
    animationsEnabled: Boolean,
    isDragLocked: Boolean,
    isLeftPressed: Boolean,
    isMiddlePressed: Boolean,
    isRightPressed: Boolean,
    onMove: (dx: Float, dy: Float) -> Unit,
    onScroll: (deltaY: Float) -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onMiddleClick: () -> Unit,
    onDragLockChange: (Boolean) -> Unit,
    onMouseButtonHeld: (buttonMask: Int, pressed: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val activeTouches = remember { mutableStateListOf<Offset>() }
    var lastTapTime by remember { mutableStateOf(0L) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val outlineColor = if (isDragLocked) primaryColor else MaterialTheme.colorScheme.outline

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
                .border(
                    width = if (isDragLocked) 1.5.dp else 1.dp,
                    color = outlineColor,
                    shape = RoundedCornerShape(22.dp)
                )
                .pointerInput(tapToClick, twoFingerRightClick, hapticEnabled, animationsEnabled) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val isDoubleTapDrag = (downTime - lastTapTime) < 260L

                        var maxPointers = 1
                        var totalDistance = 0f
                        var dragStartedLocked = false

                        if (isDoubleTapDrag && tapToClick) {
                            dragStartedLocked = true
                            onDragLockChange(true)
                            if (hapticEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressedChanges = event.changes.filter { it.pressed }
                            if (animationsEnabled) {
                                activeTouches.clear()
                                activeTouches.addAll(pressedChanges.map { it.position })
                            } else if (activeTouches.isNotEmpty()) {
                                activeTouches.clear()
                            }

                            if (pressedChanges.isEmpty()) {
                                break
                            }

                            if (pressedChanges.size > maxPointers) {
                                maxPointers = pressedChanges.size
                            }

                            if (pressedChanges.size == 1) {
                                val delta = pressedChanges[0].positionChange()
                                totalDistance += hypot(delta.x, delta.y)
                                if (delta != Offset.Zero) {
                                    onMove(delta.x, delta.y)
                                    pressedChanges[0].consume()
                                }
                            } else if (pressedChanges.size >= 2) {
                                val avgDy = pressedChanges.map { it.positionChange().y }.average().toFloat()
                                val avgDx = pressedChanges.map { it.positionChange().x }.average().toFloat()
                                totalDistance += hypot(avgDx, avgDy)
                                if (kotlin.math.abs(avgDy) > 0.25f) {
                                    onScroll(avgDy)
                                }
                                pressedChanges.forEach { it.consume() }
                            }
                        }

                        activeTouches.clear()
                        val elapsed = System.currentTimeMillis() - downTime

                        if (dragStartedLocked) {
                            onDragLockChange(false)
                            lastTapTime = 0L
                        } else if (tapToClick && elapsed < 240L && totalDistance < 18f) {
                            if (maxPointers >= 2 && twoFingerRightClick) {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                }
                                onRightClick()
                                lastTapTime = 0L
                            } else if (maxPointers == 1) {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onLeftClick()
                                lastTapTime = System.currentTimeMillis()
                            }
                        }
                    }
                }
                .testTag("trackpad_surface")
        ) {
            if (animationsEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 28.dp.toPx()
                    val dotColor = Color(0xFF283347)
                    var x = step
                    while (x < size.width) {
                        var y = step
                        while (y < size.height) {
                            drawCircle(
                                color = dotColor,
                                radius = 1.2.dp.toPx(),
                                center = Offset(x, y)
                            )
                            y += step
                        }
                        x += step
                    }

                    drawLine(
                        color = Color(0xFF253044),
                        start = Offset(size.width - 10.dp.toPx(), 28.dp.toPx()),
                        end = Offset(size.width - 10.dp.toPx(), size.height - 28.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )

                    activeTouches.forEachIndexed { idx, pos ->
                        val glowColor = if (idx == 0) primaryColor else secondaryColor
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                                center = pos,
                                radius = 56.dp.toPx()
                            ),
                            radius = 56.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = glowColor.copy(alpha = 0.85f),
                            radius = 8.dp.toPx(),
                            center = pos
                        )
                    }
                }
            }

            if (activeTouches.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isDragLocked) Icons.Rounded.Lock else Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = when {
                            isDragLocked -> "Arraste travado — toque para soltar"
                            !animationsEnabled -> "Modo ECO ativo • Animações e brilhos desativados"
                            twoFingerRightClick -> "1 dedo: mover / clicar  •  2 dedos: rolar / botão direito"
                            else -> "1 dedo: mover / clicar  •  2 dedos: rolar"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }

        if (showMouseButtons) {
            Spacer(modifier = Modifier.height(10.dp))

            val firstLabel = if (swapMouseButtons) "DIR" else "ESQ"
            val lastLabel = if (swapMouseButtons) "ESQ" else "DIR"
            val firstActive = if (swapMouseButtons) isRightPressed else (isLeftPressed || isDragLocked)
            val lastActive = if (swapMouseButtons) (isLeftPressed || isDragLocked) else isRightPressed

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MouseButtonKey(
                    label = firstLabel,
                    isActive = firstActive,
                    hapticEnabled = hapticEnabled,
                    animationsEnabled = animationsEnabled,
                    onPressChange = { pressed -> onMouseButtonHeld(HidConstants.MOUSE_LEFT, pressed) },
                    modifier = Modifier
                        .weight(2.2f)
                        .testTag("mouse_left_button")
                )
                MouseButtonKey(
                    label = "MEIO",
                    isActive = isMiddlePressed,
                    hapticEnabled = hapticEnabled,
                    animationsEnabled = animationsEnabled,
                    onPressChange = { pressed -> onMouseButtonHeld(HidConstants.MOUSE_MIDDLE, pressed) },
                    onTapFallback = onMiddleClick,
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("mouse_middle_button")
                )
                MouseButtonKey(
                    label = lastLabel,
                    isActive = lastActive,
                    hapticEnabled = hapticEnabled,
                    animationsEnabled = animationsEnabled,
                    onPressChange = { pressed -> onMouseButtonHeld(HidConstants.MOUSE_RIGHT, pressed) },
                    modifier = Modifier
                        .weight(2.2f)
                        .testTag("mouse_right_button")
                )
            }
        }
    }
}

@Composable
private fun MouseButtonKey(
    label: String,
    isActive: Boolean,
    hapticEnabled: Boolean,
    animationsEnabled: Boolean,
    onPressChange: (Boolean) -> Unit,
    onTapFallback: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed && hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        onPressChange(isPressed)
    }

    val bgColor by animateColorAsState(
        targetValue = if (isActive || isPressed) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = if (animationsEnabled) tween(140) else tween(0),
        label = "mouse_btn_bg"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive || isPressed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = if (animationsEnabled) tween(140) else tween(0),
        label = "mouse_btn_border"
    )

    Surface(
        onClick = { onTapFallback?.invoke() },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(50.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive || isPressed) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
