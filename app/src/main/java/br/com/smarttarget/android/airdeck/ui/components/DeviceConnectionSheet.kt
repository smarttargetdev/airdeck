package br.com.smarttarget.android.airdeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DevicesOther
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SettingsBluetooth
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.com.smarttarget.android.airdeck.R
import br.com.smarttarget.android.airdeck.domain.ReconnectionState
import br.com.smarttarget.android.airdeck.hid.BondedHostDevice
import br.com.smarttarget.android.airdeck.hid.DeviceBondState
import br.com.smarttarget.android.airdeck.hid.DiscoveredBluetoothDevice
import br.com.smarttarget.android.airdeck.hid.DiscoveryState
import br.com.smarttarget.android.airdeck.hid.HidConnectionState
import br.com.smarttarget.android.airdeck.hid.HostDeviceType
import br.com.smarttarget.android.airdeck.ui.theme.AmberWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceConnectionSheet(
    connectionState: HidConnectionState,
    discoveryState: DiscoveryState,
    reconnectionState: ReconnectionState = ReconnectionState.Idle,
    animationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefresh: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onPairDevice: (String) -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
    onCancelReconnect: () -> Unit = {}
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .clip(RoundedCornerShape(18.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_bt_hero_1795115885371),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Air Deck • Conexão Bluetooth HID",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dispositivos & Pareamento",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_devices_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Atualizar dispositivos",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (reconnectionState is ReconnectionState.Reconnecting) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reconectando a ${reconnectionState.device.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = AmberWarning
                            )
                            Text(
                                text = "Tentativa ${reconnectionState.attempt} de ${reconnectionState.maxAttempts}…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onCancelReconnect,
                            modifier = Modifier.testTag("sheet_cancel_reconnect_button")
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }

            when (connectionState) {
                is HidConnectionState.Unsupported -> {
                    StatusActionCard(
                        title = "Bluetooth HID indisponível",
                        subtitle = connectionState.reason,
                        actionLabel = null,
                        onAction = null
                    )
                }
                is HidConnectionState.PermissionRequired -> {
                    StatusActionCard(
                        title = "Permissão de Dispositivos Próximos",
                        subtitle = "O Air Deck precisa de permissão Bluetooth para descobrir dispositivos, parear e registrar o perfil HID.",
                        actionLabel = "Conceder Permissões",
                        onAction = onRequestPermissions,
                        buttonTag = "grant_bt_permissions_button"
                    )
                }
                is HidConnectionState.BluetoothDisabled -> {
                    StatusActionCard(
                        title = "Bluetooth está desligado",
                        subtitle = "Ative o Bluetooth do celular para buscar, parear e conectar a um computador, notebook ou Smart TV.",
                        actionLabel = "Ativar Bluetooth",
                        onAction = onEnableBluetooth,
                        buttonTag = "enable_bt_button"
                    )
                }
                is HidConnectionState.Registering -> {
                    StatusActionCard(
                        title = "Registrando perfil HID…",
                        subtitle = "Aguarde enquanto o serviço Bluetooth HID inicializa o descritor composto.",
                        actionLabel = "Atualizar",
                        onAction = onRefresh
                    )
                }
                is HidConnectionState.Connected -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BluetoothConnected,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = connectionState.device.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Conectado via HID • ${connectionState.device.address}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onDisconnect,
                                modifier = Modifier.testTag("disconnect_host_button")
                            ) {
                                Text("Desconectar")
                            }
                        }
                    }
                }
                is HidConnectionState.Connecting -> {
                    StatusActionCard(
                        title = "Conectando a ${connectionState.device.name}…",
                        subtitle = "Aceite o pareamento no computador ou TV caso seja solicitado.",
                        actionLabel = null,
                        onAction = null
                    )
                }
                is HidConnectionState.Disconnected,
                is HidConnectionState.Error -> {
                    val bondedDevices = when (connectionState) {
                        is HidConnectionState.Disconnected -> connectionState.bondedDevices
                        is HidConnectionState.Error -> connectionState.bondedDevices
                        else -> emptyList()
                    }
                    val isDiscoverable = (connectionState as? HidConnectionState.Disconnected)?.isDiscoverable == true

                    if (connectionState is HidConnectionState.Error) {
                        ErrorBannerCard(connectionState.message)
                    }
                    discoveryState.discoveryError?.let { discoveryErr ->
                        ErrorBannerCard(discoveryErr)
                    }

                    // Discovery, Visibility & System Settings Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (discoveryState.isDiscovering) onStopDiscovery() else onStartDiscovery()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("scan_devices_button")
                        ) {
                            if (discoveryState.isDiscovering && animationsEnabled) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (discoveryState.isDiscovering) Icons.Rounded.Stop else Icons.Rounded.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (discoveryState.isDiscovering) "Parar Busca" else "Procurar",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onMakeDiscoverable,
                            modifier = Modifier
                                .weight(1.1f)
                                .height(48.dp)
                                .testTag("make_discoverable_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BluetoothSearching,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDiscoverable) "Visível" else "Ficar Visível",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenBluetoothSettings,
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("open_bt_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SettingsBluetooth,
                                contentDescription = "Ajustes Bluetooth",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Nearby Discovered Devices (for Pairing)
                    val unpairedDiscovered = discoveryState.discoveredDevices.filter {
                        it.bondState != DeviceBondState.BONDED
                    }
                    if (discoveryState.isDiscovering || unpairedDiscovered.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dispositivos Próximos (${unpairedDiscovered.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (discoveryState.isDiscovering) {
                                Text(
                                    text = "Buscando…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        if (unpairedDiscovered.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Escaneando dispositivos Bluetooth próximos…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                unpairedDiscovered.forEach { discovered ->
                                    DiscoveredDeviceRow(
                                        device = discovered,
                                        isPairing = discoveryState.pairingDeviceAddress == discovered.address ||
                                            discovered.bondState == DeviceBondState.BONDING,
                                        onPair = { onPairDevice(discovered.address) }
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Dispositivos Pareados (${bondedDevices.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (bondedDevices.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bluetooth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Nenhum dispositivo pareado encontrado. Toque em 'Procurar' ou 'Ficar Visível' para parear com seu PC ou TV.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bondedDevices.forEach { device ->
                                BondedDeviceRow(
                                    device = device,
                                    onConnect = { onConnectDevice(device.address) }
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
private fun ErrorBannerCard(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun StatusActionCard(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    buttonTag: String? = null
) {
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .let { if (buttonTag != null) it.testTag(buttonTag) else it }
                ) {
                    Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DiscoveredDeviceRow(
    device: DiscoveredBluetoothDevice,
    isPairing: Boolean,
    onPair: () -> Unit
) {
    Surface(
        onClick = { if (!isPairing) onPair() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovered_device_${device.address}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = hostTypeIcon(device.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val rssiLabel = device.rssi?.let { " • $it dBm" } ?: ""
                    Text(
                        text = "${device.address}$rssiLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = if (isPairing) "Pareando…" else "Parear",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("pair_device_${device.address}")
            )
        }
    }
}

@Composable
private fun BondedDeviceRow(
    device: BondedHostDevice,
    onConnect: () -> Unit
) {
    Surface(
        onClick = onConnect,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bonded_device_${device.address}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = hostTypeIcon(device.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Conectar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun hostTypeIcon(type: HostDeviceType) = when (type) {
    HostDeviceType.COMPUTER -> Icons.Rounded.Computer
    HostDeviceType.TV_OR_MEDIA -> Icons.Rounded.Tv
    HostDeviceType.PHONE_OR_TABLET -> Icons.Rounded.PhoneAndroid
    HostDeviceType.OTHER -> Icons.Rounded.DevicesOther
}
