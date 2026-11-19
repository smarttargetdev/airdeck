package br.com.smarttarget.android.airdeck.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AirMousePanel(
    isAvailable: Boolean,
    isStreaming: Boolean,
    yawRate: Float,
    pitchRate: Float,
    hapticEnabled: Boolean,
    animationsEnabled: Boolean,
    onToggleStreaming: () -> Unit,
    onSetHeld: (Boolean) -> Unit,
    onRecenter: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val activeColor by animateColorAsState(
        targetValue = if (isStreaming) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = if (animationsEnabled) tween(200) else tween(0),
        label = "air_mouse_color"
    )

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(if (isStreaming) 1.5.dp else 1.dp, activeColor),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (!isAvailable) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SensorsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Giroscópio indisponível",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Este aparelho não possui sensor giroscópio para o modo Air Mouse. Utilize o Trackpad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Air Mouse (Giroscópio)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isStreaming) "Ativo — aponte o celular" else "Pausado — toque ou segure",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isStreaming) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (hapticEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onRecenter()
                            },
                            modifier = Modifier.testTag("air_mouse_recenter")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CenterFocusStrong,
                                contentDescription = "Recentralizar",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Zerar", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    val primaryColor = MaterialTheme.colorScheme.secondary
                    val trackColor = MaterialTheme.colorScheme.outline
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .pointerInput(isAvailable) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    onSetHeld(true)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.none { it.pressed }) break
                                    }
                                    onSetHeld(false)
                                }
                            }
                            .testTag("air_mouse_hold_pad"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.minDimension / 2f - 6.dp.toPx()
                            drawCircle(
                                color = trackColor,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            drawCircle(
                                color = trackColor.copy(alpha = 0.5f),
                                radius = radius * 0.5f,
                                center = center,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            val offsetX = (-yawRate * 18f).coerceIn(-radius * 0.65f, radius * 0.65f)
                            val offsetY = (-pitchRate * 18f).coerceIn(-radius * 0.65f, radius * 0.65f)
                            drawCircle(
                                color = if (isStreaming) primaryColor else primaryColor.copy(alpha = 0.45f),
                                radius = 14.dp.toPx(),
                                center = Offset(center.x + offsetX, center.y + offsetY)
                            )
                        }
                        Text(
                            text = if (isStreaming) "MOVENDO" else "SEGURE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = {
                            if (hapticEnabled) {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                            onToggleStreaming()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isStreaming) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            contentColor = if (isStreaming) {
                                MaterialTheme.colorScheme.onSecondary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        border = BorderStroke(1.dp, activeColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("air_mouse_lock_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sensors,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isStreaming) "Travar Air Mouse (Ligado)" else "Ativar Air Mouse Contínuo",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onLeftClick,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("air_left_click")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "CLIQUE ESQ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Surface(
                onClick = onRightClick,
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("air_right_click")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "CLIQUE DIR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
