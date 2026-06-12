package com.cuidavoz.mobile.util

import com.cuidavoz.mobile.data.model.HealthSettingsEntity

object DefaultHealthSettings {
    fun createForPatient(
        patientId: String,
        existingId: String? = null,
        now: Long = System.currentTimeMillis(),
    ): HealthSettingsEntity = HealthSettingsEntity(
        id = existingId ?: createLocalId("health_settings"),
        patientId = patientId,
        systolicMinNormal = 100,
        systolicMaxNormal = 130,
        diastolicMinNormal = 60,
        diastolicMaxNormal = 85,
        pulseMinNormal = 60,
        pulseMaxNormal = 100,
        doctorRecommendation = null,
        updatedAt = now,
    )
}
