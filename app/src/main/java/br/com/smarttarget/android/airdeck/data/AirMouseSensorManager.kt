package br.com.smarttarget.android.airdeck.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import br.com.smarttarget.android.airdeck.domain.PowerSaverPolicy
import kotlin.math.abs

class AirMouseSensorManager(
    context: Context,
    private val onDelta: (dx: Float, dy: Float) -> Unit,
    private val onTiltSample: (yawRate: Float, pitchRate: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscope: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var isListening = false
    private var powerSaverEnabled = false
    private var lastTiltDispatchMs = 0L
    private var biasX = 0f
    private var biasZ = 0f

    val isAvailable: Boolean
        get() = gyroscope != null

    fun setPowerSaverMode(enabled: Boolean) {
        if (powerSaverEnabled == enabled) return
        powerSaverEnabled = enabled
        if (isListening) {
            stop()
            start()
        }
    }

    fun start() {
        val sensor = gyroscope ?: return
        if (isListening) return
        val policy = PowerSaverPolicy.resolve(powerSaverEnabled)
        sensorManager?.registerListener(this, sensor, policy.sensorSamplingDelay)
        isListening = true
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
    }

    fun recenter() {
        biasX = 0f
        biasZ = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return
        val rawPitch = event.values[0] - biasX
        val rawYaw = event.values[2] - biasZ

        val now = System.currentTimeMillis()
        val policy = PowerSaverPolicy.resolve(powerSaverEnabled)
        if ((now - lastTiltDispatchMs) >= policy.sensorUiThrottleMs) {
            lastTiltDispatchMs = now
            onTiltSample(rawYaw, rawPitch)
        }

        val deadzone = 0.035f
        val dx = if (abs(rawYaw) > deadzone) -rawYaw * 16f else 0f
        val dy = if (abs(rawPitch) > deadzone) -rawPitch * 16f else 0f

        if (dx != 0f || dy != 0f) {
            onDelta(dx, dy)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
