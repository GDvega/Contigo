package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.BloodPressureDao
import com.cuidavoz.mobile.data.local.HealthSettingsDao
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.sync.SyncManager
import com.cuidavoz.mobile.domain.PressureClassifier
import com.cuidavoz.mobile.util.createLocalId

class PressureRepository(
    private val bloodPressureDao: BloodPressureDao,
    private val healthSettingsDao: HealthSettingsDao,
    private val firebaseSyncManager: SyncManager? = null,
) {
    fun observeRecentReadings(patientId: String) = bloodPressureDao.observeRecentReadings(patientId)

    suspend fun recordPressure(
        patientId: String,
        systolic: Int,
        diastolic: Int,
        pulse: Int?,
        notes: String?,
        measuredAt: Long? = null,
    ): BloodPressureEntity {
        val now = System.currentTimeMillis()
        val rawTimestamp = measuredAt ?: now
        val timestamp = rawTimestamp.coerceAtMost(now) // Prevent future dates
        val settings = healthSettingsDao.getSettings(patientId)
        val reading = BloodPressureEntity(
            id = createLocalId("pressure"),
            patientId = patientId,
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            status = PressureClassifier.classify(
                systolic = systolic,
                diastolic = diastolic,
                settings = settings,
            ).name,
            notes = notes,
            measuredAt = timestamp,
            createdAt = now,
        )
        bloodPressureDao.insert(reading)
        firebaseSyncManager?.enqueuePressureReading(reading)
        return reading
    }

    suspend fun deletePressureReading(reading: BloodPressureEntity) {
        firebaseSyncManager?.enqueueDeletePressureReading(reading)
        bloodPressureDao.delete(reading)
    }
}
