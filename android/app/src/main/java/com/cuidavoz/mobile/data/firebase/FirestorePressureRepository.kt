package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
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

    suspend fun uploadPressureReadingsBatch(
        familyId: String,
        patientId: String,
        readings: List<BloodPressureEntity>,
        createdBy: String?,
    ) {
        if (readings.isEmpty()) return
        val db = firestore ?: return
        val collection = db.collection(FirestorePaths.pressureCollection(familyId, patientId))
        val batch = db.batch()
        val syncedAt = System.currentTimeMillis()
        readings.forEach { reading ->
            batch.set(
                collection.document(reading.id),
                mapOf(
                    "systolic" to reading.systolic,
                    "diastolic" to reading.diastolic,
                    "pulse" to reading.pulse,
                    "note" to reading.notes,
                    "measuredAt" to reading.measuredAt,
                    "createdBy" to createdBy,
                    "syncedAt" to syncedAt,
                    "status" to reading.status,
                ),
            )
        }
        batch.commit().await()
    }

    suspend fun deletePressureReading(
        familyId: String,
        patientId: String,
        readingId: String,
    ) {
        val db = firestore ?: return
        db.collection(FirestorePaths.pressureCollection(familyId, patientId))
            .document(readingId)
            .delete()
            .await()
    }

    suspend fun fetchPressureReadingsPage(
        familyId: String,
        patientId: String,
        pageSize: Long,
        after: DocumentSnapshot? = null,
    ): FirestorePage {
        val db = firestore ?: return FirestorePage(emptyList(), null)
        var query = db.collection(FirestorePaths.pressureCollection(familyId, patientId))
            .orderBy("measuredAt", Query.Direction.DESCENDING)
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
