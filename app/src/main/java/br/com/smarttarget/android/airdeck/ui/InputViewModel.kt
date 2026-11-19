package br.com.smarttarget.android.airdeck.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.smarttarget.android.airdeck.data.AppPreferencesState
import br.com.smarttarget.android.airdeck.data.SettingsStore
import br.com.smarttarget.android.airdeck.domain.ButtonActionType
import br.com.smarttarget.android.airdeck.domain.ControlProfile
import br.com.smarttarget.android.airdeck.domain.CustomActionButton
import br.com.smarttarget.android.airdeck.domain.GestureProfileDraft
import br.com.smarttarget.android.airdeck.domain.HapticFeedbackEngine
import br.com.smarttarget.android.airdeck.domain.HapticPattern
import br.com.smarttarget.android.airdeck.domain.ImeTextDiffer
import br.com.smarttarget.android.airdeck.domain.NavigationAction
import br.com.smarttarget.android.airdeck.domain.NoOpHapticFeedbackEngine
import br.com.smarttarget.android.airdeck.domain.PointerDeltaAccumulator
import br.com.smarttarget.android.airdeck.domain.PowerSaverPolicy
import br.com.smarttarget.android.airdeck.domain.ProfileOperations
import br.com.smarttarget.android.airdeck.domain.SwipeGesturePattern
import br.com.smarttarget.android.airdeck.domain.SwipeGesturePresets
import br.com.smarttarget.android.airdeck.hid.HidConstants
import br.com.smarttarget.android.airdeck.hid.HidReportSender
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InputViewModel(
    private val hidReportSender: HidReportSender,
    private val settingsStore: SettingsStore,
    private val deltaAccumulator: PointerDeltaAccumulator = PointerDeltaAccumulator(),
    private val hapticEngine: HapticFeedbackEngine = NoOpHapticFeedbackEngine
) : ViewModel() {

    private val preferencesState: StateFlow<AppPreferencesState> = settingsStore.stateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppPreferencesState()
        )

    private val _inputState = MutableStateFlow(
        InputUiState(activeSwipeGestures = preferencesState.value.activeProfile.swipeGestures)
    )
    val inputState: StateFlow<InputUiState> = _inputState.asStateFlow()

    private var rawButtonMask = HidConstants.MOUSE_NONE
    private var accumulatedZoom = 1f

    init {
        viewModelScope.launch {
            preferencesState.collect { prefs ->
                _inputState.update { it.copy(activeSwipeGestures = prefs.activeProfile.swipeGestures) }
            }
        }
    }

    fun onTouchGesture(event: TouchGestureEvent) {
        when (event) {
            is TouchGestureEvent.SingleFingerMove -> {
                emitPointerDelta(
                    rawDx = event.dx,
                    rawDy = event.dy,
                    sensitivity = preferencesState.value.activeProfile.pointerSensitivity
                )
            }
            TouchGestureEvent.SingleTap -> handleSingleTap()
            TouchGestureEvent.TwoFingerTap -> handleTwoFingerTap()
            is TouchGestureEvent.TwoFingerScroll -> handleScroll(event.deltaY)
            is TouchGestureEvent.PinchZoom -> handlePinchZoom(event.zoomFactor)
            is TouchGestureEvent.DragLockChanged -> setDragLock(event.active)
            is TouchGestureEvent.MouseButtonStateChanged -> setMouseButtonHeld(event.buttonMask, event.pressed)
            TouchGestureEvent.MiddleClick -> {
                emitHaptic(HapticPattern.CLICK)
                sendMouseClickPulse(HidConstants.MOUSE_MIDDLE)
            }
            is TouchGestureEvent.SwipeGesture -> handleSwipeGesture(event.pattern)
        }
    }

    fun onAirMouseDelta(rawDx: Float, rawDy: Float) {
        emitPointerDelta(
            rawDx = rawDx,
            rawDy = rawDy,
            sensitivity = preferencesState.value.activeProfile.airMouseSensitivity
        )
    }

    fun onKeyboardEvent(event: KeyboardInputEvent) {
        when (event) {
            is KeyboardInputEvent.ImeTextChanged -> handleImeTextChanged(event.newText)
            is KeyboardInputEvent.SpecialKeyPressed -> {
                emitHaptic(HapticPattern.STEP_TICK)
                handleSpecialKey(event.keyCode, event.label)
            }
            is KeyboardInputEvent.ShortcutTriggered -> {
                emitHaptic(HapticPattern.STEP_TICK)
                hidReportSender.sendKeyCombo(event.preset.modifiers, event.preset.keyCode)
                _inputState.update { it.copy(lastInputFeedback = event.preset.badge) }
            }
            is KeyboardInputEvent.ToggleModifier -> {
                emitHaptic(HapticPattern.STEP_TICK)
                toggleModifier(event.modifier)
            }
            KeyboardInputEvent.ToggleStickyLock -> {
                emitHaptic(HapticPattern.DRAG_LOCK)
                _inputState.update { state ->
                    state.copy(modifiers = state.modifiers.copy(stickyLock = !state.modifiers.stickyLock))
                }
            }
            KeyboardInputEvent.ClearImeBuffer -> {
                _inputState.update { it.copy(imeInputText = "") }
            }
        }
    }

    fun onConsumerMediaAction(mask: Int, label: String) {
        emitHaptic(HapticPattern.CLICK)
        hidReportSender.sendConsumerPulse(mask)
        _inputState.update { it.copy(lastInputFeedback = label) }
    }

    fun onCustomButtonPressed(button: CustomActionButton) {
        emitHaptic(HapticPattern.CLICK)
        when (button.actionType) {
            ButtonActionType.KEY_COMBO -> {
                hidReportSender.sendKeyCombo(button.modifiers, button.keyCode)
                _inputState.update { it.copy(lastInputFeedback = button.label) }
            }
            ButtonActionType.CONSUMER_MEDIA -> {
                hidReportSender.sendConsumerPulse(button.consumerMask)
                _inputState.update { it.copy(lastInputFeedback = button.label) }
            }
            ButtonActionType.MOUSE_CLICK -> {
                sendMouseClickPulse(button.mouseMask)
                _inputState.update { it.copy(lastInputFeedback = button.label) }
            }
        }
    }

    fun setStatusFeedback(message: String) {
        _inputState.update { it.copy(lastInputFeedback = message) }
    }

    // --- Custom Swipe Gesture Profile Editing & Saving ---

    fun startEditingGestureProfile(profile: ControlProfile = preferencesState.value.activeProfile) {
        _inputState.update {
            it.copy(
                gestureProfileDraft = GestureProfileDraft(
                    profileId = profile.id,
                    profileName = profile.name,
                    bindings = profile.swipeGestures.filterValues { action -> action != NavigationAction.NONE },
                    isDirty = false
                )
            )
        }
    }

    fun startCreatingCustomGestureProfile(baseProfile: ControlProfile = preferencesState.value.activeProfile) {
        _inputState.update {
            it.copy(
                gestureProfileDraft = GestureProfileDraft(
                    profileId = "gesture_profile_${System.currentTimeMillis()}",
                    profileName = "Gestos Personalizados",
                    bindings = baseProfile.swipeGestures.filterValues { action -> action != NavigationAction.NONE },
                    isDirty = true
                )
            )
        }
    }

    fun updateGestureProfileName(name: String) {
        val currentDraft = _inputState.value.gestureProfileDraft ?: return
        _inputState.update {
            it.copy(
                gestureProfileDraft = currentDraft.copy(
                    profileName = name,
                    isDirty = true
                )
            )
        }
    }

    fun updateGestureBinding(pattern: SwipeGesturePattern, action: NavigationAction) {
        val currentDraft = _inputState.value.gestureProfileDraft ?: run {
            val active = preferencesState.value.activeProfile
            GestureProfileDraft(
                profileId = active.id,
                profileName = active.name,
                bindings = active.swipeGestures.filterValues { it != NavigationAction.NONE }
            )
        }
        val nextBindings = currentDraft.bindings.toMutableMap()
        if (action == NavigationAction.NONE) {
            nextBindings.remove(pattern)
        } else {
            nextBindings[pattern] = action
        }
        _inputState.update {
            it.copy(
                gestureProfileDraft = currentDraft.copy(
                    bindings = nextBindings,
                    isDirty = true
                )
            )
        }
    }

    fun resetGestureProfileDraftToDefaults() {
        val currentDraft = _inputState.value.gestureProfileDraft ?: return
        _inputState.update {
            it.copy(
                gestureProfileDraft = currentDraft.copy(
                    bindings = SwipeGesturePresets.defaultsForProfileId(currentDraft.profileId),
                    isDirty = true
                )
            )
        }
    }

    fun discardGestureProfileDraft() {
        _inputState.update { it.copy(gestureProfileDraft = null) }
    }

    fun saveGestureProfile() {
        val draft = _inputState.value.gestureProfileDraft ?: return
        val prefs = preferencesState.value
        val cleanBindings = draft.bindings.filterValues { it != NavigationAction.NONE }
        val existingTarget = prefs.profiles.firstOrNull { it.id == draft.profileId }
        val updatedProfile = (existingTarget ?: prefs.activeProfile.copy(id = draft.profileId)).copy(
            name = draft.profileName.trim().ifBlank { existingTarget?.name ?: "Gestos Personalizados" },
            swipeGestures = cleanBindings
        )
        val result = ProfileOperations.upsertProfile(prefs.profiles, updatedProfile)
        viewModelScope.launch {
            settingsStore.saveProfiles(result.profiles, updatedProfile.id)
        }
        emitHaptic(HapticPattern.DRAG_LOCK)
        _inputState.update {
            it.copy(
                activeSwipeGestures = if (prefs.activeProfile.id == updatedProfile.id || existingTarget == null) {
                    cleanBindings
                } else {
                    it.activeSwipeGestures
                },
                gestureProfileDraft = null,
                lastInputFeedback = "Gestos salvos (${updatedProfile.name})"
            )
        }
    }

    fun saveCustomGestureProfile(
        profileName: String,
        bindings: Map<SwipeGesturePattern, NavigationAction>,
        profileId: String = "gesture_profile_${System.currentTimeMillis()}"
    ) {
        val prefs = preferencesState.value
        val cleanBindings = bindings.filterValues { it != NavigationAction.NONE }
        val newProfile = prefs.activeProfile.copy(
            id = profileId,
            name = profileName.trim().ifBlank { "Gestos Personalizados" },
            swipeGestures = cleanBindings
        )
        val result = ProfileOperations.upsertProfile(prefs.profiles, newProfile)
        viewModelScope.launch {
            settingsStore.saveProfiles(result.profiles, newProfile.id)
        }
        emitHaptic(HapticPattern.DRAG_LOCK)
        _inputState.update {
            it.copy(
                activeSwipeGestures = cleanBindings,
                gestureProfileDraft = null,
                lastInputFeedback = "Gestos salvos (${newProfile.name})"
            )
        }
    }

    fun resetTransientInput() {
        deltaAccumulator.reset()
        rawButtonMask = HidConstants.MOUSE_NONE
        accumulatedZoom = 1f
        _inputState.update {
            it.copy(
                isDragLocked = false,
                isLeftPressed = false,
                isRightPressed = false,
                isMiddlePressed = false
            )
        }
        hidReportSender.releaseAll()
    }

    private fun emitPointerDelta(rawDx: Float, rawDy: Float, sensitivity: Float) {
        val minInterval = PowerSaverPolicy.resolve(preferencesState.value.powerSaverMode).hidReportMinIntervalMs
        val delta = deltaAccumulator.accumulateMove(rawDx, rawDy, sensitivity, minInterval) ?: return
        hidReportSender.sendMouseReport(effectiveButtonMask(), delta.dx, delta.dy, 0)
    }

    private fun emitHaptic(pattern: HapticPattern) {
        val prefs = preferencesState.value
        val policy = PowerSaverPolicy.resolve(prefs.powerSaverMode)
        if (prefs.activeProfile.hapticFeedback && policy.hapticsAllowed) {
            hapticEngine.trigger(pattern)
        }
    }

    private fun handleScroll(deltaY: Float) {
        val prefs = preferencesState.value
        val minInterval = PowerSaverPolicy.resolve(prefs.powerSaverMode).hidReportMinIntervalMs
        val wheel = deltaAccumulator.accumulateScroll(
            rawDeltaY = deltaY,
            sensitivity = prefs.activeProfile.scrollSensitivity,
            naturalScroll = prefs.activeProfile.naturalScroll,
            minIntervalMs = minInterval
        ) ?: return
        emitHaptic(HapticPattern.STEP_TICK)
        hidReportSender.sendMouseReport(effectiveButtonMask(), 0, 0, wheel)
    }

    private fun handlePinchZoom(zoomFactor: Float) {
        if (zoomFactor <= 0f) return
        accumulatedZoom *= zoomFactor
        if (accumulatedZoom >= 1.12f) {
            accumulatedZoom = 1f
            emitHaptic(HapticPattern.STEP_TICK)
            hidReportSender.sendZoomScroll(1)
            _inputState.update { it.copy(lastInputFeedback = "Zoom +") }
        } else if (accumulatedZoom <= 0.88f) {
            accumulatedZoom = 1f
            emitHaptic(HapticPattern.STEP_TICK)
            hidReportSender.sendZoomScroll(-1)
            _inputState.update { it.copy(lastInputFeedback = "Zoom -") }
        }
    }

    private fun handleSwipeGesture(pattern: SwipeGesturePattern) {
        val action = preferencesState.value.activeProfile.swipeGestures[pattern] ?: NavigationAction.NONE
        if (action == NavigationAction.NONE) return
        emitHaptic(HapticPattern.STEP_TICK)
        if (action.consumerMask != HidConstants.CONSUMER_NONE) {
            hidReportSender.sendConsumerPulse(action.consumerMask)
        } else if (action.keyCode != HidConstants.KEY_NONE) {
            hidReportSender.sendKeyCombo(action.modifiers, action.keyCode)
        }
        _inputState.update { it.copy(lastInputFeedback = "${pattern.shortHint} → ${action.badge}") }
    }

    private fun handleSingleTap() {
        val state = _inputState.value
        if (state.isDragLocked) {
            setDragLock(false)
            return
        }
        val profile = preferencesState.value.activeProfile
        if (!profile.tapToClick) return
        val mask = if (profile.swapMouseButtons) HidConstants.MOUSE_RIGHT else HidConstants.MOUSE_LEFT
        emitHaptic(HapticPattern.CLICK)
        sendMouseClickPulse(mask)
    }

    private fun handleTwoFingerTap() {
        val profile = preferencesState.value.activeProfile
        if (!profile.tapToClick || !profile.twoFingerRightClick) return
        val mask = if (profile.swapMouseButtons) HidConstants.MOUSE_LEFT else HidConstants.MOUSE_RIGHT
        emitHaptic(HapticPattern.CLICK)
        sendMouseClickPulse(mask)
    }

    private fun setDragLock(active: Boolean) {
        val primaryMask = if (preferencesState.value.activeProfile.swapMouseButtons) {
            HidConstants.MOUSE_RIGHT
        } else {
            HidConstants.MOUSE_LEFT
        }
        if (_inputState.value.isDragLocked != active) {
            emitHaptic(HapticPattern.DRAG_LOCK)
        }
        _inputState.update { it.copy(isDragLocked = active) }
        hidReportSender.sendMouseReport(if (active) primaryMask else HidConstants.MOUSE_NONE, 0, 0, 0)
    }

    private fun setMouseButtonHeld(buttonMask: Int, pressed: Boolean) {
        val swap = preferencesState.value.activeProfile.swapMouseButtons
        val resolvedMask = if (swap) {
            when (buttonMask) {
                HidConstants.MOUSE_LEFT -> HidConstants.MOUSE_RIGHT
                HidConstants.MOUSE_RIGHT -> HidConstants.MOUSE_LEFT
                else -> buttonMask
            }
        } else {
            buttonMask
        }

        val wasHeld = (rawButtonMask and resolvedMask) != 0
        rawButtonMask = if (pressed) {
            rawButtonMask or resolvedMask
        } else {
            rawButtonMask and resolvedMask.inv()
        }

        if (pressed && !wasHeld) {
            emitHaptic(HapticPattern.CLICK)
        }

        val currentDrag = _inputState.value.isDragLocked
        val nextDrag = if (!pressed && currentDrag && buttonMask == HidConstants.MOUSE_LEFT) {
            false
        } else {
            currentDrag
        }

        _inputState.update {
            it.copy(
                isDragLocked = nextDrag,
                isLeftPressed = (rawButtonMask and HidConstants.MOUSE_LEFT) != 0,
                isRightPressed = (rawButtonMask and HidConstants.MOUSE_RIGHT) != 0,
                isMiddlePressed = (rawButtonMask and HidConstants.MOUSE_MIDDLE) != 0
            )
        }
        hidReportSender.sendMouseReport(effectiveButtonMask(), 0, 0, 0)
    }

    private fun effectiveButtonMask(): Int {
        val dragMask = if (_inputState.value.isDragLocked) {
            if (preferencesState.value.activeProfile.swapMouseButtons) {
                HidConstants.MOUSE_RIGHT
            } else {
                HidConstants.MOUSE_LEFT
            }
        } else {
            HidConstants.MOUSE_NONE
        }
        return rawButtonMask or dragMask
    }

    private fun sendMouseClickPulse(button: Int) {
        viewModelScope.launch {
            hidReportSender.sendMouseReport(button, 0, 0, 0)
            delay(22)
            hidReportSender.sendMouseReport(HidConstants.MOUSE_NONE, 0, 0, 0)
        }
    }

    private fun toggleModifier(modifier: KeyboardModifier) {
        _inputState.update { state ->
            val mods = state.modifiers
            val updated = when (modifier) {
                KeyboardModifier.CTRL -> mods.copy(ctrl = !mods.ctrl)
                KeyboardModifier.SHIFT -> mods.copy(shift = !mods.shift)
                KeyboardModifier.ALT -> mods.copy(alt = !mods.alt)
                KeyboardModifier.GUI -> mods.copy(gui = !mods.gui)
            }
            state.copy(modifiers = updated)
        }
    }

    private fun handleSpecialKey(keyCode: Int, label: String) {
        val mods = _inputState.value.modifiers
        val modMask = mods.toBitmask()
        hidReportSender.sendKeyCombo(modMask, keyCode)
        val nextMods = if (!mods.stickyLock && mods.hasActiveModifier) ModifierState() else mods
        _inputState.update {
            it.copy(
                modifiers = nextMods,
                lastInputFeedback = formatComboLabel(modMask, label)
            )
        }
    }

    private fun handleImeTextChanged(newText: String) {
        val state = _inputState.value
        val oldText = state.imeInputText
        val diff = ImeTextDiffer.computeDiff(
            oldText = oldText,
            newText = newText,
            layout = preferencesState.value.activeProfile.keyboardLayout,
            activeModifierMask = state.modifiers.toBitmask()
        )
        val nextMods = if (diff.consumedActiveModifier && !state.modifiers.stickyLock) {
            ModifierState()
        } else {
            state.modifiers
        }
        val feedback = when {
            diff.insertions.isNotEmpty() -> "\"${diff.insertions.takeLast(12)}\""
            diff.deletions > 0 -> "Backspace ×${diff.deletions}"
            else -> state.lastInputFeedback
        }
        _inputState.update {
            it.copy(
                imeInputText = newText,
                modifiers = nextMods,
                lastInputFeedback = feedback
            )
        }
        if (diff.strokes.isNotEmpty()) {
            hidReportSender.sendTextStrokes(diff.strokes)
        }
    }

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
