package br.com.smarttarget.android.airdeck.ui

import br.com.smarttarget.android.airdeck.domain.GestureProfileDraft
import br.com.smarttarget.android.airdeck.domain.NavigationAction
import br.com.smarttarget.android.airdeck.domain.SwipeGesturePattern
import br.com.smarttarget.android.airdeck.hid.HidConstants
import br.com.smarttarget.android.airdeck.hid.ShortcutPreset

enum class KeyboardModifier {
    CTRL,
    SHIFT,
    ALT,
    GUI
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

sealed interface TouchGestureEvent {
    data class SingleFingerMove(val dx: Float, val dy: Float) : TouchGestureEvent
    data object SingleTap : TouchGestureEvent
    data object TwoFingerTap : TouchGestureEvent
    data class TwoFingerScroll(val deltaX: Float, val deltaY: Float) : TouchGestureEvent
    data class PinchZoom(val zoomFactor: Float) : TouchGestureEvent
    data class DragLockChanged(val active: Boolean) : TouchGestureEvent
    data class MouseButtonStateChanged(val buttonMask: Int, val pressed: Boolean) : TouchGestureEvent
    data object MiddleClick : TouchGestureEvent
    data class SwipeGesture(val pattern: SwipeGesturePattern) : TouchGestureEvent
}

sealed interface KeyboardInputEvent {
    data class ImeTextChanged(val newText: String) : KeyboardInputEvent
    data class SpecialKeyPressed(val keyCode: Int, val label: String) : KeyboardInputEvent
    data class ShortcutTriggered(val preset: ShortcutPreset) : KeyboardInputEvent
    data class ToggleModifier(val modifier: KeyboardModifier) : KeyboardInputEvent
    data object ToggleStickyLock : KeyboardInputEvent
    data object ClearImeBuffer : KeyboardInputEvent
}

data class InputUiState(
    val modifiers: ModifierState = ModifierState(),
    val isDragLocked: Boolean = false,
    val isLeftPressed: Boolean = false,
    val isRightPressed: Boolean = false,
    val isMiddlePressed: Boolean = false,
    val imeInputText: String = "",
    val lastInputFeedback: String = "",
    val activeSwipeGestures: Map<SwipeGesturePattern, NavigationAction> = emptyMap(),
    val gestureProfileDraft: GestureProfileDraft? = null
) {
    val activeMouseButtonMask: Int
        get() {
            var mask = 0
            if (isLeftPressed) mask = mask or HidConstants.MOUSE_LEFT
            if (isRightPressed) mask = mask or HidConstants.MOUSE_RIGHT
            if (isMiddlePressed) mask = mask or HidConstants.MOUSE_MIDDLE
            return mask
        }
}
