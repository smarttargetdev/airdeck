package br.com.smarttarget.android.airdeck

import br.com.smarttarget.android.airdeck.data.AppPreferencesState
import br.com.smarttarget.android.airdeck.data.SettingsStore
import br.com.smarttarget.android.airdeck.domain.ButtonActionType
import br.com.smarttarget.android.airdeck.domain.ControlProfile
import br.com.smarttarget.android.airdeck.domain.CustomActionButton
import br.com.smarttarget.android.airdeck.domain.HapticFeedbackEngine
import br.com.smarttarget.android.airdeck.domain.HapticPattern
import br.com.smarttarget.android.airdeck.domain.NavigationAction
import br.com.smarttarget.android.airdeck.domain.PointerDeltaAccumulator
import br.com.smarttarget.android.airdeck.domain.SwipeGesturePattern
import br.com.smarttarget.android.airdeck.hid.HidConstants
import br.com.smarttarget.android.airdeck.hid.HidKeyMapper
import br.com.smarttarget.android.airdeck.hid.HidKeyStroke
import br.com.smarttarget.android.airdeck.hid.HidPacketStats
import br.com.smarttarget.android.airdeck.hid.HidReportSender
import br.com.smarttarget.android.airdeck.hid.KeyboardLayout
import br.com.smarttarget.android.airdeck.ui.InputViewModel
import br.com.smarttarget.android.airdeck.ui.KeyboardInputEvent
import br.com.smarttarget.android.airdeck.ui.KeyboardModifier
import br.com.smarttarget.android.airdeck.ui.TouchGestureEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InputViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSender: FakeHidReportSender
    private lateinit var fakeStore: FakeSettingsStore
    private lateinit var fakeHaptics: RecordingHapticEngine
    private var fakeClockMs = 1000L

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSender = FakeHidReportSender()
        fakeStore = FakeSettingsStore()
        fakeHaptics = RecordingHapticEngine()
        fakeClockMs = 1000L
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): InputViewModel = InputViewModel(
        hidReportSender = fakeSender,
        settingsStore = fakeStore,
        deltaAccumulator = PointerDeltaAccumulator(clockMs = { fakeClockMs }),
        hapticEngine = fakeHaptics
    )

    @Test
    fun `single finger move sends mouse report and respects power saver throttle`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.SingleFingerMove(5f, 3f))
        assertEquals(1, fakeSender.mouseReports.size)
        assertTrue(fakeSender.mouseReports.first().dx > 0)

        // Enable Power Saver (45ms throttle)
        fakeStore.setPowerSaverMode(true)
        advanceUntilIdle()

        fakeClockMs += 15L
        vm.onTouchGesture(TouchGestureEvent.SingleFingerMove(4f, 0f))
        assertEquals(1, fakeSender.mouseReports.size)

        fakeClockMs += 35L
        vm.onTouchGesture(TouchGestureEvent.SingleFingerMove(4f, 0f))
        assertEquals(2, fakeSender.mouseReports.size)
    }

    @Test
    fun `taps respect tapToClick, twoFingerRightClick, and swapMouseButtons`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.SingleTap)
        advanceTimeBy(30)
        assertEquals(HidConstants.MOUSE_LEFT, fakeSender.mouseReports[0].buttons)
        assertEquals(HidConstants.MOUSE_NONE, fakeSender.mouseReports[1].buttons)

        fakeSender.mouseReports.clear()
        vm.onTouchGesture(TouchGestureEvent.TwoFingerTap)
        advanceTimeBy(30)
        assertEquals(HidConstants.MOUSE_RIGHT, fakeSender.mouseReports[0].buttons)

        // Enable left-handed swap
        fakeSender.mouseReports.clear()
        val swapped = fakeStore.current.activeProfile.copy(swapMouseButtons = true)
        fakeStore.saveProfiles(listOf(swapped), swapped.id)
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.SingleTap)
        advanceTimeBy(30)
        assertEquals(HidConstants.MOUSE_RIGHT, fakeSender.mouseReports[0].buttons)

        // Disable tapToClick
        fakeSender.mouseReports.clear()
        val noTap = swapped.copy(tapToClick = false)
        fakeStore.saveProfiles(listOf(noTap), noTap.id)
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.SingleTap)
        advanceTimeBy(30)
        assertTrue(fakeSender.mouseReports.isEmpty())
    }

    @Test
    fun `pinch zoom and two finger scroll emit expected HID reports`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.TwoFingerScroll(0f, 30f))
        assertEquals(1, fakeSender.mouseReports.size)
        assertTrue(fakeSender.mouseReports.first().wheel > 0)

        vm.onTouchGesture(TouchGestureEvent.PinchZoom(1.15f))
        assertEquals(listOf(1), fakeSender.zoomScrolls)
        assertEquals("Zoom +", vm.inputState.value.lastInputFeedback)

        vm.onTouchGesture(TouchGestureEvent.PinchZoom(0.85f))
        assertEquals(listOf(1, -1), fakeSender.zoomScrolls)
        assertEquals("Zoom -", vm.inputState.value.lastInputFeedback)
    }

    @Test
    fun `drag lock keeps mouse button pressed during movement until single tap`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.DragLockChanged(true))
        assertTrue(vm.inputState.value.isDragLocked)
        assertEquals(HidConstants.MOUSE_LEFT, fakeSender.mouseReports.last().buttons)

        fakeClockMs += 50L
        vm.onTouchGesture(TouchGestureEvent.SingleFingerMove(8f, 4f))
        assertEquals(HidConstants.MOUSE_LEFT, fakeSender.mouseReports.last().buttons)

        vm.onTouchGesture(TouchGestureEvent.SingleTap)
        assertFalse(vm.inputState.value.isDragLocked)
        assertEquals(HidConstants.MOUSE_NONE, fakeSender.mouseReports.last().buttons)
    }

    @Test
    fun `keyboard events handle sticky vs one-shot modifiers, IME diff, and shortcuts`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        // One-shot Ctrl + C
        vm.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.CTRL))
        vm.onKeyboardEvent(KeyboardInputEvent.SpecialKeyPressed(HidConstants.KEY_C, "C"))
        assertEquals(HidConstants.MOD_LEFT_CTRL to HidConstants.KEY_C, fakeSender.keyCombos.last())
        assertFalse(vm.inputState.value.modifiers.ctrl)

        // Sticky lock Alt + Tab keeps Alt active
        vm.onKeyboardEvent(KeyboardInputEvent.ToggleStickyLock)
        vm.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.ALT))
        vm.onKeyboardEvent(KeyboardInputEvent.SpecialKeyPressed(HidConstants.KEY_TAB, "Tab"))
        assertEquals(HidConstants.MOD_LEFT_ALT to HidConstants.KEY_TAB, fakeSender.keyCombos.last())
        assertTrue(vm.inputState.value.modifiers.alt)

        // IME text diff sends ABNT2 strokes then backspace on deletion
        vm.onKeyboardEvent(KeyboardInputEvent.ToggleStickyLock)
        vm.onKeyboardEvent(KeyboardInputEvent.ToggleModifier(KeyboardModifier.ALT))
        vm.onKeyboardEvent(KeyboardInputEvent.ImeTextChanged("oi"))
        assertEquals(2, fakeSender.textStrokes.last().size)
        assertEquals(HidConstants.KEY_O, fakeSender.textStrokes.last()[0].keyCode)
        assertEquals(HidConstants.KEY_I, fakeSender.textStrokes.last()[1].keyCode)

        vm.onKeyboardEvent(KeyboardInputEvent.ImeTextChanged("o"))
        assertEquals(listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)), fakeSender.textStrokes.last())

        // Shortcut preset
        val preset = HidKeyMapper.shortcutPresets.first()
        vm.onKeyboardEvent(KeyboardInputEvent.ShortcutTriggered(preset))
        assertEquals(preset.modifiers to preset.keyCode, fakeSender.keyCombos.last())
    }

    @Test
    fun `custom action buttons dispatch key combos, consumer media, and mouse clicks`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onCustomButtonPressed(
            CustomActionButton("b1", "Salvar", ButtonActionType.KEY_COMBO, HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_S)
        )
        assertEquals(HidConstants.MOD_LEFT_CTRL to HidConstants.KEY_S, fakeSender.keyCombos.last())

        vm.onCustomButtonPressed(
            CustomActionButton("b2", "Mudo", ButtonActionType.CONSUMER_MEDIA, consumerMask = HidConstants.CONSUMER_MUTE)
        )
        assertEquals(listOf(HidConstants.CONSUMER_MUTE), fakeSender.consumerPulses)

        vm.onCustomButtonPressed(
            CustomActionButton("b3", "Meio", ButtonActionType.MOUSE_CLICK, mouseMask = HidConstants.MOUSE_MIDDLE)
        )
        advanceTimeBy(30)
        assertEquals(HidConstants.MOUSE_MIDDLE, fakeSender.mouseReports.first().buttons)
    }

    @Test
    fun `haptic feedback emits distinct patterns per action and stays silent in ECO or disabled profile`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.SingleTap)
        vm.onTouchGesture(TouchGestureEvent.DragLockChanged(true))
        vm.onTouchGesture(TouchGestureEvent.PinchZoom(1.15f))
        vm.onKeyboardEvent(KeyboardInputEvent.ToggleStickyLock)
        vm.onKeyboardEvent(KeyboardInputEvent.SpecialKeyPressed(HidConstants.KEY_ENTER, "Enter"))
        vm.onConsumerMediaAction(HidConstants.CONSUMER_PLAY_PAUSE, "Play")
        advanceTimeBy(30)

        assertEquals(
            listOf(
                HapticPattern.CLICK,
                HapticPattern.DRAG_LOCK,
                HapticPattern.STEP_TICK,
                HapticPattern.DRAG_LOCK,
                HapticPattern.STEP_TICK,
                HapticPattern.CLICK
            ),
            fakeHaptics.patterns
        )

        // Disable all vibration in Power Saver (ECO) mode
        fakeHaptics.patterns.clear()
        fakeStore.setPowerSaverMode(true)
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.SingleTap)
        vm.onTouchGesture(TouchGestureEvent.DragLockChanged(false))
        vm.onKeyboardEvent(KeyboardInputEvent.SpecialKeyPressed(HidConstants.KEY_ESCAPE, "Esc"))
        advanceTimeBy(30)
        assertTrue(fakeHaptics.patterns.isEmpty())

        // Restore normal mode, disable hapticFeedback in profile
        fakeStore.setPowerSaverMode(false)
        val silentProfile = fakeStore.current.activeProfile.copy(hapticFeedback = false)
        fakeStore.saveProfiles(listOf(silentProfile), silentProfile.id)
        advanceUntilIdle()

        vm.onTouchGesture(TouchGestureEvent.SingleTap)
        advanceTimeBy(30)
        assertTrue(fakeHaptics.patterns.isEmpty())
    }

    @Test
    fun `edit and save custom gesture profile in InputViewModel dispatches mapped navigation actions`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.startEditingGestureProfile()
        val initialDraft = vm.inputState.value.gestureProfileDraft
        assertEquals("default", initialDraft?.profileId)
        assertFalse(initialDraft?.isDirty ?: true)

        vm.updateGestureProfileName("Navegação Web")
        vm.updateGestureBinding(SwipeGesturePattern.THREE_FINGER_LEFT, NavigationAction.CLOSE_TAB)
        vm.updateGestureBinding(SwipeGesturePattern.THREE_FINGER_UP, NavigationAction.TV_HOME)
        assertTrue(vm.inputState.value.gestureProfileDraft?.isDirty == true)

        vm.saveGestureProfile()
        advanceUntilIdle()

        assertEquals(null, vm.inputState.value.gestureProfileDraft)
        assertEquals("Navegação Web", fakeStore.current.activeProfile.name)
        assertEquals(
            NavigationAction.CLOSE_TAB,
            vm.inputState.value.activeSwipeGestures[SwipeGesturePattern.THREE_FINGER_LEFT]
        )

        // Dispatch saved keyboard-combo navigation gesture
        vm.onTouchGesture(TouchGestureEvent.SwipeGesture(SwipeGesturePattern.THREE_FINGER_LEFT))
        assertEquals(HidConstants.MOD_LEFT_CTRL to HidConstants.KEY_W, fakeSender.keyCombos.last())
        assertEquals(HapticPattern.STEP_TICK, fakeHaptics.patterns.last())

        // Dispatch saved consumer-control navigation gesture
        vm.onTouchGesture(TouchGestureEvent.SwipeGesture(SwipeGesturePattern.THREE_FINGER_UP))
        assertEquals(HidConstants.CONSUMER_AC_HOME, fakeSender.consumerPulses.last())

        // Create and activate a dedicated custom gesture profile directly via InputViewModel
        vm.saveCustomGestureProfile(
            profileName = "Leitura PDF",
            bindings = mapOf(SwipeGesturePattern.TWO_FINGER_SWIPE_LEFT to NavigationAction.PAGE_DOWN),
            profileId = "pdf_reader"
        )
        advanceUntilIdle()

        assertEquals("pdf_reader", fakeStore.current.activeProfileId)
        vm.onTouchGesture(TouchGestureEvent.SwipeGesture(SwipeGesturePattern.TWO_FINGER_SWIPE_LEFT))
        assertEquals(HidConstants.MOD_NONE to HidConstants.KEY_PAGE_DOWN, fakeSender.keyCombos.last())
    }

    private class RecordingHapticEngine : HapticFeedbackEngine {
        val patterns = mutableListOf<HapticPattern>()
        override fun trigger(pattern: HapticPattern) {
            patterns += pattern
        }
    }

    private data class RecordedMouseReport(val buttons: Int, val dx: Int, val dy: Int, val wheel: Int)

    private class FakeHidReportSender : HidReportSender {
        private val _packetStats = MutableStateFlow(HidPacketStats())
        override val packetStats: StateFlow<HidPacketStats> = _packetStats.asStateFlow()

        val mouseReports = mutableListOf<RecordedMouseReport>()
        val zoomScrolls = mutableListOf<Int>()
        val keyCombos = mutableListOf<Pair<Int, Int>>()
        val textStrokes = mutableListOf<List<HidKeyStroke>>()
        val consumerPulses = mutableListOf<Int>()
        var releaseAllCount = 0

        override fun sendMouseReport(buttons: Int, dx: Int, dy: Int, wheel: Int) {
            mouseReports += RecordedMouseReport(buttons, dx, dy, wheel)
        }

        override fun sendZoomScroll(wheelDelta: Int) {
            zoomScrolls += wheelDelta
        }

        override fun sendKeyCombo(modifiers: Int, keyCode: Int) {
            keyCombos += modifiers to keyCode
        }

        override fun sendTextStrokes(strokes: List<HidKeyStroke>) {
            textStrokes += strokes
        }

        override fun sendConsumerPulse(mask: Int) {
            consumerPulses += mask
        }

        override fun releaseAll() {
            releaseAllCount++
        }
    }

    private class FakeSettingsStore : SettingsStore {
        private val _state = MutableStateFlow(
            AppPreferencesState(
                powerSaverMode = false,
                profiles = listOf(ControlProfile(id = "default", name = "Default", keyboardLayout = KeyboardLayout.ABNT2)),
                activeProfileId = "default"
            )
        )
        val current: AppPreferencesState get() = _state.value
        override val stateFlow: Flow<AppPreferencesState> = _state

        override suspend fun setPowerSaverMode(enabled: Boolean) {
            _state.value = _state.value.copy(powerSaverMode = enabled)
        }

        override suspend fun setActiveProfileId(profileId: String) {
            _state.value = _state.value.copy(activeProfileId = profileId)
        }

        override suspend fun saveProfiles(profiles: List<ControlProfile>, activeProfileId: String) {
            _state.value = _state.value.copy(profiles = profiles, activeProfileId = activeProfileId)
        }
    }
}
