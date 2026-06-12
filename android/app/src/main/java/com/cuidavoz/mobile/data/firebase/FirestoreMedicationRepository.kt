package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.domain.sync.MedicationImageSyncOperation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class FirestoreMedicationRepository(
    private val firestore: FirebaseFirestore?,
) {
    suspend fun upsertMedication(
        familyId: String,
        patientId: String,
        medication: MedicationEntity,
        updatedBy: String?,
        imageSyncOperation: MedicationImageSyncOperation = MedicationImageSyncOperation.KEEP,
        imagePath: String? = null,
    ) {
        val db = firestore ?: return
        val data = mutableMapOf<String, Any?>(
            "name" to medication.name,
            "dose" to medication.dose,
            "time24" to medication.scheduleTime,
            "instructions" to medication.instructions,
            "color" to medication.color,
            "shape" to medication.shape,
            "active" to medication.isActive,
            "scheduleType" to medication.scheduleType,
            "startDate" to medication.startDate.toFirestoreDate(),
            "endDate" to medication.endDate?.toFirestoreDate(),
            "daysOfWeek" to medication.daysOfWeek,
            "specificDates" to medication.specificDates.map { it.toFirestoreDate() },
            "createdAt" to medication.createdAt,
            "updatedAt" to medication.updatedAt,
            "updatedBy" to updatedBy,
        )
        when (imageSyncOperation) {
            MedicationImageSyncOperation.KEEP -> Unit
            MedicationImageSyncOperation.UPLOAD -> {
                data["imagePath"] = checkNotNull(imagePath) { "Falta imagePath para subir la imagen" }
            }
            MedicationImageSyncOperation.DELETE -> data["imagePath"] = null
        }
        db.collection(FirestorePaths.medicationsCollection(familyId, patientId))
            .document(medication.id)
            .set(
                data,
                SetOptions.merge(),
            ).await()
    }

    suspend fun fetchMedicationUpdatedAtFromServer(
        familyId: String,
        patientId: String,
        medicationId: String,
    ): Long? {
        val db = firestore ?: return null
        val snapshot = db.collection(FirestorePaths.medicationsCollection(familyId, patientId))
            .document(medicationId)
            .get(Source.SERVER)
            .await()
        return snapshot.takeIf { it.exists() }?.getLong("updatedAt")
    }

    fun listenToMedications(
        familyId: String,
        patientId: String,
        onChange: (List<Map<String, Any?>>) -> Unit,
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return db.collection(FirestorePaths.medicationsCollection(familyId, patientId))
            .addSnapshotListener { snapshot, _ ->
                val data = snapshot?.documents?.map { document ->
                    document.data.orEmpty() + mapOf("id" to document.id)
                }.orEmpty()
                onChange(data)
            }
    }

    private fun LocalDate.toFirestoreDate(): Date {
        return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }

    private fun String.toFirestoreDate(): Date {
        val localDate = runCatching { LocalDate.parse(this) }
            .getOrDefault(LocalDate.parse(MedicationScheduleDefaults.todayIso()))
        return localDate.toFirestoreDate()
    }
}
