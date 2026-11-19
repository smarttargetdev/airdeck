package br.com.smarttarget.android.controller.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.KeyboardReturn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.smarttarget.android.controller.hid.HidConstants
import br.com.smarttarget.android.controller.hid.HidKeyMapper
import br.com.smarttarget.android.controller.hid.ShortcutPreset
import br.com.smarttarget.android.controller.ui.KeyboardPanelMode
import br.com.smarttarget.android.controller.ui.ModifierState

@Composable
fun HybridKeyboardPanel(
    keyboardMode: KeyboardPanelMode,
    modifiers: ModifierState,
    imeInputText: String,
    lastKeyFeedback: String,
    hapticEnabled: Boolean,
    animationsEnabled: Boolean,
    onSelectMode: (KeyboardPanelMode) -> Unit,
    onToggleCtrl: () -> Unit,
    onToggleShift: () -> Unit,
    onToggleAlt: () -> Unit,
    onToggleGui: () -> Unit,
    onToggleStickyLock: () -> Unit,
    onImeTextChanged: (String) -> Unit,
    onClearImeBuffer: () -> Unit,
    onSendSpecialKey: (keyCode: Int, label: String) -> Unit,
    onSendShortcutPreset: (ShortcutPreset) -> Unit,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current

    fun haptic() {
        if (hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top mode tabs + last key feedback + close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        KeyboardPanelMode.HYBRID_IME,
                        KeyboardPanelMode.FUNCTION_AND_NUMPAD,
                        KeyboardPanelMode.SHORTCUTS
                    ).forEach { tab ->
                        val selected = keyboardMode == tab
                        Surface(
                            onClick = {
                                haptic()
                                onSelectMode(tab)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                }
                            ),
                            modifier = Modifier.testTag("kb_tab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                if (lastKeyFeedback.isNotBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lastKeyFeedback,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("kb_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Fechar painel de teclado",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Sticky Modifiers Row (always accessible across keyboard sub-modes)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModifierChip(
                    label = "CTRL",
                    active = modifiers.ctrl,
                    onClick = {
                        haptic()
                        onToggleCtrl()
                    },
                    modifier = Modifier.testTag("mod_ctrl")
                )
                ModifierChip(
                    label = "ALT",
                    active = modifiers.alt,
                    onClick = {
                        haptic()
                        onToggleAlt()
                    },
                    modifier = Modifier.testTag("mod_alt")
                )
                ModifierChip(
                    label = "SHIFT",
                    active = modifiers.shift,
                    onClick = {
                        haptic()
                        onToggleShift()
                    },
                    modifier = Modifier.testTag("mod_shift")
                )
                ModifierChip(
                    label = "WIN",
                    active = modifiers.gui,
                    onClick = {
                        haptic()
                        onToggleGui()
                    },
                    modifier = Modifier.testTag("mod_win")
                )
                Surface(
                    onClick = {
                        haptic()
                        onToggleStickyLock()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (modifiers.stickyLock) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    border = BorderStroke(
                        1.dp,
                        if (modifiers.stickyLock) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("mod_sticky_lock")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (modifiers.stickyLock) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = "Travar modificadores",
                            tint = if (modifiers.stickyLock) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "TRAVAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (modifiers.stickyLock) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Sub-panel Content
            AnimatedContent(
                targetState = keyboardMode,
                transitionSpec = {
                    if (animationsEnabled) {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    } else {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                },
                label = "keyboard_subpanel"
            ) { mode ->
                when (mode) {
                    KeyboardPanelMode.HYBRID_IME -> {
                        HybridImeContent(
                            imeInputText = imeInputText,
                            onImeTextChanged = onImeTextChanged,
                            onClearImeBuffer = onClearImeBuffer,
                            onSendSpecialKey = { code, label ->
                                haptic()
                                onSendSpecialKey(code, label)
                            }
                        )
                    }
                    KeyboardPanelMode.FUNCTION_AND_NUMPAD -> {
                        FunctionAndNumpadContent(
                            onSendSpecialKey = { code, label ->
                                haptic()
                                onSendSpecialKey(code, label)
                            }
                        )
                    }
                    KeyboardPanelMode.SHORTCUTS -> {
                        ShortcutsGridContent(
                            onSendShortcutPreset = { preset ->
                                haptic()
                                onSendShortcutPreset(preset)
                            }
                        )
                    }
                    KeyboardPanelMode.HIDDEN -> Unit
                }
            }
        }
    }
}

@Composable
private fun HybridImeContent(
    imeInputText: String,
    onImeTextChanged: (String) -> Unit,
    onClearImeBuffer: () -> Unit,
    onSendSpecialKey: (Int, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Hybrid IME Bridge Text Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (imeInputText.isEmpty()) {
                    Text(
                        text = "Digite aqui para enviar via Bluetooth…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = imeInputText,
                    onValueChange = onImeTextChanged,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            onSendSpecialKey(HidConstants.KEY_ENTER, "Enter")
                            onClearImeBuffer()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ime_bridge_input")
                )
            }
            if (imeInputText.isNotEmpty()) {
                IconButton(
                    onClick = onClearImeBuffer,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("ime_clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Limpar campo local",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Essential PC Keys + Arrows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecialKeyButton("ESC", { onSendSpecialKey(HidConstants.KEY_ESCAPE, "Esc") }, Modifier.weight(1f).testTag("key_esc"))
            SpecialKeyButton("TAB", { onSendSpecialKey(HidConstants.KEY_TAB, "Tab") }, Modifier.weight(1f).testTag("key_tab"))
            SpecialKeyButton("DEL", { onSendSpecialKey(HidConstants.KEY_DELETE, "Del") }, Modifier.weight(1f).testTag("key_del"))
            SpecialKeyIconButton(
                icon = Icons.AutoMirrored.Rounded.Backspace,
                contentDescription = "Backspace",
                onClick = { onSendSpecialKey(HidConstants.KEY_BACKSPACE, "Backspace") },
                modifier = Modifier.weight(1.2f).testTag("key_backspace")
            )
            SpecialKeyIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardReturn,
                contentDescription = "Enter",
                highlighted = true,
                onClick = { onSendSpecialKey(HidConstants.KEY_ENTER, "Enter") },
                modifier = Modifier.weight(1.3f).testTag("key_enter")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecialKeyIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Seta esquerda",
                onClick = { onSendSpecialKey(HidConstants.KEY_LEFT, "←") },
                modifier = Modifier.weight(1f).testTag("key_left")
            )
            SpecialKeyIconButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Seta baixo",
                onClick = { onSendSpecialKey(HidConstants.KEY_DOWN, "↓") },
                modifier = Modifier.weight(1f).testTag("key_down")
            )
            SpecialKeyIconButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Seta cima",
                onClick = { onSendSpecialKey(HidConstants.KEY_UP, "↑") },
                modifier = Modifier.weight(1f).testTag("key_up")
            )
            SpecialKeyIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Seta direita",
                onClick = { onSendSpecialKey(HidConstants.KEY_RIGHT, "→") },
                modifier = Modifier.weight(1f).testTag("key_right")
            )
            SpecialKeyButton("ESPAÇO", { onSendSpecialKey(HidConstants.KEY_SPACE, "Espaço") }, Modifier.weight(1.6f).testTag("key_space"))
        }
    }
}

@Composable
private fun FunctionAndNumpadContent(
    onSendSpecialKey: (Int, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // F1 - F12 scrollable strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val fKeys = listOf(
                "F1" to HidConstants.KEY_F1,
                "F2" to HidConstants.KEY_F2,
                "F3" to HidConstants.KEY_F3,
                "F4" to HidConstants.KEY_F4,
                "F5" to HidConstants.KEY_F5,
                "F6" to HidConstants.KEY_F6,
                "F7" to HidConstants.KEY_F7,
                "F8" to HidConstants.KEY_F8,
                "F9" to HidConstants.KEY_F9,
                "F10" to HidConstants.KEY_F10,
                "F11" to HidConstants.KEY_F11,
                "F12" to HidConstants.KEY_F12
            )
            fKeys.forEach { (label, code) ->
                SpecialKeyButton(
                    label = label,
                    onClick = { onSendSpecialKey(code, label) },
                    modifier = Modifier.width(50.dp).testTag("key_${label.lowercase()}")
                )
            }
        }

        // Navigation strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SpecialKeyButton("HOME", { onSendSpecialKey(HidConstants.KEY_HOME, "Home") }, Modifier.weight(1f))
            SpecialKeyButton("END", { onSendSpecialKey(HidConstants.KEY_END, "End") }, Modifier.weight(1f))
            SpecialKeyButton("PGUP", { onSendSpecialKey(HidConstants.KEY_PAGE_UP, "PgUp") }, Modifier.weight(1f))
            SpecialKeyButton("PGDN", { onSendSpecialKey(HidConstants.KEY_PAGE_DOWN, "PgDn") }, Modifier.weight(1f))
            SpecialKeyButton("PRTSC", { onSendSpecialKey(HidConstants.KEY_PRINT_SCREEN, "PrtSc") }, Modifier.weight(1f))
        }

        // Compact Numpad rows
        val numRows = listOf(
            listOf("7" to HidConstants.KEY_KP_7, "8" to HidConstants.KEY_KP_8, "9" to HidConstants.KEY_KP_9, "/" to HidConstants.KEY_KP_DIVIDE),
            listOf("4" to HidConstants.KEY_KP_4, "5" to HidConstants.KEY_KP_5, "6" to HidConstants.KEY_KP_6, "*" to HidConstants.KEY_KP_MULTIPLY),
            listOf("1" to HidConstants.KEY_KP_1, "2" to HidConstants.KEY_KP_2, "3" to HidConstants.KEY_KP_3, "-" to HidConstants.KEY_KP_MINUS),
            listOf("0" to HidConstants.KEY_KP_0, "." to HidConstants.KEY_KP_DOT, "+" to HidConstants.KEY_KP_PLUS, "ENT" to HidConstants.KEY_KP_ENTER)
        )
        numRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { (label, code) ->
                    SpecialKeyButton(
                        label = label,
                        highlighted = label == "ENT",
                        onClick = { onSendSpecialKey(code, "Num $label") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutsGridContent(
    onSendShortcutPreset: (ShortcutPreset) -> Unit
) {
    val rows = HidKeyMapper.shortcutPresets.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { preset ->
                    Surface(
                        onClick = { onSendShortcutPreset(preset) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("shortcut_${preset.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = preset.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModifierChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun SpecialKeyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .height(44.dp)
            .minimumInteractiveComponentSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(PaddingValues(horizontal = 6.dp))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SpecialKeyIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .height(44.dp)
            .minimumInteractiveComponentSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (highlighted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
