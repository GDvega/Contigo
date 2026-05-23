package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestorePressureRepository(
    private val firestore: FirebaseFirestore?,
) {
    suspend fun createPressureReading(
        familyId: String,
        patientId: String,
        reading: BloodPressureEntity,
        createdBy: String?,
    ) {
        val db = firestore ?: return
        db.collection(FirestorePaths.pressureCollection(familyId, patientId))
            .document(reading.id)
            .set(
                mapOf(
                    "systolic" to reading.systolic,
                    "diastolic" to reading.diastolic,
                    "pulse" to reading.pulse,
                    "note" to reading.notes,
                    "measuredAt" to reading.measuredAt,
                    "createdBy" to createdBy,
                    "syncedAt" to System.currentTimeMillis(),
                    "status" to reading.status,
                ),
            ).await()
    }
}
