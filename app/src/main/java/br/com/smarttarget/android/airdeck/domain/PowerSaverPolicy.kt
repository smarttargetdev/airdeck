package br.com.smarttarget.android.airdeck.domain

import android.hardware.SensorManager

data class PowerSaverConfig(
    val enabled: Boolean,
    val animationsEnabled: Boolean,
    val hapticsAllowed: Boolean,
    val hidReportMinIntervalMs: Long,
    val sensorSamplingDelay: Int,
    val sensorUiThrottleMs: Long,
    val telemetryUiBatchSize: Int,
    val qosTokenRateBytesPerSec: Int,
    val qosLatencyMicros: Int
)

object PowerSaverPolicy {
    fun resolve(enabled: Boolean): PowerSaverConfig {
        return if (enabled) {
            PowerSaverConfig(
                enabled = true,
                animationsEnabled = false,
                hapticsAllowed = false,
                hidReportMinIntervalMs = 45L,
                sensorSamplingDelay = SensorManager.SENSOR_DELAY_NORMAL,
                sensorUiThrottleMs = 120L,
                telemetryUiBatchSize = 5,
                qosTokenRateBytesPerSec = 400,
                qosLatencyMicros = 22500
            )
        } else {
            PowerSaverConfig(
                enabled = false,
                animationsEnabled = true,
                hapticsAllowed = true,
                hidReportMinIntervalMs = 12L,
                sensorSamplingDelay = SensorManager.SENSOR_DELAY_GAME,
                sensorUiThrottleMs = 16L,
                telemetryUiBatchSize = 1,
                qosTokenRateBytesPerSec = 800,
                qosLatencyMicros = 11250
            )
        }
    }
}
