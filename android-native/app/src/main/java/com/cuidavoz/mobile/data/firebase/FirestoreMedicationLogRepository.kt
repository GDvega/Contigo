package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreMedicationLogRepository(
    private val firestore: FirebaseFirestore?,
) {
    suspend fun createMedicationLog(
        familyId: String,
        patientId: String,
        log: MedicationLogEntity,
        createdBy: String?,
    ) {
        val db = firestore ?: return
        db.collection(FirestorePaths.medicationLogsCollection(familyId, patientId))
            .document(log.id)
            .set(
                mapOf(
                    "medicationId" to log.medicationId,
                    "scheduledAt" to log.scheduledFor,
                    "takenAt" to log.takenAt,
                    "status" to log.status,
                    "createdBy" to createdBy,
                    "syncedAt" to System.currentTimeMillis(),
                ),
            ).await()
    }
}
