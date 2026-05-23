package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PressureClassifierTest {
    @Test
    fun `classify returns out of range before generic thresholds`() {
        val settings = HealthSettingsEntity(
            id = "settings",
            patientId = "patient",
            systolicMinNormal = 100,
            systolicMaxNormal = 130,
            diastolicMinNormal = 60,
            diastolicMaxNormal = 85,
            pulseMinNormal = 60,
            pulseMaxNormal = 100,
            doctorRecommendation = null,
            updatedAt = 1L,
        )

        assertEquals(PressureStatus.OUT_OF_RANGE, PressureClassifier.classify(95, 70, settings))
    }

    @Test
    fun `safe message stays empty for normal status`() {
        assertNull(PressureClassifier.safeMessage(PressureStatus.NORMAL))
    }
}
