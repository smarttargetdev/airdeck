package br.com.smarttarget.android.airdeck.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.smarttarget.android.airdeck.R
import br.com.smarttarget.android.airdeck.domain.ReconnectionState
import br.com.smarttarget.android.airdeck.hid.HidConnectionState
import br.com.smarttarget.android.airdeck.hid.HidPacketStats
import br.com.smarttarget.android.airdeck.ui.theme.AmberWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusHeaderBar(
    connectionState: HidConnectionState,
    reconnectionState: ReconnectionState = ReconnectionState.Idle,
    packetStats: HidPacketStats,
    powerSaverMode: Boolean,
    animationsEnabled: Boolean,
    onTogglePowerSaver: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val targetStatusColor = when {
        reconnectionState is ReconnectionState.Reconnecting -> AmberWarning
        connectionState is HidConnectionState.Connected -> MaterialTheme.colorScheme.tertiary
        connectionState is HidConnectionState.Connecting ||
            connectionState is HidConnectionState.Registering ||
            connectionState is HidConnectionState.Disconnected -> MaterialTheme.colorScheme.primary
        connectionState is HidConnectionState.PermissionRequired ||
            connectionState is HidConnectionState.BluetoothDisabled -> AmberWarning
        else -> MaterialTheme.colorScheme.error
    }

    val statusColor by animateColorAsState(
        targetValue = targetStatusColor,
        animationSpec = if (animationsEnabled) tween(220) else tween(0),
        label = "status_color"
    )

    val statusText = when {
        reconnectionState is ReconnectionState.Reconnecting ->
            "Reconectando (${reconnectionState.attempt}/${reconnectionState.maxAttempts})…"
        connectionState is HidConnectionState.Connected -> connectionState.device.name
        connectionState is HidConnectionState.Connecting -> "Conectando a ${connectionState.device.name}…"
        connectionState is HidConnectionState.Disconnected -> "Toque para conectar"
        connectionState is HidConnectionState.Registering -> stringResource(R.string.status_registering)
        connectionState is HidConnectionState.BluetoothDisabled -> stringResource(R.string.status_bt_off)
        connectionState is HidConnectionState.PermissionRequired -> stringResource(R.string.status_permission_needed)
        connectionState is HidConnectionState.Unsupported -> stringResource(R.string.status_unsupported)
        else -> "Erro de conexão"
    }

    val statusIcon = when (connectionState) {
        is HidConnectionState.Connected -> Icons.Rounded.BluetoothConnected
        is HidConnectionState.BluetoothDisabled,
        is HidConnectionState.Unsupported -> Icons.Rounded.BluetoothDisabled
        else -> Icons.Rounded.Bluetooth
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = if (packetStats.totalReportsSent > 0) {
                            "HID #${packetStats.totalReportsSent}"
                        } else {
                            "HID READY"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (packetStats.wasDeliveredToHost) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(4.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .clickable(onClick = onOpenDevices)
                    .minimumInteractiveComponentSize()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("connection_status_pill"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = statusIcon,
                    contentDescription = stringResource(R.string.cd_devices_button),
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            onClick = onTogglePowerSaver,
            shape = RoundedCornerShape(12.dp),
            color = if (powerSaverMode) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
                1.dp,
                if (powerSaverMode) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            ),
            modifier = Modifier
                .height(48.dp)
                .minimumInteractiveComponentSize()
                .testTag("power_saver_chip")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.BatterySaver,
                    contentDescription = "Modo economia de energia",
                    tint = if (powerSaverMode) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ECO",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (powerSaverMode) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = {
                PlainTooltip(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Text(
                        text = stringResource(R.string.cd_help_button),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            },
            state = rememberTooltipState()
        ) {
            IconButton(
                onClick = onOpenHelp,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .testTag("help_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = stringResource(R.string.cd_help_button),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.cd_settings_button),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
