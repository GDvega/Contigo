package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.reminders.ReminderPreferences
import com.cuidavoz.mobile.reminders.VoicePreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
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

    suspend fun fetchHealthSettingsUpdatedAtFromServer(familyId: String, patientId: String): Long? {
        val db = firestore ?: return null
        val snapshot = db.document(FirestorePaths.healthSettingsDocument(familyId, patientId))
            .get(Source.SERVER)
            .await()
        return snapshot.takeIf { it.exists() }?.getLong("updatedAt")
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

    suspend fun fetchFamilyContactUpdatedAtFromServer(familyId: String, patientId: String): Long? {
        val db = firestore ?: return null
        val snapshot = db.collection(FirestorePaths.patientDocument(familyId, patientId) + "/contact")
            .document("main")
            .get(Source.SERVER)
            .await()
        return snapshot.takeIf { it.exists() }?.getLong("updatedAt")
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

    suspend fun upsertReminderPreferences(
        familyId: String,
        patientId: String,
        reminderPrefs: ReminderPreferences,
        voicePrefs: VoicePreferences,
        updatedAt: Long,
    ) {
        val db = firestore ?: return
        db.document(FirestorePaths.reminderPreferencesDocument(familyId, patientId))
            .set(
                mapOf(
                    "remindersEnabled" to reminderPrefs.remindersEnabled,
                    "repeatIntervalMinutes" to reminderPrefs.repeatIntervalMinutes,
                    "maxRepeatCount" to reminderPrefs.maxRepeatCount,
                    "soundEnabled" to reminderPrefs.soundEnabled,
                    "vibrationEnabled" to reminderPrefs.vibrationEnabled,
                    "notifyCaregiverOnMissed" to reminderPrefs.notifyCaregiverOnMissed,
                    "voiceAssistantEnabled" to voicePrefs.voiceAssistantEnabled,
                    "voiceReminderEnabled" to voicePrefs.voiceReminderEnabled,
                    "voiceRepeatCount" to voicePrefs.voiceRepeatCount,
                    "easyModeEnabled" to voicePrefs.easyModeEnabled,
                    "voiceGuidanceEnabled" to voicePrefs.voiceGuidanceEnabled,
                    "updatedAt" to updatedAt,
                ),
            ).await()
    }

    fun listenToReminderPreferences(
        familyId: String,
        patientId: String,
        onChange: (Map<String, Any?>?) -> Unit,
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return db.document(FirestorePaths.reminderPreferencesDocument(familyId, patientId))
            .addSnapshotListener { snapshot, _ -> onChange(snapshot?.data) }
    }
}
