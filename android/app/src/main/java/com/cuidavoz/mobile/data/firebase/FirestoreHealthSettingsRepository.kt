package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class FirestoreHealthSettingsRepository(
    private val firestore: FirebaseFirestore?,
) {
    suspend fun upsertHealthSettings(
        familyId: String,
        patientId: String,
        settings: HealthSettingsEntity,
    ) {
        val db = firestore ?: return
        db.document(FirestorePaths.healthSettingsDocument(familyId, patientId))
            .set(
                mapOf(
                    "systolicMin" to settings.systolicMinNormal,
                    "systolicMax" to settings.systolicMaxNormal,
                    "diastolicMin" to settings.diastolicMinNormal,
                    "diastolicMax" to settings.diastolicMaxNormal,
                    "pulseMin" to settings.pulseMinNormal,
                    "pulseMax" to settings.pulseMaxNormal,
                    "doctorRecommendation" to settings.doctorRecommendation,
                    "updatedAt" to settings.updatedAt,
                ),
            ).await()
    }

    suspend fun upsertFamilyContact(
        familyId: String,
        patientId: String,
        contact: FamilyContactEntity,
    ) {
        val db = firestore ?: return
        db.collection(FirestorePaths.patientDocument(familyId, patientId) + "/contact")
            .document("main")
            .set(
                mapOf(
                    "fullName" to contact.fullName,
                    "phone" to contact.phone,
                    "relationship" to contact.relationship,
                    "updatedAt" to contact.updatedAt,
                ),
            ).await()
    }

    fun listenToHealthSettings(
        familyId: String,
        patientId: String,
        onChange: (Map<String, Any?>?) -> Unit,
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return db.document(FirestorePaths.healthSettingsDocument(familyId, patientId))
            .addSnapshotListener { snapshot, _ -> onChange(snapshot?.data) }
    }
}
