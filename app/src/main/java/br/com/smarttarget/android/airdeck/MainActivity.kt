package br.com.smarttarget.android.airdeck

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import br.com.smarttarget.android.airdeck.domain.ReconnectionState
import br.com.smarttarget.android.airdeck.hid.HidConnectionState
import br.com.smarttarget.android.airdeck.service.BluetoothConnectionManagerService
import br.com.smarttarget.android.airdeck.ui.ControlMode
import br.com.smarttarget.android.airdeck.ui.ControllerViewModelFactory
import br.com.smarttarget.android.airdeck.ui.InputUiState
import br.com.smarttarget.android.airdeck.ui.InputViewModel
import br.com.smarttarget.android.airdeck.ui.KeyboardInputEvent
import br.com.smarttarget.android.airdeck.ui.KeyboardModifier
import br.com.smarttarget.android.airdeck.ui.KeyboardPanelMode
import br.com.smarttarget.android.airdeck.ui.RemoteControlViewModel
import br.com.smarttarget.android.airdeck.ui.RemoteUiState
import br.com.smarttarget.android.airdeck.ui.TouchGestureEvent
import br.com.smarttarget.android.airdeck.ui.components.AirMousePanel
import br.com.smarttarget.android.airdeck.ui.components.CustomActionsStrip
import br.com.smarttarget.android.airdeck.ui.components.DeviceConnectionSheet
import br.com.smarttarget.android.airdeck.ui.components.GestureProfileEditorSheet
import br.com.smarttarget.android.airdeck.ui.components.HelpReadmeSheet
import br.com.smarttarget.android.airdeck.ui.components.HybridKeyboardPanel
import br.com.smarttarget.android.airdeck.ui.components.MediaAndTvPanel
import br.com.smarttarget.android.airdeck.ui.components.ProfileEditorSheet
import br.com.smarttarget.android.airdeck.ui.components.ProfileSwitcherBar
import br.com.smarttarget.android.airdeck.ui.components.SettingsSheet
import br.com.smarttarget.android.airdeck.ui.components.StatusHeaderBar
import br.com.smarttarget.android.airdeck.ui.components.TrackpadSurface
import br.com.smarttarget.android.airdeck.ui.theme.AirDeckTheme
import br.com.smarttarget.android.airdeck.ui.theme.AmberWarning

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color(0xFF0B0E14).toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color(0xFF0B0E14).toArgb())
        )
        setContent {
            AirDeckTheme {
                AirDeckApp()
            }
        }
    }
}

@Composable
fun AirDeckApp() {
    val context = LocalContext.current
    val factory = remember(context.applicationContext) {
        ControllerViewModelFactory(context.applicationContext as Application)
    }
    val remoteViewModel: RemoteControlViewModel = viewModel(factory = factory)
    val inputViewModel: InputViewModel = viewModel(factory = factory)

    AirDeckScreen(
        remoteViewModel = remoteViewModel,
        inputViewModel = inputViewModel
    )
}

@Composable
fun AirDeckScreen(
    remoteViewModel: RemoteControlViewModel,
    inputViewModel: InputViewModel
) {
    val uiState by remoteViewModel.uiState.collectAsStateWithLifecycle()
    val inputState by inputViewModel.inputState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Forward Air Mouse gyroscope deltas into InputViewModel
    LaunchedEffect(remoteViewModel, inputViewModel) {
        remoteViewModel.airMouseDeltas.collect { (dx, dy) ->
            inputViewModel.onAirMouseDelta(dx, dy)
        }
    }

    // Bind to BluetoothConnectionManagerService to coordinate discovery, pairing, and HID foreground lifecycle
    DisposableEffect(context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) = Unit
            override fun onServiceDisconnected(name: ComponentName?) = Unit
        }
        val bindIntent = BluetoothConnectionManagerService.createBindIntent(context)
        runCatching {
            context.startService(bindIntent)
            context.bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)
        }
        onDispose {
            runCatching { context.unbindService(connection) }
        }
    }

    DisposableEffect(lifecycleOwner, remoteViewModel, inputViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> remoteViewModel.onResumeLifecycle()
                Lifecycle.Event.ON_PAUSE -> {
                    remoteViewModel.onPauseLifecycle()
                    inputViewModel.resetTransientInput()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        remoteViewModel.refreshBluetoothState()
    }

    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        remoteViewModel.refreshBluetoothState()
    }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        remoteViewModel.refreshBluetoothState()
    }

    fun requestPermissions() {
        val required = remoteViewModel.hidTransport.requiredRuntimePermissions()
        if (required.isNotEmpty()) {
            permissionLauncher.launch(required.toTypedArray())
        } else {
            remoteViewModel.refreshBluetoothState()
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
            remoteViewModel.closeKeyboardPanel()
        }
    }

    if (inputState.gestureProfileDraft != null) {
        BackHandler {
            inputViewModel.discardGestureProfileDraft()
        }
    }

    if (uiState.showHelpSheet) {
        BackHandler {
            remoteViewModel.setHelpSheetVisible(false)
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
                    reconnectionState = uiState.reconnectionState,
                    packetStats = uiState.packetStats,
                    powerSaverMode = uiState.powerSaverMode,
                    animationsEnabled = uiState.animationsEnabled,
                    onTogglePowerSaver = {
                        val next = !uiState.powerSaverMode
                        remoteViewModel.setPowerSaverMode(next)
                        inputViewModel.setStatusFeedback(if (next) "Economia ativada" else "Modo padrão")
                    },
                    onOpenDevices = { remoteViewModel.setDeviceSheetVisible(true) },
                    onOpenSettings = { remoteViewModel.setSettingsSheetVisible(true) },
                    onOpenHelp = { remoteViewModel.setHelpSheetVisible(true) }
                )

                AnimatedVisibility(
                    visible = uiState.reconnectionState is ReconnectionState.Reconnecting,
                    enter = animEnter,
                    exit = animExit
                ) {
                    val reconnecting = uiState.reconnectionState as? ReconnectionState.Reconnecting
                    if (reconnecting != null) {
                        InlineReconnectingBanner(
                            reconnecting = reconnecting,
                            onCancel = remoteViewModel::cancelAutoReconnect
                        )
                    }
                }

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

                ProfileSwitcherBar(
                    profiles = uiState.profiles,
                    activeProfile = uiState.activeProfile,
                    onSelectProfile = { profileId ->
                        remoteViewModel.selectProfile(profileId)
                        inputViewModel.resetTransientInput()
                    },
                    onCreateProfile = remoteViewModel::openNewProfileEditor,
                    onEditActiveProfile = { remoteViewModel.openEditProfileEditor(uiState.activeProfile) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                ModeSwitcherBar(
                    activeMode = uiState.controlMode,
                    keyboardOpen = uiState.keyboardMode != KeyboardPanelMode.HIDDEN,
                    isExpandedLayout = isExpandedLayout,
                    onSelectMode = remoteViewModel::setControlMode,
                    onToggleKeyboard = {
                        val next = if (uiState.keyboardMode == KeyboardPanelMode.HIDDEN) {
                            KeyboardPanelMode.HYBRID_IME
                        } else {
                            KeyboardPanelMode.HIDDEN
                        }
                        remoteViewModel.toggleKeyboardMode(next)
                    }
                )

                if (uiState.activeProfile.customButtons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    CustomActionsStrip(
                        buttons = uiState.activeProfile.customButtons,
                        hapticEnabled = uiState.effectiveHapticEnabled,
                        onExecuteButton = inputViewModel::onCustomButtonPressed,
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
                            inputState = inputState,
                            remoteViewModel = remoteViewModel,
                            inputViewModel = inputViewModel,
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
                                modifiers = inputState.modifiers,
                                imeInputText = inputState.imeInputText,
                                lastKeyFeedback = inputState.lastInputFeedback,
                                hapticEnabled = uiState.effectiveHapticEnabled,
                                animationsEnabled = uiState.animationsEnabled,
                                onSelectMode = { mode ->
                                    if (uiState.keyboardMode != mode) {
                                        remoteViewModel.toggleKeyboardMode(mode)
                                    }
                                },
                                onToggleCtrl = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.CTRL)) },
                                onToggleShift = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.SHIFT)) },
                                onToggleAlt = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.ALT)) },
                                onToggleGui = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.GUI)) },
                                onToggleStickyLock = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleStickyLock) },
                                onImeTextChanged = { text -> inputViewModel.onKeyboardEvent(KeyboardInputEvent.ImeTextChanged(text)) },
                                onClearImeBuffer = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ClearImeBuffer) },
                                onSendSpecialKey = { code, label ->
                                    inputViewModel.onKeyboardEvent(KeyboardInputEvent.SpecialKeyPressed(code, label))
                                },
                                onSendShortcutPreset = { preset ->
                                    inputViewModel.onKeyboardEvent(KeyboardInputEvent.ShortcutTriggered(preset))
                                },
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
                            inputState = inputState,
                            remoteViewModel = remoteViewModel,
                            inputViewModel = inputViewModel,
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
                                modifiers = inputState.modifiers,
                                imeInputText = inputState.imeInputText,
                                lastKeyFeedback = inputState.lastInputFeedback,
                                hapticEnabled = uiState.effectiveHapticEnabled,
                                animationsEnabled = uiState.animationsEnabled,
                                onSelectMode = { mode ->
                                    if (uiState.keyboardMode != mode) {
                                        remoteViewModel.toggleKeyboardMode(mode)
                                    }
                                },
                                onToggleCtrl = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.CTRL)) },
                                onToggleShift = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.SHIFT)) },
                                onToggleAlt = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.ALT)) },
                                onToggleGui = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.GUI)) },
                                onToggleStickyLock = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ToggleStickyLock) },
                                onImeTextChanged = { text -> inputViewModel.onKeyboardEvent(KeyboardInputEvent.ImeTextChanged(text)) },
                                onClearImeBuffer = { inputViewModel.onKeyboardEvent(KeyboardInputEvent.ClearImeBuffer) },
                                onSendSpecialKey = { code, label ->
                                    inputViewModel.onKeyboardEvent(KeyboardInputEvent.SpecialKeyPressed(code, label))
                                },
                                onSendShortcutPreset = { preset ->
                                    inputViewModel.onKeyboardEvent(KeyboardInputEvent.ShortcutTriggered(preset))
                                },
                                onClose = remoteViewModel::closeKeyboardPanel
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
            discoveryState = uiState.discoveryState,
            reconnectionState = uiState.reconnectionState,
            animationsEnabled = uiState.animationsEnabled,
            onDismiss = { remoteViewModel.setDeviceSheetVisible(false) },
            onRequestPermissions = ::requestPermissions,
            onEnableBluetooth = ::enableBluetooth,
            onMakeDiscoverable = ::makeDiscoverable,
            onOpenBluetoothSettings = ::openBluetoothSettings,
            onRefresh = remoteViewModel::refreshBluetoothState,
            onStartDiscovery = remoteViewModel::startDiscovery,
            onStopDiscovery = remoteViewModel::stopDiscovery,
            onPairDevice = remoteViewModel::pairDevice,
            onConnectDevice = remoteViewModel::connectToDevice,
            onDisconnect = remoteViewModel::disconnectDevice,
            onCancelReconnect = remoteViewModel::cancelAutoReconnect
        )
    }

    if (uiState.showSettingsSheet) {
        SettingsSheet(
            activeProfile = uiState.activeProfile,
            powerSaverMode = uiState.powerSaverMode,
            onDismiss = { remoteViewModel.setSettingsSheetVisible(false) },
            onPowerSaverChange = remoteViewModel::setPowerSaverMode,
            onPointerSensitivityChange = remoteViewModel::updatePointerSensitivity,
            onScrollSensitivityChange = remoteViewModel::updateScrollSensitivity,
            onAirMouseSensitivityChange = remoteViewModel::updateAirMouseSensitivity,
            onNaturalScrollChange = remoteViewModel::updateNaturalScroll,
            onTapToClickChange = remoteViewModel::updateTapToClick,
            onTwoFingerRightClickChange = remoteViewModel::updateTwoFingerRightClick,
            onShowMouseButtonsChange = remoteViewModel::updateShowMouseButtons,
            onSwapMouseButtonsChange = remoteViewModel::updateSwapMouseButtons,
            onHapticFeedbackChange = remoteViewModel::updateHapticFeedback,
            onKeyboardLayoutChange = remoteViewModel::updateKeyboardLayout,
            onOpenGestureEditor = {
                remoteViewModel.setSettingsSheetVisible(false)
                inputViewModel.startEditingGestureProfile(uiState.activeProfile)
            }
        )
    }

    if (uiState.showHelpSheet) {
        val readmeMarkdown = remember(context) {
            runCatching {
                context.assets.open("README.md").bufferedReader().use { it.readText() }
            }.getOrDefault("# Air Deck – Virtual Hardware\n\nDocumentação `README.md` indisponível no pacote.")
        }
        HelpReadmeSheet(
            readmeMarkdown = readmeMarkdown,
            onDismiss = { remoteViewModel.setHelpSheetVisible(false) }
        )
    }

    inputState.gestureProfileDraft?.let { draft ->
        GestureProfileEditorSheet(
            draft = draft,
            profiles = uiState.profiles,
            onSelectProfile = inputViewModel::startEditingGestureProfile,
            onCreateCustomProfile = { inputViewModel.startCreatingCustomGestureProfile(uiState.activeProfile) },
            onNameChange = inputViewModel::updateGestureProfileName,
            onBindingChange = inputViewModel::updateGestureBinding,
            onResetDefaults = inputViewModel::resetGestureProfileDraftToDefaults,
            onDismiss = inputViewModel::discardGestureProfileDraft,
            onSave = inputViewModel::saveGestureProfile
        )
    }

    uiState.editingProfile?.let { profileToEdit ->
        val isExisting = uiState.profiles.any { it.id == profileToEdit.id }
        ProfileEditorSheet(
            initialProfile = profileToEdit,
            isExistingProfile = isExisting,
            canDelete = uiState.profiles.size > 1,
            onDismiss = remoteViewModel::closeProfileEditor,
            onSave = remoteViewModel::saveProfile,
            onDuplicate = remoteViewModel::duplicateProfile,
            onDelete = remoteViewModel::deleteProfile
        )
    }
}

@Composable
private fun PrimaryControlSurface(
    uiState: RemoteUiState,
    inputState: InputUiState,
    remoteViewModel: RemoteControlViewModel,
    inputViewModel: InputViewModel,
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
                    isDragLocked = inputState.isDragLocked,
                    isLeftPressed = inputState.isLeftPressed,
                    isMiddlePressed = inputState.isMiddlePressed,
                    isRightPressed = inputState.isRightPressed,
                    activeSwipeGestures = inputState.activeSwipeGestures,
                    onTouchGesture = inputViewModel::onTouchGesture,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ControlMode.MEDIA_REMOTE -> {
                MediaAndTvPanel(
                    hapticEnabled = uiState.effectiveHapticEnabled,
                    onSendKey = { code, label ->
                        inputViewModel.onKeyboardEvent(KeyboardInputEvent.SpecialKeyPressed(code, label))
                    },
                    onSendConsumer = inputViewModel::onConsumerMediaAction,
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
                    onToggleStreaming = remoteViewModel::toggleAirMouseStreaming,
                    onSetHeld = remoteViewModel::setAirMouseHeld,
                    onRecenter = remoteViewModel::recenterAirMouse,
                    onLeftClick = { inputViewModel.onTouchGesture(TouchGestureEvent.SingleTap) },
                    onRightClick = { inputViewModel.onTouchGesture(TouchGestureEvent.TwoFingerTap) },
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
                    is HidConnectionState.BluetoothDisabled -> "Ative o bluetooth para conectar"
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
private fun InlineReconnectingBanner(
    reconnecting: ReconnectionState.Reconnecting,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("reconnecting_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reconectando a ${reconnecting.device.name}…",
                    style = MaterialTheme.typography.labelLarge,
                    color = AmberWarning
                )
                Text(
                    text = "Tentativa ${reconnecting.attempt} de ${reconnecting.maxAttempts}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_reconnect_button")
            ) {
                Text(
                    text = "Cancelar",
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
        Box(modifier = Modifier.weight(1f)) {
            ModeChip(
                icon = Icons.Rounded.Mouse,
                label = "Mouse",
                selected = activeMode == ControlMode.TRACKPAD,
                onClick = { onSelectMode(ControlMode.TRACKPAD) },
                buttonTag = "mode_trackpad",
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            ModeChip(
                icon = Icons.Rounded.SettingsRemote,
                label = "Mídia",
                selected = activeMode == ControlMode.MEDIA_REMOTE,
                onClick = { onSelectMode(ControlMode.MEDIA_REMOTE) },
                buttonTag = "mode_media",
                modifier = Modifier.fillMaxWidth()
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            ModeChip(
                icon = Icons.Rounded.Sensors,
                label = "Air",
                selected = activeMode == ControlMode.AIR_MOUSE,
                onClick = { onSelectMode(ControlMode.AIR_MOUSE) },
                buttonTag = "mode_air",
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (!isExpandedLayout) {
            Box(modifier = Modifier.weight(1f)) {
                ModeChip(
                    icon = if (keyboardOpen) Icons.Rounded.KeyboardHide else Icons.Rounded.Keyboard,
                    label = "Teclado",
                    selected = keyboardOpen,
                    onClick = onToggleKeyboard,
                    buttonTag = "toggle_keyboard_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    buttonTag: String,
    modifier: Modifier = Modifier
) {
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        state = tooltipState,
        modifier = modifier
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
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .minimumInteractiveComponentSize()
                .testTag(buttonTag)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
