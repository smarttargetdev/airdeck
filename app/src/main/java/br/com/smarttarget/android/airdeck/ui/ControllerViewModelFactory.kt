package br.com.smarttarget.android.airdeck.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import br.com.smarttarget.android.airdeck.data.AndroidVibratorHapticEngine
import br.com.smarttarget.android.airdeck.data.PreferencesRepository
import br.com.smarttarget.android.airdeck.data.SettingsStore
import br.com.smarttarget.android.airdeck.domain.HapticFeedbackEngine
import br.com.smarttarget.android.airdeck.hid.BluetoothTransportRegistry
import br.com.smarttarget.android.airdeck.hid.HidTransport

class ControllerViewModelFactory(
    private val application: Application,
    private val hidTransport: HidTransport = BluetoothTransportRegistry.getOrCreate(application),
    private val settingsStore: SettingsStore = PreferencesRepository(application),
    private val hapticEngine: HapticFeedbackEngine = AndroidVibratorHapticEngine(application)
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RemoteControlViewModel::class.java) -> {
                RemoteControlViewModel(
                    application = application,
                    hidTransport = hidTransport,
                    settingsStore = settingsStore
                ) as T
            }
            modelClass.isAssignableFrom(InputViewModel::class.java) -> {
                InputViewModel(
                    hidReportSender = hidTransport,
                    settingsStore = settingsStore,
                    hapticEngine = hapticEngine
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
