package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
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
                    "skipReason" to log.skipReason,
                    "createdBy" to createdBy,
                    "syncedAt" to System.currentTimeMillis(),
                ),
            ).await()
    }

    suspend fun uploadMedicationLogsBatch(
        familyId: String,
        patientId: String,
        logs: List<MedicationLogEntity>,
        createdBy: String?,
    ) {
        if (logs.isEmpty()) return
        val db = firestore ?: return
        val collection = db.collection(FirestorePaths.medicationLogsCollection(familyId, patientId))
        val batch = db.batch()
        val syncedAt = System.currentTimeMillis()
        logs.forEach { log ->
            batch.set(
                collection.document(log.id),
                mapOf(
                    "medicationId" to log.medicationId,
                    "scheduledAt" to log.scheduledFor,
                    "takenAt" to log.takenAt,
                    "status" to log.status,
                    "skipReason" to log.skipReason,
                    "createdBy" to createdBy,
                    "syncedAt" to syncedAt,
                ),
            )
        }
        batch.commit().await()
    }

    suspend fun fetchMedicationLogsPage(
        familyId: String,
        patientId: String,
        pageSize: Long,
        after: DocumentSnapshot? = null,
    ): FirestorePage {
        val db = firestore ?: return FirestorePage(emptyList(), null)
        var query = db.collection(FirestorePaths.medicationLogsCollection(familyId, patientId))
            .orderBy("scheduledAt", Query.Direction.DESCENDING)
            .limit(pageSize)
        if (after != null) {
            query = query.startAfter(after)
        }
        val documents = query.get(Source.SERVER).await().documents
        return FirestorePage(
            documents = documents,
            nextCursor = documents.lastOrNull(),
        )
    }
}
