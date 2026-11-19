package br.com.smarttarget.android.controller.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.smarttarget.android.controller.data.AirMouseSensorManager
import br.com.smarttarget.android.controller.data.AppPreferencesState
import br.com.smarttarget.android.controller.data.PreferencesRepository
import br.com.smarttarget.android.controller.data.SettingsStore
import br.com.smarttarget.android.controller.domain.ButtonActionType
import br.com.smarttarget.android.controller.domain.ControlProfile
import br.com.smarttarget.android.controller.domain.CustomActionButton
import br.com.smarttarget.android.controller.domain.ImeTextDiffer
import br.com.smarttarget.android.controller.domain.PointerDeltaAccumulator
import br.com.smarttarget.android.controller.domain.PowerSaverPolicy
import br.com.smarttarget.android.controller.domain.ProfileOperations
import br.com.smarttarget.android.controller.domain.ProfilePresets
import br.com.smarttarget.android.controller.hid.BluetoothHidController
import br.com.smarttarget.android.controller.hid.HidConnectionState
import br.com.smarttarget.android.controller.hid.HidConstants
import br.com.smarttarget.android.controller.hid.HidPacketStats
import br.com.smarttarget.android.controller.hid.HidTransport
import br.com.smarttarget.android.controller.hid.KeyboardLayout
import br.com.smarttarget.android.controller.hid.ShortcutPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ControlMode(val label: String) {
    TRACKPAD("Trackpad"),
    MEDIA_REMOTE("Mídia & TV"),
    AIR_MOUSE("Air Mouse")
}

enum class KeyboardPanelMode(val label: String) {
    HIDDEN("Oculto"),
    HYBRID_IME("Teclado IME"),
    FUNCTION_AND_NUMPAD("F1–F12 & Num"),
    SHORTCUTS("Atalhos")
}

data class ModifierState(
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
    val gui: Boolean = false,
    val stickyLock: Boolean = false
) {
    fun toBitmask(): Int {
        var mask = 0
        if (ctrl) mask = mask or HidConstants.MOD_LEFT_CTRL
        if (shift) mask = mask or HidConstants.MOD_LEFT_SHIFT
        if (alt) mask = mask or HidConstants.MOD_LEFT_ALT
        if (gui) mask = mask or HidConstants.MOD_LEFT_GUI
        return mask
    }

    val hasActiveModifier: Boolean
        get() = ctrl || shift || alt || gui
}

data class RemoteUiState(
    val connectionState: HidConnectionState = HidConnectionState.Registering,
    val packetStats: HidPacketStats = HidPacketStats(),
    val powerSaverMode: Boolean = false,
    val profiles: List<ControlProfile> = ProfilePresets.defaultProfiles(),
    val activeProfile: ControlProfile = ProfilePresets.defaultProfiles().first(),
    val controlMode: ControlMode = ControlMode.TRACKPAD,
    val keyboardMode: KeyboardPanelMode = KeyboardPanelMode.HIDDEN,
    val modifiers: ModifierState = ModifierState(),
    val isDragLocked: Boolean = false,
    val isLeftPressed: Boolean = false,
    val isRightPressed: Boolean = false,
    val isMiddlePressed: Boolean = false,
    val isAirMouseStreaming: Boolean = false,
    val isAirMouseAvailable: Boolean = false,
    val airTiltYaw: Float = 0f,
    val airTiltPitch: Float = 0f,
    val showDeviceSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val editingProfile: ControlProfile? = null,
    val imeInputText: String = "",
    val lastKeyFeedback: String = ""
) {
    val effectiveHapticEnabled: Boolean
        get() = activeProfile.hapticFeedback && PowerSaverPolicy.resolve(powerSaverMode).hapticsAllowed

    val animationsEnabled: Boolean
        get() = PowerSaverPolicy.resolve(powerSaverMode).animationsEnabled
}

class RemoteControlViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsStore: SettingsStore = PreferencesRepository(application)
    val hidTransport: HidTransport = BluetoothHidController(application)
    private val deltaAccumulator = PointerDeltaAccumulator()

    private val _controlMode = MutableStateFlow(ControlMode.TRACKPAD)
    private val _keyboardMode = MutableStateFlow(KeyboardPanelMode.HIDDEN)
    private val _modifiers = MutableStateFlow(ModifierState())
    private val _isDragLocked = MutableStateFlow(false)
    private val _buttonMask = MutableStateFlow(0)
    private val _isAirMouseStreaming = MutableStateFlow(false)
    private val _airTilt = MutableStateFlow(0f to 0f)
    private val _showDeviceSheet = MutableStateFlow(false)
    private val _showSettingsSheet = MutableStateFlow(false)
    private val _editingProfile = MutableStateFlow<ControlProfile?>(null)
    private val _imeInputText = MutableStateFlow("")
    private val _lastKeyFeedback = MutableStateFlow("")

    private var latestPrefs = AppPreferencesState()

    private val airMouseManager = AirMouseSensorManager(
        context = application,
        onDelta = { dx, dy ->
            if (_isAirMouseStreaming.value && _controlMode.value == ControlMode.AIR_MOUSE) {
                emitPointerDelta(dx, dy, latestPrefs.activeProfile.airMouseSensitivity)
            }
        },
        onTiltSample = { yaw, pitch ->
            _airTilt.value = yaw to pitch
        }
    )

    val uiState: StateFlow<RemoteUiState> = combine(
        hidTransport.connectionState,
        hidTransport.packetStats,
        settingsStore.stateFlow,
        _controlMode,
        _keyboardMode,
        _modifiers,
        _isDragLocked,
        _buttonMask,
        _isAirMouseStreaming,
        _airTilt,
        _showDeviceSheet,
        _showSettingsSheet,
        _editingProfile,
        _imeInputText,
        _lastKeyFeedback
    ) { values ->
        val conn = values[0] as HidConnectionState
        val stats = values[1] as HidPacketStats
        val prefs = values[2] as AppPreferencesState
        latestPrefs = prefs
        hidTransport.setPowerSaverMode(prefs.powerSaverMode)
        airMouseManager.setPowerSaverMode(prefs.powerSaverMode)

        val mode = values[3] as ControlMode
        val kbMode = values[4] as KeyboardPanelMode
        val mods = values[5] as ModifierState
        val dragLocked = values[6] as Boolean
        val btnMask = values[7] as Int
        val airStreaming = values[8] as Boolean
        @Suppress("UNCHECKED_CAST")
        val tilt = values[9] as Pair<Float, Float>
        val deviceSheet = values[10] as Boolean
        val settingsSheet = values[11] as Boolean
        val editProfile = values[12] as ControlProfile?
        val imeText = values[13] as String
        val keyFeedback = values[14] as String

        RemoteUiState(
            connectionState = conn,
            packetStats = stats,
            powerSaverMode = prefs.powerSaverMode,
            profiles = prefs.profiles,
            activeProfile = prefs.activeProfile,
            controlMode = mode,
            keyboardMode = kbMode,
            modifiers = mods,
            isDragLocked = dragLocked,
            isLeftPressed = (btnMask and HidConstants.MOUSE_LEFT) != 0,
            isRightPressed = (btnMask and HidConstants.MOUSE_RIGHT) != 0,
            isMiddlePressed = (btnMask and HidConstants.MOUSE_MIDDLE) != 0,
            isAirMouseStreaming = airStreaming,
            isAirMouseAvailable = airMouseManager.isAvailable,
            airTiltYaw = tilt.first,
            airTiltPitch = tilt.second,
            showDeviceSheet = deviceSheet,
            showSettingsSheet = settingsSheet,
            editingProfile = editProfile,
            imeInputText = imeText,
            lastKeyFeedback = keyFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemoteUiState(isAirMouseAvailable = airMouseManager.isAvailable)
    )

    init {
        hidTransport.start()
    }

    override fun onCleared() {
        super.onCleared()
        airMouseManager.stop()
        hidTransport.stop()
    }

    fun onResumeLifecycle() {
        hidTransport.refreshState()
        if (_controlMode.value == ControlMode.AIR_MOUSE && _isAirMouseStreaming.value) {
            airMouseManager.start()
        }
    }

    fun onPauseLifecycle() {
        airMouseManager.stop()
        if (_isAirMouseStreaming.value) {
            _isAirMouseStreaming.value = false
        }
        hidTransport.releaseAll()
        deltaAccumulator.reset()
        _buttonMask.value = 0
        _isDragLocked.value = false
    }

    fun refreshBluetoothState() = hidTransport.refreshState()

    fun connectToDevice(address: String) = hidTransport.connect(address)

    fun disconnectDevice() = hidTransport.disconnect()

    fun setControlMode(mode: ControlMode) {
        _controlMode.value = mode
        if (mode != ControlMode.AIR_MOUSE) {
            _isAirMouseStreaming.value = false
            airMouseManager.stop()
        }
    }

    fun toggleKeyboardMode(target: KeyboardPanelMode) {
        _keyboardMode.value = if (_keyboardMode.value == target) {
            KeyboardPanelMode.HIDDEN
        } else {
            target
        }
    }

    fun closeKeyboardPanel() {
        _keyboardMode.value = KeyboardPanelMode.HIDDEN
    }

    fun setDeviceSheetVisible(visible: Boolean) {
        _showDeviceSheet.value = visible
        if (visible) hidTransport.refreshState()
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _showSettingsSheet.value = visible
    }

    // --- Power Saver Mode ---

    fun setPowerSaverMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setPowerSaverMode(enabled)
        }
        hidTransport.setPowerSaverMode(enabled)
        airMouseManager.setPowerSaverMode(enabled)
        _lastKeyFeedback.value = if (enabled) "Economia ativada" else "Modo padrão"
    }

    // --- Control Profiles Management ---

    fun selectProfile(profileId: String) {
        val target = latestPrefs.profiles.firstOrNull { it.id == profileId } ?: return
        viewModelScope.launch {
            settingsStore.setActiveProfileId(profileId)
        }
        deltaAccumulator.reset()
        setControlMode(target.defaultControlMode)
        _lastKeyFeedback.value = "Perfil: ${target.name}"
    }

    fun openNewProfileEditor() {
        val base = latestPrefs.activeProfile
        _editingProfile.value = base.copy(
            id = "profile_${System.currentTimeMillis()}",
            name = "Novo Perfil"
        )
    }

    fun openEditProfileEditor(profile: ControlProfile) {
        _editingProfile.value = profile
    }

    fun closeProfileEditor() {
        _editingProfile.value = null
    }

    fun saveProfile(profile: ControlProfile) {
        val result = ProfileOperations.upsertProfile(latestPrefs.profiles, profile)
        viewModelScope.launch {
            settingsStore.saveProfiles(result.profiles, result.activeProfileId)
        }
        setControlMode(profile.defaultControlMode)
        _editingProfile.value = null
        _lastKeyFeedback.value = "Salvo: ${profile.name}"
    }

    fun deleteProfile(profileId: String) {
        val result = ProfileOperations.deleteProfile(
            existing = latestPrefs.profiles,
            profileId = profileId,
            currentActiveId = latestPrefs.activeProfileId
        )
        viewModelScope.launch {
            settingsStore.saveProfiles(result.profiles, result.activeProfileId)
        }
        _editingProfile.value = null
    }

    fun duplicateProfile(profileId: String) {
        val result = ProfileOperations.duplicateProfile(
            existing = latestPrefs.profiles,
            sourceId = profileId,
            newId = "profile_${System.currentTimeMillis()}"
        )
        viewModelScope.launch {
            settingsStore.saveProfiles(result.profiles, result.activeProfileId)
        }
    }

    fun executeCustomButton(button: CustomActionButton) {
        when (button.actionType) {
            ButtonActionType.KEY_COMBO -> {
                hidTransport.sendKeyCombo(button.modifiers, button.keyCode)
                _lastKeyFeedback.value = button.label
            }
            ButtonActionType.CONSUMER_MEDIA -> {
                hidTransport.sendConsumerPulse(button.consumerMask)
                _lastKeyFeedback.value = button.label
            }
            ButtonActionType.MOUSE_CLICK -> {
                sendMouseClickPulse(button.mouseMask)
                _lastKeyFeedback.value = button.label
            }
        }
    }

    // --- Air Mouse ---

    fun toggleAirMouseStreaming() {
        if (!airMouseManager.isAvailable) return
        val next = !_isAirMouseStreaming.value
        _isAirMouseStreaming.value = next
        if (next) airMouseManager.start() else airMouseManager.stop()
    }

    fun setAirMouseHeld(held: Boolean) {
        if (!airMouseManager.isAvailable) return
        _isAirMouseStreaming.value = held
        if (held) airMouseManager.start() else airMouseManager.stop()
    }

    fun recenterAirMouse() {
        airMouseManager.recenter()
        _airTilt.value = 0f to 0f
    }

    // --- Trackpad & Mouse ---

    fun onTrackpadMove(dx: Float, dy: Float) {
        emitPointerDelta(dx, dy, latestPrefs.activeProfile.pointerSensitivity)
    }

    private fun emitPointerDelta(rawDx: Float, rawDy: Float, sensitivity: Float) {
        val interval = PowerSaverPolicy.resolve(latestPrefs.powerSaverMode).hidReportMinIntervalMs
        val delta = deltaAccumulator.accumulateMove(rawDx, rawDy, sensitivity, interval) ?: return
        hidTransport.sendMouseReport(effectiveButtonMask(), delta.dx, delta.dy, 0)
    }

    fun onTrackpadScroll(deltaY: Float) {
        val profile = latestPrefs.activeProfile
        val interval = PowerSaverPolicy.resolve(latestPrefs.powerSaverMode).hidReportMinIntervalMs
        val wheel = deltaAccumulator.accumulateScroll(
            rawDeltaY = deltaY,
            sensitivity = profile.scrollSensitivity,
            naturalScroll = profile.naturalScroll,
            minIntervalMs = interval
        ) ?: return
        hidTransport.sendMouseReport(effectiveButtonMask(), 0, 0, wheel)
    }

    fun onLeftClick() {
        if (_isDragLocked.value) {
            _isDragLocked.value = false
            hidTransport.sendMouseReport(HidConstants.MOUSE_NONE, 0, 0, 0)
            return
        }
        val mask = if (latestPrefs.activeProfile.swapMouseButtons) {
            HidConstants.MOUSE_RIGHT
        } else {
            HidConstants.MOUSE_LEFT
        }
        sendMouseClickPulse(mask)
    }

    fun onRightClick() {
        if (!latestPrefs.activeProfile.twoFingerRightClick) return
        val mask = if (latestPrefs.activeProfile.swapMouseButtons) {
            HidConstants.MOUSE_LEFT
        } else {
            HidConstants.MOUSE_RIGHT
        }
        sendMouseClickPulse(mask)
    }

    fun onMiddleClick() = sendMouseClickPulse(HidConstants.MOUSE_MIDDLE)

    fun setDragLock(active: Boolean) {
        _isDragLocked.value = active
        val primaryMask = if (latestPrefs.activeProfile.swapMouseButtons) {
            HidConstants.MOUSE_RIGHT
        } else {
            HidConstants.MOUSE_LEFT
        }
        hidTransport.sendMouseReport(if (active) primaryMask else HidConstants.MOUSE_NONE, 0, 0, 0)
    }

    fun setMouseButtonHeld(buttonMask: Int, pressed: Boolean) {
        val resolvedMask = if (latestPrefs.activeProfile.swapMouseButtons) {
            when (buttonMask) {
                HidConstants.MOUSE_LEFT -> HidConstants.MOUSE_RIGHT
                HidConstants.MOUSE_RIGHT -> HidConstants.MOUSE_LEFT
                else -> buttonMask
            }
        } else {
            buttonMask
        }

        val updated = if (pressed) {
            _buttonMask.value or resolvedMask
        } else {
            _buttonMask.value and resolvedMask.inv()
        }
        _buttonMask.value = updated
        if (!pressed && _isDragLocked.value && buttonMask == HidConstants.MOUSE_LEFT) {
            _isDragLocked.value = false
        }
        hidTransport.sendMouseReport(effectiveButtonMask(), 0, 0, 0)
    }

    private fun effectiveButtonMask(): Int {
        val dragMask = if (_isDragLocked.value) {
            if (latestPrefs.activeProfile.swapMouseButtons) HidConstants.MOUSE_RIGHT else HidConstants.MOUSE_LEFT
        } else 0
        return _buttonMask.value or dragMask
    }

    private fun sendMouseClickPulse(button: Int) {
        viewModelScope.launch {
            hidTransport.sendMouseReport(button, 0, 0, 0)
            delay(22)
            hidTransport.sendMouseReport(HidConstants.MOUSE_NONE, 0, 0, 0)
        }
    }

    // --- Keyboard & Shortcuts ---

    fun toggleCtrl() = _modifiers.update { it.copy(ctrl = !it.ctrl) }
    fun toggleShift() = _modifiers.update { it.copy(shift = !it.shift) }
    fun toggleAlt() = _modifiers.update { it.copy(alt = !it.alt) }
    fun toggleGui() = _modifiers.update { it.copy(gui = !it.gui) }
    fun toggleStickyLock() = _modifiers.update { it.copy(stickyLock = !it.stickyLock) }

    fun sendSpecialKey(keyCode: Int, label: String) {
        val modMask = _modifiers.value.toBitmask()
        hidTransport.sendKeyCombo(modMask, keyCode)
        _lastKeyFeedback.value = formatComboLabel(modMask, label)
        if (!_modifiers.value.stickyLock && _modifiers.value.hasActiveModifier) {
            _modifiers.value = ModifierState(stickyLock = false)
        }
    }

    fun sendShortcutPreset(preset: ShortcutPreset) {
        hidTransport.sendKeyCombo(preset.modifiers, preset.keyCode)
        _lastKeyFeedback.value = preset.badge
    }

    fun onImeTextChanged(newText: String) {
        val oldText = _imeInputText.value
        _imeInputText.value = newText

        val diff = ImeTextDiffer.computeDiff(
            oldText = oldText,
            newText = newText,
            layout = latestPrefs.activeProfile.keyboardLayout,
            activeModifierMask = _modifiers.value.toBitmask()
        )
        if (diff.consumedActiveModifier && !_modifiers.value.stickyLock) {
            _modifiers.value = ModifierState()
        }
        if (diff.strokes.isNotEmpty()) {
            hidTransport.sendTextStrokes(diff.strokes)
            _lastKeyFeedback.value = when {
                diff.insertions.isNotEmpty() -> "\"${diff.insertions.takeLast(12)}\""
                diff.deletions > 0 -> "Backspace ×${diff.deletions}"
                else -> ""
            }
        }
    }

    fun clearImeBuffer() {
        _imeInputText.value = ""
    }

    // --- Media ---

    fun sendConsumerAction(mask: Int, label: String) {
        hidTransport.sendConsumerPulse(mask)
        _lastKeyFeedback.value = label
    }

    // --- Active Profile Quick Setting Updates ---

    private fun mutateActiveProfile(transform: (ControlProfile) -> ControlProfile) {
        val updated = transform(latestPrefs.activeProfile)
        val result = ProfileOperations.upsertProfile(latestPrefs.profiles, updated)
        viewModelScope.launch {
            settingsStore.saveProfiles(result.profiles, result.activeProfileId)
        }
    }

    fun updatePointerSensitivity(value: Float) = mutateActiveProfile { it.copy(pointerSensitivity = value) }
    fun updateScrollSensitivity(value: Float) = mutateActiveProfile { it.copy(scrollSensitivity = value) }
    fun updateAirMouseSensitivity(value: Float) = mutateActiveProfile { it.copy(airMouseSensitivity = value) }
    fun updateNaturalScroll(enabled: Boolean) = mutateActiveProfile { it.copy(naturalScroll = enabled) }
    fun updateTapToClick(enabled: Boolean) = mutateActiveProfile { it.copy(tapToClick = enabled) }
    fun updateTwoFingerRightClick(enabled: Boolean) = mutateActiveProfile { it.copy(twoFingerRightClick = enabled) }
    fun updateHapticFeedback(enabled: Boolean) = mutateActiveProfile { it.copy(hapticFeedback = enabled) }
    fun updateShowMouseButtons(enabled: Boolean) = mutateActiveProfile { it.copy(showMouseButtons = enabled) }
    fun updateSwapMouseButtons(enabled: Boolean) = mutateActiveProfile { it.copy(swapMouseButtons = enabled) }
    fun updateKeyboardLayout(layout: KeyboardLayout) = mutateActiveProfile { it.copy(keyboardLayout = layout) }

    private fun formatComboLabel(modMask: Int, keyLabel: String): String {
        val parts = mutableListOf<String>()
        if ((modMask and HidConstants.MOD_LEFT_CTRL) != 0) parts.add("Ctrl")
        if ((modMask and HidConstants.MOD_LEFT_ALT) != 0) parts.add("Alt")
        if ((modMask and HidConstants.MOD_LEFT_SHIFT) != 0) parts.add("Shift")
        if ((modMask and HidConstants.MOD_LEFT_GUI) != 0) parts.add("Win")
        parts.add(keyLabel)
        return parts.joinToString("+")
    }
}
