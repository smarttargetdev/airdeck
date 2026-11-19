package br.com.smarttarget.android.airdeck.domain

import br.com.smarttarget.android.airdeck.hid.BondedHostDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ReconnectionState {
    data object Idle : ReconnectionState
    data class Reconnecting(
        val device: BondedHostDevice,
        val attempt: Int,
        val maxAttempts: Int = ReconnectionPolicy.MAX_ATTEMPTS
    ) : ReconnectionState
}

object ReconnectionPolicy {
    const val MAX_ATTEMPTS = 5
    val backoffDelaysMs: List<Long> = listOf(2_000L, 4_000L, 8_000L, 16_000L, 30_000L)

    fun delayForAttempt(attempt: Int, schedule: List<Long> = backoffDelaysMs): Long {
        if (schedule.isEmpty()) return 2_000L
        val index = (attempt - 1).coerceIn(0, schedule.lastIndex)
        return schedule[index]
    }
}

object BluetoothReconnectionBridge {
    private val _state = MutableStateFlow<ReconnectionState>(ReconnectionState.Idle)
    val state: StateFlow<ReconnectionState> = _state.asStateFlow()

    @Volatile
    private var cancelHandler: (() -> Unit)? = null

    fun registerCancelHandler(handler: (() -> Unit)?) {
        cancelHandler = handler
    }

    fun updateState(newState: ReconnectionState) {
        _state.value = newState
    }

    fun requestCancel() {
        val handler = cancelHandler
        if (handler != null) {
            handler.invoke()
        } else {
            _state.value = ReconnectionState.Idle
        }
    }

    internal fun resetForTesting() {
        cancelHandler = null
        _state.value = ReconnectionState.Idle
    }
}
