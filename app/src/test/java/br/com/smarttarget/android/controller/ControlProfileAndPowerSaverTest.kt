package br.com.smarttarget.android.controller

import android.hardware.SensorManager
import br.com.smarttarget.android.controller.domain.ControlProfile
import br.com.smarttarget.android.controller.domain.ControlProfileCodec
import br.com.smarttarget.android.controller.domain.PointerDeltaAccumulator
import br.com.smarttarget.android.controller.domain.PowerSaverPolicy
import br.com.smarttarget.android.controller.domain.ProfileIconType
import br.com.smarttarget.android.controller.domain.ProfileOperations
import br.com.smarttarget.android.controller.domain.ProfilePresets
import br.com.smarttarget.android.controller.hid.KeyboardLayout
import br.com.smarttarget.android.controller.ui.ControlMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlProfileAndPowerSaverTest {

    @Test
    fun `profile codec roundtrips default profiles and custom buttons`() {
        val original = ProfilePresets.defaultProfiles()
        val encoded = ControlProfileCodec.encodeList(original)
        val decoded = ControlProfileCodec.decodeList(encoded)
        assertEquals(original, decoded)
    }

    @Test
    fun `upsert creates and edits profile with sanitized name and clamped sensitivity`() {
        val initial = ProfilePresets.defaultProfiles()
        val created = ControlProfile(
            id = "custom_1",
            name = "   ",
            iconType = ProfileIconType.GAMING,
            defaultControlMode = ControlMode.AIR_MOUSE,
            keyboardLayout = KeyboardLayout.US_INTL,
            pointerSensitivity = 10f,
            swapMouseButtons = true
        )
        val afterCreate = ProfileOperations.upsertProfile(initial, created)
        assertEquals(initial.size + 1, afterCreate.profiles.size)
        val saved = afterCreate.profiles.last()
        assertEquals("Perfil Personalizado", saved.name)
        assertEquals(3.5f, saved.pointerSensitivity, 0.001f)
        assertEquals("custom_1", afterCreate.activeProfileId)

        val renamed = saved.copy(name = "FPS Competitivo")
        val afterEdit = ProfileOperations.upsertProfile(afterCreate.profiles, renamed)
        assertEquals(afterCreate.profiles.size, afterEdit.profiles.size)
        assertEquals("FPS Competitivo", afterEdit.profiles.last().name)
    }

    @Test
    fun `delete profile never removes last remaining profile and updates active id`() {
        val initial = ProfilePresets.defaultProfiles()
        val activeId = initial[1].id
        val afterDelete = ProfileOperations.deleteProfile(initial, activeId, activeId)
        assertEquals(initial.size - 1, afterDelete.profiles.size)
        assertEquals(afterDelete.profiles.first().id, afterDelete.activeProfileId)

        val single = listOf(initial.first())
        val afterSingleDelete = ProfileOperations.deleteProfile(single, single.first().id, single.first().id)
        assertEquals(1, afterSingleDelete.profiles.size)
    }

    @Test
    fun `pointer accumulator throttles reports in power saver mode without losing deltas`() {
        var fakeTime = 1000L
        val accumulator = PointerDeltaAccumulator(clockMs = { fakeTime })

        // First move dispatches immediately
        val first = accumulator.accumulateMove(4f, 0f, sensitivity = 1.0f, minIntervalMs = 45L)
        assertNotNull(first)

        // Move 15ms later is throttled (accumulated internally)
        fakeTime = 1015L
        val throttled = accumulator.accumulateMove(6f, 0f, sensitivity = 1.0f, minIntervalMs = 45L)
        assertNull(throttled)

        // Move at 1050ms dispatches combined accumulated delta
        fakeTime = 1050L
        val combined = accumulator.accumulateMove(6f, 0f, sensitivity = 1.0f, minIntervalMs = 45L)
        assertNotNull(combined)
        assertTrue(combined!!.dx > 10)
    }

    @Test
    fun `power saver policy disables animations reduces rates and optimizes bluetooth qos`() {
        val normal = PowerSaverPolicy.resolve(enabled = false)
        val eco = PowerSaverPolicy.resolve(enabled = true)

        assertTrue(normal.animationsEnabled)
        assertTrue(normal.hapticsAllowed)
        assertEquals(SensorManager.SENSOR_DELAY_GAME, normal.sensorSamplingDelay)

        assertFalse(eco.animationsEnabled)
        assertFalse(eco.hapticsAllowed)
        assertTrue(eco.hidReportMinIntervalMs > normal.hidReportMinIntervalMs)
        assertEquals(SensorManager.SENSOR_DELAY_NORMAL, eco.sensorSamplingDelay)
        assertTrue(eco.qosTokenRateBytesPerSec < normal.qosTokenRateBytesPerSec)
        assertTrue(eco.qosLatencyMicros > normal.qosLatencyMicros)
        assertTrue(eco.telemetryUiBatchSize > normal.telemetryUiBatchSize)
    }
}
