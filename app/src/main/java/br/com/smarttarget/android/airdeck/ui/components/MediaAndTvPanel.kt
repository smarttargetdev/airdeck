package br.com.smarttarget.android.airdeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.smarttarget.android.airdeck.hid.HidConstants

@Composable
fun MediaAndTvPanel(
    @Suppress("UNUSED_PARAMETER") hapticEnabled: Boolean,
    onSendKey: (keyCode: Int, label: String) -> Unit,
    onSendConsumer: (mask: Int, label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    fun triggerHaptic() = Unit

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxHeight < 360.dp
            val dpadBtnSize = if (compact) 48.dp else 56.dp
            val okSize = if (compact) 56.dp else 72.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(if (compact) 12.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MediaPillButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        label = "Voltar",
                        onClick = {
                            triggerHaptic()
                            onSendConsumer(HidConstants.CONSUMER_AC_BACK, "Voltar")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("media_back_button")
                    )
                    MediaPillButton(
                        icon = Icons.Rounded.Home,
                        label = "Início",
                        onClick = {
                            triggerHaptic()
                            onSendConsumer(HidConstants.CONSUMER_AC_HOME, "Início")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("media_home_button")
                    )
                    MediaPillButton(
                        icon = Icons.Rounded.Search,
                        label = "Buscar",
                        onClick = {
                            triggerHaptic()
                            onSendConsumer(HidConstants.CONSUMER_AC_SEARCH, "Buscar")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("media_search_button")
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
                ) {
                    DPadButton(
                        icon = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Cima",
                        size = dpadBtnSize,
                        onClick = {
                            triggerHaptic()
                            onSendKey(HidConstants.KEY_UP, "Cima")
                        },
                        modifier = Modifier.testTag("dpad_up")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DPadButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Esquerda",
                            size = dpadBtnSize,
                            onClick = {
                                triggerHaptic()
                                onSendKey(HidConstants.KEY_LEFT, "Esquerda")
                            },
                            modifier = Modifier.testTag("dpad_left")
                        )
                        Surface(
                            onClick = {
                                triggerHaptic()
                                onSendKey(HidConstants.KEY_ENTER, "OK")
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .size(okSize)
                                .testTag("dpad_ok")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "OK",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        DPadButton(
                            icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Direita",
                            size = dpadBtnSize,
                            onClick = {
                                triggerHaptic()
                                onSendKey(HidConstants.KEY_RIGHT, "Direita")
                            },
                            modifier = Modifier.testTag("dpad_right")
                        )
                    }
                    DPadButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Baixo",
                        size = dpadBtnSize,
                        onClick = {
                            triggerHaptic()
                            onSendKey(HidConstants.KEY_DOWN, "Baixo")
                        },
                        modifier = Modifier.testTag("dpad_down")
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaIconButton(
                            icon = Icons.Rounded.SkipPrevious,
                            contentDescription = "Anterior",
                            onClick = {
                                triggerHaptic()
                                onSendConsumer(HidConstants.CONSUMER_PREV, "Anterior")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("media_prev_button")
                        )
                        MediaIconButton(
                            icon = Icons.Rounded.PlayArrow,
                            contentDescription = "Reproduzir ou Pausar",
                            highlighted = true,
                            onClick = {
                                triggerHaptic()
                                onSendConsumer(HidConstants.CONSUMER_PLAY_PAUSE, "Play/Pause")
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("media_play_pause_button")
                        )
                        MediaIconButton(
                            icon = Icons.Rounded.Stop,
                            contentDescription = "Parar",
                            onClick = {
                                triggerHaptic()
                                onSendConsumer(HidConstants.CONSUMER_STOP, "Stop")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("media_stop_button")
                        )
                        MediaIconButton(
                            icon = Icons.Rounded.SkipNext,
                            contentDescription = "Próxima",
                            onClick = {
                                triggerHaptic()
                                onSendConsumer(HidConstants.CONSUMER_NEXT, "Próxima")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("media_next_button")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaPillButton(
                            icon = Icons.AutoMirrored.Rounded.VolumeDown,
                            label = "Vol -",
                            onClick = {
                                triggerHaptic()
                                onSendConsumer(HidConstants.CONSUMER_VOL_DOWN, "Volume -")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("media_vol_down")
                        )
                        MediaPillButton(
                            icon = Icons.AutoMirrored.Rounded.VolumeOff,
                            label = "Mudo",
                            onClick = {
                                triggerHaptic()
                                onSendConsumer(HidConstants.CONSUMER_MUTE, "Mudo")
                            },
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("media_mute")
                        )
                        MediaPillButton(
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            label = "Vol +",
                            onClick = {
                                triggerHaptic()
                                onSendConsumer(HidConstants.CONSUMER_VOL_UP, "Volume +")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("media_vol_up")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DPadButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .size(size)
            .minimumInteractiveComponentSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun MediaIconButton(
    icon: ImageVector,
    contentDescription: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.height(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (highlighted) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun MediaPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
