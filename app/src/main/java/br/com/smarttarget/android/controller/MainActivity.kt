package br.com.smarttarget.android.controller

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.smarttarget.android.controller.hid.HidConnectionState
import br.com.smarttarget.android.controller.ui.ControlMode
import br.com.smarttarget.android.controller.ui.KeyboardPanelMode
import br.com.smarttarget.android.controller.ui.RemoteControlViewModel
import br.com.smarttarget.android.controller.ui.RemoteUiState
import br.com.smarttarget.android.controller.ui.components.AirMousePanel
import br.com.smarttarget.android.controller.ui.components.CustomActionsStrip
import br.com.smarttarget.android.controller.ui.components.DeviceConnectionSheet
import br.com.smarttarget.android.controller.ui.components.HybridKeyboardPanel
import br.com.smarttarget.android.controller.ui.components.MediaAndTvPanel
import br.com.smarttarget.android.controller.ui.components.ProfileEditorSheet
import br.com.smarttarget.android.controller.ui.components.ProfileSwitcherBar
import br.com.smarttarget.android.controller.ui.components.SettingsSheet
import br.com.smarttarget.android.controller.ui.components.StatusHeaderBar
import br.com.smarttarget.android.controller.ui.components.TrackpadSurface
import br.com.smarttarget.android.controller.ui.theme.SmartiesControllerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color(0xFF0B0E14).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color(0xFF0B0E14).toArgb())
        )
        setContent {
            SmartiesControllerTheme {
                SmartiesControllerApp()
            }
        }
    }
}

@Composable
fun SmartiesControllerApp(
    viewModel: RemoteControlViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResumeLifecycle()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPauseLifecycle()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshBluetoothState()
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetoothState()
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetoothState()
    }

    fun requestPermissions() {
        val required = viewModel.hidTransport.requiredRuntimePermissions()
        if (required.isNotEmpty()) {
            permissionLauncher.launch(required.toTypedArray())
        } else {
            viewModel.refreshBluetoothState()
        }
    }

    fun enableBluetooth() {
        runCatching {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    fun makeDiscoverable() {
        runCatching {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
            }
            discoverableLauncher.launch(intent)
        }
    }

    fun openBluetoothSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
    }

    if (uiState.keyboardMode != KeyboardPanelMode.HIDDEN) {
        BackHandler {
            viewModel.closeKeyboardPanel()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            val isExpandedLayout = maxWidth >= 720.dp
            val animEnter = if (uiState.animationsEnabled) expandVertically() + fadeIn() else EnterTransition.None
            val animExit = if (uiState.animationsEnabled) shrinkVertically() + fadeOut() else ExitTransition.None

            Column(modifier = Modifier.fillMaxSize()) {
                StatusHeaderBar(
                    connectionState = uiState.connectionState,
                    packetStats = uiState.packetStats,
                    powerSaverMode = uiState.powerSaverMode,
                    animationsEnabled = uiState.animationsEnabled,
                    onTogglePowerSaver = { viewModel.setPowerSaverMode(!uiState.powerSaverMode) },
                    onOpenDevices = { viewModel.setDeviceSheetVisible(true) },
                    onOpenSettings = { viewModel.setSettingsSheetVisible(true) }
                )

                AnimatedVisibility(
                    visible = uiState.connectionState is HidConnectionState.PermissionRequired ||
                        uiState.connectionState is HidConnectionState.BluetoothDisabled,
                    enter = animEnter,
                    exit = animExit
                ) {
                    InlineBluetoothBanner(
                        connectionState = uiState.connectionState,
                        onRequestPermissions = ::requestPermissions,
                        onEnableBluetooth = ::enableBluetooth
                    )
                }

                // Customizable Control Profile Switcher
                ProfileSwitcherBar(
                    profiles = uiState.profiles,
                    activeProfile = uiState.activeProfile,
                    onSelectProfile = viewModel::selectProfile,
                    onCreateProfile = viewModel::openNewProfileEditor,
                    onEditActiveProfile = { viewModel.openEditProfileEditor(uiState.activeProfile) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Mode Switcher Row
                ModeSwitcherBar(
                    activeMode = uiState.controlMode,
                    keyboardOpen = uiState.keyboardMode != KeyboardPanelMode.HIDDEN,
                    isExpandedLayout = isExpandedLayout,
                    onSelectMode = viewModel::setControlMode,
                    onToggleKeyboard = {
                        val next = if (uiState.keyboardMode == KeyboardPanelMode.HIDDEN) {
                            KeyboardPanelMode.HYBRID_IME
                        } else {
                            KeyboardPanelMode.HIDDEN
                        }
                        viewModel.toggleKeyboardMode(next)
                    }
                )

                // Active Profile Custom Action Buttons Bar
                if (uiState.activeProfile.customButtons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CustomActionsStrip(
                        buttons = uiState.activeProfile.customButtons,
                        hapticEnabled = uiState.effectiveHapticEnabled,
                        onExecuteButton = viewModel::executeCustomButton,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isExpandedLayout) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PrimaryControlSurface(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                        )
                        val activeKbMode = if (uiState.keyboardMode == KeyboardPanelMode.HIDDEN) {
                            KeyboardPanelMode.HYBRID_IME
                        } else {
                            uiState.keyboardMode
                        }
                        Box(
                            modifier = Modifier
                                .weight(0.95f)
                                .fillMaxHeight()
                        ) {
                            HybridKeyboardPanel(
                                keyboardMode = activeKbMode,
                                modifiers = uiState.modifiers,
                                imeInputText = uiState.imeInputText,
                                lastKeyFeedback = uiState.lastKeyFeedback,
                                hapticEnabled = uiState.effectiveHapticEnabled,
                                animationsEnabled = uiState.animationsEnabled,
                                onSelectMode = { mode ->
                                    if (uiState.keyboardMode != mode) {
                                        viewModel.toggleKeyboardMode(mode)
                                    }
                                },
                                onToggleCtrl = viewModel::toggleCtrl,
                                onToggleShift = viewModel::toggleShift,
                                onToggleAlt = viewModel::toggleAlt,
                                onToggleGui = viewModel::toggleGui,
                                onToggleStickyLock = viewModel::toggleStickyLock,
                                onImeTextChanged = viewModel::onImeTextChanged,
                                onClearImeBuffer = viewModel::clearImeBuffer,
                                onSendSpecialKey = viewModel::sendSpecialKey,
                                onSendShortcutPreset = viewModel::sendShortcutPreset,
                                onClose = null
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryControlSurface(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        AnimatedVisibility(
                            visible = uiState.keyboardMode != KeyboardPanelMode.HIDDEN,
                            enter = animEnter,
                            exit = animExit
                        ) {
                            HybridKeyboardPanel(
                                keyboardMode = uiState.keyboardMode,
                                modifiers = uiState.modifiers,
                                imeInputText = uiState.imeInputText,
                                lastKeyFeedback = uiState.lastKeyFeedback,
                                hapticEnabled = uiState.effectiveHapticEnabled,
                                animationsEnabled = uiState.animationsEnabled,
                                onSelectMode = { mode ->
                                    if (uiState.keyboardMode != mode) {
                                        viewModel.toggleKeyboardMode(mode)
                                    }
                                },
                                onToggleCtrl = viewModel::toggleCtrl,
                                onToggleShift = viewModel::toggleShift,
                                onToggleAlt = viewModel::toggleAlt,
                                onToggleGui = viewModel::toggleGui,
                                onToggleStickyLock = viewModel::toggleStickyLock,
                                onImeTextChanged = viewModel::onImeTextChanged,
                                onClearImeBuffer = viewModel::clearImeBuffer,
                                onSendSpecialKey = viewModel::sendSpecialKey,
                                onSendShortcutPreset = viewModel::sendShortcutPreset,
                                onClose = viewModel::closeKeyboardPanel
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeviceSheet) {
        DeviceConnectionSheet(
            connectionState = uiState.connectionState,
            onDismiss = { viewModel.setDeviceSheetVisible(false) },
            onRequestPermissions = ::requestPermissions,
            onEnableBluetooth = ::enableBluetooth,
            onMakeDiscoverable = ::makeDiscoverable,
            onOpenBluetoothSettings = ::openBluetoothSettings,
            onRefresh = viewModel::refreshBluetoothState,
            onConnectDevice = viewModel::connectToDevice,
            onDisconnect = viewModel::disconnectDevice
        )
    }

    if (uiState.showSettingsSheet) {
        SettingsSheet(
            activeProfile = uiState.activeProfile,
            powerSaverMode = uiState.powerSaverMode,
            onDismiss = { viewModel.setSettingsSheetVisible(false) },
            onPowerSaverChange = viewModel::setPowerSaverMode,
            onPointerSensitivityChange = viewModel::updatePointerSensitivity,
            onScrollSensitivityChange = viewModel::updateScrollSensitivity,
            onAirMouseSensitivityChange = viewModel::updateAirMouseSensitivity,
            onNaturalScrollChange = viewModel::updateNaturalScroll,
            onTapToClickChange = viewModel::updateTapToClick,
            onTwoFingerRightClickChange = viewModel::updateTwoFingerRightClick,
            onShowMouseButtonsChange = viewModel::updateShowMouseButtons,
            onSwapMouseButtonsChange = viewModel::updateSwapMouseButtons,
            onHapticFeedbackChange = viewModel::updateHapticFeedback,
            onKeyboardLayoutChange = viewModel::updateKeyboardLayout
        )
    }

    uiState.editingProfile?.let { profileToEdit ->
        val isExisting = uiState.profiles.any { it.id == profileToEdit.id }
        ProfileEditorSheet(
            initialProfile = profileToEdit,
            isExistingProfile = isExisting,
            canDelete = uiState.profiles.size > 1,
            onDismiss = viewModel::closeProfileEditor,
            onSave = viewModel::saveProfile,
            onDuplicate = viewModel::duplicateProfile,
            onDelete = viewModel::deleteProfile
        )
    }
}

@Composable
private fun PrimaryControlSurface(
    uiState: RemoteUiState,
    viewModel: RemoteControlViewModel,
    modifier: Modifier = Modifier
) {
    val profile = uiState.activeProfile
    Box(modifier = modifier) {
        when (uiState.controlMode) {
            ControlMode.TRACKPAD -> {
                TrackpadSurface(
                    tapToClick = profile.tapToClick,
                    twoFingerRightClick = profile.twoFingerRightClick,
                    showMouseButtons = profile.showMouseButtons,
                    swapMouseButtons = profile.swapMouseButtons,
                    hapticEnabled = uiState.effectiveHapticEnabled,
                    animationsEnabled = uiState.animationsEnabled,
                    isDragLocked = uiState.isDragLocked,
                    isLeftPressed = uiState.isLeftPressed,
                    isMiddlePressed = uiState.isMiddlePressed,
                    isRightPressed = uiState.isRightPressed,
                    onMove = viewModel::onTrackpadMove,
                    onScroll = viewModel::onTrackpadScroll,
                    onLeftClick = viewModel::onLeftClick,
                    onRightClick = viewModel::onRightClick,
                    onMiddleClick = viewModel::onMiddleClick,
                    onDragLockChange = viewModel::setDragLock,
                    onMouseButtonHeld = viewModel::setMouseButtonHeld,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ControlMode.MEDIA_REMOTE -> {
                MediaAndTvPanel(
                    hapticEnabled = uiState.effectiveHapticEnabled,
                    onSendKey = viewModel::sendSpecialKey,
                    onSendConsumer = viewModel::sendConsumerAction,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ControlMode.AIR_MOUSE -> {
                AirMousePanel(
                    isAvailable = uiState.isAirMouseAvailable,
                    isStreaming = uiState.isAirMouseStreaming,
                    yawRate = uiState.airTiltYaw,
                    pitchRate = uiState.airTiltPitch,
                    hapticEnabled = uiState.effectiveHapticEnabled,
                    animationsEnabled = uiState.animationsEnabled,
                    onToggleStreaming = viewModel::toggleAirMouseStreaming,
                    onSetHeld = viewModel::setAirMouseHeld,
                    onRecenter = viewModel::recenterAirMouse,
                    onLeftClick = viewModel::onLeftClick,
                    onRightClick = viewModel::onRightClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun InlineBluetoothBanner(
    connectionState: HidConnectionState,
    onRequestPermissions: () -> Unit,
    onEnableBluetooth: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (connectionState) {
                    is HidConnectionState.PermissionRequired -> "Autorize o Bluetooth para conectar"
                    is HidConnectionState.BluetoothDisabled -> "Ative o Bluetooth para usar como HID"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    when (connectionState) {
                        is HidConnectionState.PermissionRequired -> onRequestPermissions()
                        is HidConnectionState.BluetoothDisabled -> onEnableBluetooth()
                        else -> Unit
                    }
                },
                modifier = Modifier.testTag("banner_bt_action_button")
            ) {
                Text(
                    text = when (connectionState) {
                        is HidConnectionState.PermissionRequired -> "Permitir"
                        is HidConnectionState.BluetoothDisabled -> "Ligar"
                        else -> "OK"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ModeSwitcherBar(
    activeMode: ControlMode,
    keyboardOpen: Boolean,
    isExpandedLayout: Boolean,
    onSelectMode: (ControlMode) -> Unit,
    onToggleKeyboard: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeChip(
            icon = Icons.Rounded.Mouse,
            label = "Mouse",
            selected = activeMode == ControlMode.TRACKPAD,
            onClick = { onSelectMode(ControlMode.TRACKPAD) },
            modifier = Modifier
                .weight(1f)
                .testTag("mode_trackpad")
        )
        ModeChip(
            icon = Icons.Rounded.SettingsRemote,
            label = "Mídia",
            selected = activeMode == ControlMode.MEDIA_REMOTE,
            onClick = { onSelectMode(ControlMode.MEDIA_REMOTE) },
            modifier = Modifier
                .weight(1f)
                .testTag("mode_media")
        )
        ModeChip(
            icon = Icons.Rounded.Sensors,
            label = "Air",
            selected = activeMode == ControlMode.AIR_MOUSE,
            onClick = { onSelectMode(ControlMode.AIR_MOUSE) },
            modifier = Modifier
                .weight(1f)
                .testTag("mode_air")
        )
        if (!isExpandedLayout) {
            Surface(
                onClick = onToggleKeyboard,
                shape = RoundedCornerShape(12.dp),
                color = if (keyboardOpen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    1.dp,
                    if (keyboardOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .height(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("toggle_keyboard_button")
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (keyboardOpen) Icons.Rounded.KeyboardHide else Icons.Rounded.Keyboard,
                        contentDescription = stringResource(R.string.cd_keyboard_toggle),
                        tint = if (keyboardOpen) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .height(48.dp)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1
            )
        }
    }
}
