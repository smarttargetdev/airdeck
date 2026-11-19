package br.com.smarttarget.android.controller.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import br.com.smarttarget.android.controller.domain.ControlProfile
import br.com.smarttarget.android.controller.hid.KeyboardLayout
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    activeProfile: ControlProfile,
    powerSaverMode: Boolean,
    onDismiss: () -> Unit,
    onPowerSaverChange: (Boolean) -> Unit,
    onPointerSensitivityChange: (Float) -> Unit,
    onScrollSensitivityChange: (Float) -> Unit,
    onAirMouseSensitivityChange: (Float) -> Unit,
    onNaturalScrollChange: (Boolean) -> Unit,
    onTapToClickChange: (Boolean) -> Unit,
    onTwoFingerRightClickChange: (Boolean) -> Unit,
    onShowMouseButtonsChange: (Boolean) -> Unit,
    onSwapMouseButtonsChange: (Boolean) -> Unit,
    onHapticFeedbackChange: (Boolean) -> Unit,
    onKeyboardLayoutChange: (KeyboardLayout) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Ajustes — ${activeProfile.name}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Global Power Saver Mode card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (powerSaverMode) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    1.dp,
                    if (powerSaverMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatterySaver,
                            contentDescription = "Economia de energia",
                            tint = if (powerSaverMode) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Modo Economia de Energia",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Desativa animações da UI, reduz frequência de atualização (22Hz) e otimiza o QoS Bluetooth.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Switch(
                        checked = powerSaverMode,
                        onCheckedChange = onPowerSaverChange,
                        modifier = Modifier.testTag("switch_power_saver")
                    )
                }
            }

            // Sensitivity Sliders (saved directly to active profile)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SliderSettingRow(
                        label = "Sensibilidade do Trackpad",
                        value = activeProfile.pointerSensitivity,
                        valueRange = 0.5f..3.0f,
                        onValueChange = onPointerSensitivityChange,
                        tag = "slider_pointer_sensitivity"
                    )
                    SliderSettingRow(
                        label = "Velocidade da Rolagem",
                        value = activeProfile.scrollSensitivity,
                        valueRange = 0.5f..3.0f,
                        onValueChange = onScrollSensitivityChange,
                        tag = "slider_scroll_sensitivity"
                    )
                    SliderSettingRow(
                        label = "Sensibilidade do Air Mouse",
                        value = activeProfile.airMouseSensitivity,
                        valueRange = 0.5f..3.0f,
                        onValueChange = onAirMouseSensitivityChange,
                        tag = "slider_air_sensitivity"
                    )
                }
            }

            // Behavior Toggles
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ToggleSettingRow(
                        title = "Toque para clicar",
                        subtitle = "Toque simples com 1 dedo envia clique primário",
                        checked = activeProfile.tapToClick,
                        onCheckedChange = onTapToClickChange,
                        tag = "switch_tap_to_click"
                    )
                    ToggleSettingRow(
                        title = "Clique direito com 2 dedos",
                        subtitle = "Toque simultâneo com 2 dedos no trackpad",
                        checked = activeProfile.twoFingerRightClick,
                        onCheckedChange = onTwoFingerRightClickChange,
                        tag = "switch_two_finger_right"
                    )
                    ToggleSettingRow(
                        title = "Rolagem natural",
                        subtitle = "Acompanha a direção dos dedos",
                        checked = activeProfile.naturalScroll,
                        onCheckedChange = onNaturalScrollChange,
                        tag = "switch_natural_scroll"
                    )
                    ToggleSettingRow(
                        title = "Botões físicos virtuais",
                        subtitle = "Exibe ESQ / MEIO / DIR abaixo do trackpad",
                        checked = activeProfile.showMouseButtons,
                        onCheckedChange = onShowMouseButtonsChange,
                        tag = "switch_show_mouse_buttons"
                    )
                    ToggleSettingRow(
                        title = "Modo canhoto (inverter botões)",
                        subtitle = "Troca clique esquerdo e direito",
                        checked = activeProfile.swapMouseButtons,
                        onCheckedChange = onSwapMouseButtonsChange,
                        tag = "switch_swap_mouse_buttons"
                    )
                    ToggleSettingRow(
                        title = "Resposta háptica",
                        subtitle = if (powerSaverMode) "Pausada pelo modo economia" else "Vibração sutil em cliques e teclas",
                        checked = activeProfile.hapticFeedback,
                        onCheckedChange = onHapticFeedbackChange,
                        tag = "switch_haptic_feedback"
                    )
                }
            }

            // Keyboard Layout Selector
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Layout do Teclado no Dispositivo Remoto",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeyboardLayout.entries.forEach { layout ->
                            val selected = activeProfile.keyboardLayout == layout
                            Surface(
                                onClick = { onKeyboardLayoutChange(layout) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("layout_${layout.name.lowercase()}")
                            ) {
                                Text(
                                    text = layout.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderSettingRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    tag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${((value * 10).roundToInt() / 10f)}x",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
