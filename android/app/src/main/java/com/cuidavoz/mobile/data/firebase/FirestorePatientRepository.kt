package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.PatientEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class FirestorePatientRepository(
    private val firestore: FirebaseFirestore?,
) {
    suspend fun upsertPatient(
        familyId: String,
        patient: PatientEntity,
        caregiverUserId: String?,
    ) {
        val db = firestore ?: return
        db.document(FirestorePaths.patientDocument(familyId, patient.id))
            .set(
                mapOf(
                    "name" to patient.fullName,
                    "age" to patient.age,
                    "mainCaregiverId" to caregiverUserId,
                    "updatedAt" to patient.updatedAt,
                ),
            ).await()
    }

    suspend fun fetchPatientUpdatedAtFromServer(familyId: String, patientId: String): Long? {
        val db = firestore ?: return null
        val snapshot = db.document(FirestorePaths.patientDocument(familyId, patientId))
            .get(Source.SERVER)
            .await()
        return snapshot.takeIf { it.exists() }?.getLong("updatedAt")
    }

    fun listenToPatient(
        familyId: String,
        patientId: String,
        onChange: (Map<String, Any?>?) -> Unit,
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return db.document(FirestorePaths.patientDocument(familyId, patientId))
            .addSnapshotListener { snapshot, _ -> onChange(snapshot?.data) }
    }
}
