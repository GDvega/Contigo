package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.PatientDao
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID

class PatientRepository(
    private val patientDao: PatientDao,
) {
    fun observeCurrentPatient() = patientDao.observePatient(DEFAULT_PATIENT_ID)

    suspend fun getCurrentPatient() =
        patientDao.getPatient(DEFAULT_PATIENT_ID) ?: patientDao.getMostRecentPatient()
}
