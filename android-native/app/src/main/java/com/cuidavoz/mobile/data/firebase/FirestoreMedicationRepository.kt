package com.cuidavoz.mobile.data.firebase

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    ) {
        val db = firestore ?: return
        db.collection(FirestorePaths.medicationsCollection(familyId, patientId))
            .document(medication.id)
            .set(
                mapOf(
                    "name" to medication.name,
                    "dose" to medication.dose,
                    "time24" to medication.scheduleTime,
                    "instructions" to medication.instructions,
                    "color" to medication.color,
                    "shape" to medication.shape,
                    "imageUrl" to null,
                    "active" to medication.isActive,
                    "scheduleType" to medication.scheduleType,
                    "startDate" to medication.startDate.toFirestoreDate(),
                    "endDate" to medication.endDate?.toFirestoreDate(),
                    "daysOfWeek" to medication.daysOfWeekJson.let { json ->
                        Regex("\\d+").findAll(json).mapNotNull { match -> match.value.toIntOrNull() }.toList()
                    },
                    "specificDates" to Regex("\\d{4}-\\d{2}-\\d{2}").findAll(medication.specificDatesJson)
                        .map { match -> match.value.toFirestoreDate() }
                        .toList(),
                    "createdAt" to medication.createdAt,
                    "updatedAt" to medication.updatedAt,
                    "updatedBy" to updatedBy,
                ),
            ).await()
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

    private fun String.toFirestoreDate(): Date {
        val localDate = runCatching { LocalDate.parse(this) }
            .getOrDefault(LocalDate.parse(MedicationScheduleDefaults.todayIso()))
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
}
