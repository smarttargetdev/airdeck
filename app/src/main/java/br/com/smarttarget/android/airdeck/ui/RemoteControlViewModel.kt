package br.com.smarttarget.android.airdeck.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.smarttarget.android.airdeck.data.AirMouseSensorManager
import br.com.smarttarget.android.airdeck.data.AppPreferencesState
import br.com.smarttarget.android.airdeck.data.SettingsStore
import br.com.smarttarget.android.airdeck.domain.BluetoothReconnectionBridge
import br.com.smarttarget.android.airdeck.domain.ControlProfile
import br.com.smarttarget.android.airdeck.domain.PowerSaverPolicy
import br.com.smarttarget.android.airdeck.domain.ProfileOperations
import br.com.smarttarget.android.airdeck.domain.ProfilePresets
import br.com.smarttarget.android.airdeck.domain.ReconnectionState
import br.com.smarttarget.android.airdeck.hid.DiscoveryState
import br.com.smarttarget.android.airdeck.hid.HidConnectionState
import br.com.smarttarget.android.airdeck.hid.HidPacketStats
import br.com.smarttarget.android.airdeck.hid.HidTransport
import br.com.smarttarget.android.airdeck.hid.KeyboardLayout
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ControlMode(val label: String) {
    TRACKPAD("Trackpad"),
    MEDIA_REMOTE("Mídia & TV"),
    AIR_MOUSE("Air Mouse")
}

enum class KeyboardPanelMode(val label: String) {
    HIDDEN("Oculto"),
    HYBRID_IME("Teclado"),
    FUNCTION_AND_NUMPAD("Controle"),
    SHORTCUTS("Atalhos")
}

data class RemoteUiState(
    val connectionState: HidConnectionState = HidConnectionState.Registering,
    val discoveryState: DiscoveryState = DiscoveryState(),
    val reconnectionState: ReconnectionState = ReconnectionState.Idle,
    val packetStats: HidPacketStats = HidPacketStats(),
    val powerSaverMode: Boolean = false,
    val profiles: List<ControlProfile> = ProfilePresets.defaultProfiles(),
    val activeProfile: ControlProfile = ProfilePresets.defaultProfiles().first(),
    val controlMode: ControlMode = ControlMode.TRACKPAD,
    val keyboardMode: KeyboardPanelMode = KeyboardPanelMode.HIDDEN,
    val isAirMouseStreaming: Boolean = false,
    val isAirMouseAvailable: Boolean = false,
    val airTiltYaw: Float = 0f,
    val airTiltPitch: Float = 0f,
    val showDeviceSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showHelpSheet: Boolean = false,
    val editingProfile: ControlProfile? = null
) {
    val effectiveHapticEnabled: Boolean
        get() = activeProfile.hapticFeedback && PowerSaverPolicy.resolve(powerSaverMode).hapticsAllowed

    val animationsEnabled: Boolean
        get() = PowerSaverPolicy.resolve(powerSaverMode).animationsEnabled
}

class RemoteControlViewModel(
    application: Application,
    val hidTransport: HidTransport,
    private val settingsStore: SettingsStore
) : AndroidViewModel(application) {

    private val _controlMode = MutableStateFlow(ControlMode.TRACKPAD)
    private val _keyboardMode = MutableStateFlow(KeyboardPanelMode.HIDDEN)
    private val _isAirMouseStreaming = MutableStateFlow(false)
    private val _airTilt = MutableStateFlow(0f to 0f)
    private val _showDeviceSheet = MutableStateFlow(false)
    private val _showSettingsSheet = MutableStateFlow(false)
    private val _showHelpSheet = MutableStateFlow(false)
    private val _editingProfile = MutableStateFlow<ControlProfile?>(null)

    private val _airMouseDeltas = MutableSharedFlow<Pair<Float, Float>>(extraBufferCapacity = 32)
    val airMouseDeltas: SharedFlow<Pair<Float, Float>> = _airMouseDeltas.asSharedFlow()

    private var latestPrefs = AppPreferencesState()

    private val airMouseManager = AirMouseSensorManager(
        context = application,
        onDelta = { dx, dy ->
            if (_isAirMouseStreaming.value && _controlMode.value == ControlMode.AIR_MOUSE) {
                _airMouseDeltas.tryEmit(dx to dy)
            }
        },
        onTiltSample = { yaw, pitch ->
            _airTilt.value = yaw to pitch
        }
    )

    private val uiFlagsFlow = combine(
        _controlMode,
        _keyboardMode,
        _isAirMouseStreaming,
        _airTilt,
        _showDeviceSheet,
        _showSettingsSheet,
        _showHelpSheet,
        _editingProfile,
        BluetoothReconnectionBridge.state
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val tilt = values[3] as Pair<Float, Float>
        UiFlagsSnapshot(
            controlMode = values[0] as ControlMode,
            keyboardMode = values[1] as KeyboardPanelMode,
            isAirMouseStreaming = values[2] as Boolean,
            tilt = tilt,
            showDeviceSheet = values[4] as Boolean,
            showSettingsSheet = values[5] as Boolean,
            showHelpSheet = values[6] as Boolean,
            editingProfile = values[7] as ControlProfile?,
            reconnectionState = values[8] as ReconnectionState
        )
    }

    val uiState: StateFlow<RemoteUiState> = combine(
        hidTransport.connectionState,
        hidTransport.discoveryState,
        hidTransport.packetStats,
        settingsStore.stateFlow,
        uiFlagsFlow
    ) { conn, discovery, stats, prefs, flags ->
        latestPrefs = prefs
        hidTransport.setPowerSaverMode(prefs.powerSaverMode)
        airMouseManager.setPowerSaverMode(prefs.powerSaverMode)

        RemoteUiState(
            connectionState = conn,
            discoveryState = discovery,
            reconnectionState = flags.reconnectionState,
            packetStats = stats,
            powerSaverMode = prefs.powerSaverMode,
            profiles = prefs.profiles,
            activeProfile = prefs.activeProfile,
            controlMode = flags.controlMode,
            keyboardMode = flags.keyboardMode,
            isAirMouseStreaming = flags.isAirMouseStreaming,
            isAirMouseAvailable = airMouseManager.isAvailable,
            airTiltYaw = flags.tilt.first,
            airTiltPitch = flags.tilt.second,
            showDeviceSheet = flags.showDeviceSheet,
            showSettingsSheet = flags.showSettingsSheet,
            showHelpSheet = flags.showHelpSheet,
            editingProfile = flags.editingProfile
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
        hidTransport.stopDiscovery()
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
    }

    // --- Bluetooth Connection & Discovery Manager Actions ---

    fun refreshBluetoothState() = hidTransport.refreshState()

    fun startDiscovery() = hidTransport.startDiscovery()

    fun stopDiscovery() = hidTransport.stopDiscovery()

    fun pairDevice(address: String) = hidTransport.pairDevice(address)

    fun connectToDevice(address: String) = hidTransport.connect(address)

    fun disconnectDevice() {
        BluetoothReconnectionBridge.requestCancel()
        hidTransport.disconnect()
    }

    fun cancelAutoReconnect() {
        BluetoothReconnectionBridge.requestCancel()
    }

    // --- Navigation & Mode Control ---

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
        if (visible) {
            hidTransport.refreshState()
        } else {
            hidTransport.stopDiscovery()
        }
    }

    fun setSettingsSheetVisible(visible: Boolean) {
        _showSettingsSheet.value = visible
    }

    fun setHelpSheetVisible(visible: Boolean) {
        _showHelpSheet.value = visible
    }

    // --- Power Saver Mode ---

    fun setPowerSaverMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setPowerSaverMode(enabled)
        }
        hidTransport.setPowerSaverMode(enabled)
        airMouseManager.setPowerSaverMode(enabled)
    }

    // --- Control Profiles Management ---

    fun selectProfile(profileId: String) {
        val target = latestPrefs.profiles.firstOrNull { it.id == profileId } ?: return
        viewModelScope.launch {
            settingsStore.setActiveProfileId(profileId)
        }
        setControlMode(target.defaultControlMode)
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

    // --- Air Mouse Sensor Streaming ---

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

    private data class UiFlagsSnapshot(
        val controlMode: ControlMode,
        val keyboardMode: KeyboardPanelMode,
        val isAirMouseStreaming: Boolean,
        val tilt: Pair<Float, Float>,
        val showDeviceSheet: Boolean,
        val showSettingsSheet: Boolean,
        val showHelpSheet: Boolean,
        val editingProfile: ControlProfile?,
        val reconnectionState: ReconnectionState
    )
}
