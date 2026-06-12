package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.PatientDao
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID

class PatientRepository(
    private val patientDao: PatientDao,
    private val firebaseSyncManager: FirebaseSyncManager? = null,
) {
    fun observeCurrentPatient() = patientDao.observePatient(DEFAULT_PATIENT_ID)

    suspend fun getCurrentPatient() =
        patientDao.getPatient(DEFAULT_PATIENT_ID) ?: patientDao.getMostRecentPatient()

    suspend fun upsertCurrentPatient(patient: PatientEntity) {
        patientDao.upsert(patient)
        firebaseSyncManager?.enqueuePatient(patient)
    }
}
