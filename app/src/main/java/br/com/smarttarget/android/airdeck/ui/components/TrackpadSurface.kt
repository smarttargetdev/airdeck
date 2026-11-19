package br.com.smarttarget.android.airdeck.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.smarttarget.android.airdeck.R
import br.com.smarttarget.android.airdeck.domain.NavigationAction
import br.com.smarttarget.android.airdeck.domain.SwipeGesturePattern
import br.com.smarttarget.android.airdeck.hid.HidConstants
import br.com.smarttarget.android.airdeck.ui.TouchGestureEvent
import kotlin.math.abs
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
    activeSwipeGestures: Map<SwipeGesturePattern, NavigationAction> = emptyMap(),
    onTouchGesture: (TouchGestureEvent) -> Unit,
    modifier: Modifier = Modifier
) {
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
                .pointerInput(tapToClick, twoFingerRightClick, hapticEnabled, animationsEnabled, activeSwipeGestures) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val downTime = System.currentTimeMillis()
                        val isDoubleTapDrag = (downTime - lastTapTime) < 260L

                        var maxPointers = 1
                        var totalDistance = 0f
                        var multiFingerDx = 0f
                        var multiFingerDy = 0f
                        var dragStartedLocked = false
                        var swipeHandled = false
                        val threeFingerThresholdPx = 56.dp.toPx()
                        val twoFingerSwipeThresholdPx = 84.dp.toPx()

                        if (isDoubleTapDrag && tapToClick) {
                            dragStartedLocked = true
                            onTouchGesture(TouchGestureEvent.DragLockChanged(true))
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
                                    onTouchGesture(TouchGestureEvent.SingleFingerMove(delta.x, delta.y))
                                    pressedChanges[0].consume()
                                }
                            } else if (pressedChanges.size >= 3) {
                                val avgDy = pressedChanges.map { it.positionChange().y }.average().toFloat()
                                val avgDx = pressedChanges.map { it.positionChange().x }.average().toFloat()
                                totalDistance += hypot(avgDx, avgDy)
                                multiFingerDx += avgDx
                                multiFingerDy += avgDy
                                if (!swipeHandled && hypot(multiFingerDx, multiFingerDy) >= threeFingerThresholdPx) {
                                    val pattern = if (abs(multiFingerDx) >= abs(multiFingerDy)) {
                                        if (multiFingerDx > 0f) SwipeGesturePattern.THREE_FINGER_RIGHT
                                        else SwipeGesturePattern.THREE_FINGER_LEFT
                                    } else {
                                        if (multiFingerDy > 0f) SwipeGesturePattern.THREE_FINGER_DOWN
                                        else SwipeGesturePattern.THREE_FINGER_UP
                                    }
                                    onTouchGesture(TouchGestureEvent.SwipeGesture(pattern))
                                    swipeHandled = true
                                }
                                pressedChanges.forEach { it.consume() }
                            } else if (pressedChanges.size == 2) {
                                val zoom = event.calculateZoom()
                                val avgDy = pressedChanges.map { it.positionChange().y }.average().toFloat()
                                val avgDx = pressedChanges.map { it.positionChange().x }.average().toFloat()
                                totalDistance += hypot(avgDx, avgDy)
                                multiFingerDx += avgDx
                                multiFingerDy += avgDy

                                val horizontalDominant = abs(multiFingerDx) >= twoFingerSwipeThresholdPx &&
                                    abs(multiFingerDx) > abs(multiFingerDy) * 2.2f
                                val twoFingerCandidate = if (multiFingerDx > 0f) {
                                    SwipeGesturePattern.TWO_FINGER_SWIPE_RIGHT
                                } else {
                                    SwipeGesturePattern.TWO_FINGER_SWIPE_LEFT
                                }
                                val hasBoundTwoFingerSwipe =
                                    (activeSwipeGestures[twoFingerCandidate] ?: NavigationAction.NONE) != NavigationAction.NONE

                                if (!swipeHandled && horizontalDominant && hasBoundTwoFingerSwipe) {
                                    onTouchGesture(TouchGestureEvent.SwipeGesture(twoFingerCandidate))
                                    swipeHandled = true
                                } else if (!swipeHandled) {
                                    if (abs(zoom - 1f) > 0.025f) {
                                        onTouchGesture(TouchGestureEvent.PinchZoom(zoom))
                                    } else if (abs(avgDy) > 0.25f || abs(avgDx) > 0.25f) {
                                        onTouchGesture(TouchGestureEvent.TwoFingerScroll(avgDx, avgDy))
                                    }
                                }
                                pressedChanges.forEach { it.consume() }
                            }
                        }

                        activeTouches.clear()
                        val elapsed = System.currentTimeMillis() - downTime

                        if (dragStartedLocked) {
                            onTouchGesture(TouchGestureEvent.DragLockChanged(false))
                            lastTapTime = 0L
                        } else if (!swipeHandled && tapToClick && elapsed < 240L && totalDistance < 18f) {
                            if (maxPointers >= 2 && twoFingerRightClick) {
                                onTouchGesture(TouchGestureEvent.TwoFingerTap)
                                lastTapTime = 0L
                            } else if (maxPointers == 1) {
                                onTouchGesture(TouchGestureEvent.SingleTap)
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
                            twoFingerRightClick -> "1 dedo: mover / clicar  •  2 dedos: rolar, pinça ou dir."
                            else -> "1 dedo: mover / clicar  •  2 dedos: rolar ou pinça"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }

        if (showMouseButtons) {
            Spacer(modifier = Modifier.height(10.dp))

            val firstActive = if (swapMouseButtons) isRightPressed else (isLeftPressed || isDragLocked)
            val lastActive = if (swapMouseButtons) (isLeftPressed || isDragLocked) else isRightPressed
            val leftTooltip = if (swapMouseButtons) "Botão Esquerdo (Direito)" else "Botão Esquerdo"
            val rightTooltip = if (swapMouseButtons) "Botão Direito (Esquerdo)" else "Botão Direito"

            Surface(
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 28.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("mouse_silhouette_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(2.2f)
                            .fillMaxHeight()
                    ) {
                        MouseZoneButton(
                            drawableRes = R.drawable.ic_mouse_zone_left,
                            tooltipLabel = leftTooltip,
                            isActive = firstActive,
                            animationsEnabled = animationsEnabled,
                            iconWidth = 54.dp,
                            iconHeight = 34.dp,
                            zoneShape = RoundedCornerShape(topStart = 27.dp, bottomStart = 17.dp),
                            zoneTag = "mouse_left_button",
                            onPressChange = { pressed ->
                                onTouchGesture(
                                    TouchGestureEvent.MouseButtonStateChanged(
                                        HidConstants.MOUSE_LEFT,
                                        pressed
                                    )
                                )
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                    ) {
                        MouseZoneButton(
                            drawableRes = R.drawable.ic_mouse_zone_middle,
                            tooltipLabel = "Botão do Meio (Scroll)",
                            isActive = isMiddlePressed,
                            animationsEnabled = animationsEnabled,
                            iconWidth = 26.dp,
                            iconHeight = 36.dp,
                            zoneShape = RoundedCornerShape(0.dp),
                            zoneTag = "mouse_middle_button",
                            onPressChange = { pressed ->
                                onTouchGesture(
                                    TouchGestureEvent.MouseButtonStateChanged(
                                        HidConstants.MOUSE_MIDDLE,
                                        pressed
                                    )
                                )
                            },
                            onTapFallback = { onTouchGesture(TouchGestureEvent.MiddleClick) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    Box(
                        modifier = Modifier
                            .weight(2.2f)
                            .fillMaxHeight()
                    ) {
                        MouseZoneButton(
                            drawableRes = R.drawable.ic_mouse_zone_right,
                            tooltipLabel = rightTooltip,
                            isActive = lastActive,
                            animationsEnabled = animationsEnabled,
                            iconWidth = 54.dp,
                            iconHeight = 34.dp,
                            zoneShape = RoundedCornerShape(topEnd = 27.dp, bottomEnd = 17.dp),
                            zoneTag = "mouse_right_button",
                            onPressChange = { pressed ->
                                onTouchGesture(
                                    TouchGestureEvent.MouseButtonStateChanged(
                                        HidConstants.MOUSE_RIGHT,
                                        pressed
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MouseZoneButton(
    @DrawableRes drawableRes: Int,
    tooltipLabel: String,
    isActive: Boolean,
    animationsEnabled: Boolean,
    iconWidth: Dp,
    iconHeight: Dp,
    zoneShape: Shape,
    zoneTag: String,
    onPressChange: (Boolean) -> Unit,
    onTapFallback: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tooltipState = rememberTooltipState()

    LaunchedEffect(isPressed) {
        onPressChange(isPressed)
    }

    val highlighted = isActive || isPressed
    val bgColor by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        animationSpec = if (animationsEnabled) tween(140) else tween(0),
        label = "mouse_btn_bg"
    )

    val iconTint by animateColorAsState(
        targetValue = if (highlighted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = if (animationsEnabled) tween(140) else tween(0),
        label = "mouse_btn_tint"
    )

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = tooltipLabel,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        state = tooltipState,
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            onClick = { onTapFallback?.invoke() },
            interactionSource = interactionSource,
            shape = zoneShape,
            color = bgColor,
            border = if (highlighted) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            modifier = Modifier
                .fillMaxSize()
                .minimumInteractiveComponentSize()
                .testTag(zoneTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = drawableRes),
                    contentDescription = tooltipLabel,
                    tint = iconTint,
                    modifier = Modifier
                        .width(iconWidth)
                        .height(iconHeight)
                )
            }
        }
    }
}
