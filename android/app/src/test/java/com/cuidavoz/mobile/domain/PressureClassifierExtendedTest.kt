package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PressureClassifierExtendedTest {
    private val settings = HealthSettingsEntity(
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

    @Test
    fun criticalTakesPriorityOverCustomRange() {
        assertEquals(
            PressureStatus.CRITICAL,
            PressureClassifier.classify(185, 125, settings),
        )
    }

    @Test
    fun highThresholdUses140Over90() {
        assertEquals(PressureStatus.HIGH, PressureClassifier.classify(141, 70, settings))
        assertEquals(PressureStatus.HIGH, PressureClassifier.classify(120, 91, settings))
    }

    @Test
    fun elevatedBetween120And139() {
        assertEquals(PressureStatus.ELEVATED, PressureClassifier.classify(125, 80, null))
    }

    @Test
    fun normalWithinStandardWithoutSettings() {
        assertEquals(PressureStatus.NORMAL, PressureClassifier.classify(110, 75, null))
    }

    @Test
    fun outOfRangeWhenInsideClinicalThresholdsButOutsidePersonalRange() {
        assertEquals(PressureStatus.OUT_OF_RANGE, PressureClassifier.classify(135, 70, settings))
    }

    @Test
    fun safeMessageForHighAndCriticalIsPresent() {
        assertNotNull(PressureClassifier.safeMessage(PressureStatus.HIGH))
        assertNotNull(PressureClassifier.safeMessage(PressureStatus.CRITICAL))
    }
}
