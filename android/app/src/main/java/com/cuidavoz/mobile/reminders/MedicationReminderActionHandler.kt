package com.cuidavoz.mobile.reminders

import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.MedicationReminderRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.data.sync.SyncManager
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.domain.MedicationDoseStatus
import com.cuidavoz.mobile.domain.MedicationOutcomeResult
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.formatScheduleTime

class MedicationReminderActionHandler(
    private val medicationReminderRepository: MedicationReminderRepository,
    private val medicationRepository: MedicationRepository,
    private val dailyStatusRepository: DailyStatusRepository,
    private val familyContactRepository: FamilyContactRepository,
    private val settingsRepository: SettingsRepository,
    private val firebaseSyncManager: SyncManager?,
) {
    suspend fun markTaken(payload: ReminderPayload): Boolean {
        val result = recordOutcomes(
            payload = payload,
            outcomes = payload.medicationIds.map {
                MedicationDoseOutcome(
                    medicationId = it,
                    status = MedicationDoseStatus.TAKEN,
                )
            },
        )
        return result.anyRecorded
    }

    suspend fun recordOutcomes(
        payload: ReminderPayload,
        outcomes: List<MedicationDoseOutcome>,
    ): MedicationOutcomeResult {
        val result = dailyStatusRepository.recordReminderOutcomes(
            patientId = payload.patientId,
            scheduleTime = payload.scheduleTime,
            scheduledFor = payload.scheduledAt,
            outcomes = outcomes,
        )
        if (result.groupResolved) {
            val now = System.currentTimeMillis()
            medicationReminderRepository.updateGroupStatus(
                reminderGroupId = payload.reminderGroupId,
                status = "TAKEN",
                respondedAt = now,
                updatedAt = now,
            )
        }
        return result
    }

    suspend fun markSnoozed(reminderId: String?) {
        if (reminderId.isNullOrBlank()) return
        medicationReminderRepository.updateReminderStatus(
            id = reminderId,
            status = "SNOOZED",
            respondedAt = System.currentTimeMillis(),
        )
    }

    suspend fun markFired(reminderId: String?) {
        if (reminderId.isNullOrBlank()) return
        medicationReminderRepository.updateReminderStatus(
            id = reminderId,
            status = "PENDING",
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun finalizeMissed(payload: ReminderPayload): Boolean {
        val current = medicationReminderRepository.getReminderById(payload.reminderId.orEmpty()) ?: return false
        if (!ReminderAttemptPolicy.shouldMarkMissed(current.attemptNumber, current.maxAttempts, current.status)) {
            return false
        }

        val pendingIds = dailyStatusRepository.getPendingMedicationIdsForScheduleTime(
            patientId = payload.patientId,
            scheduleTime = payload.scheduleTime,
        )
        if (pendingIds.isEmpty()) {
            return false
        }

        medicationReminderRepository.updateGroupStatus(
            reminderGroupId = payload.reminderGroupId,
            status = "MISSED",
            updatedAt = System.currentTimeMillis(),
        )

        val prefs = settingsRepository.getReminderPreferences()
        if (prefs.notifyCaregiverOnMissed) {
            val meds = medicationRepository.getMedicationsByIds(pendingIds)
            val names = meds.map { it.name }.ifEmpty {
                payload.medicationNames.filterIndexed { index, _ ->
                    payload.medicationIds.getOrNull(index) in pendingIds
                }
            }
            firebaseSyncManager?.enqueueAlert(
                type = "missed_medication",
                message = "El paciente no confirmó la toma de ${names.joinToString(", ")} de las ${formatScheduleTime(payload.scheduleTime)}.",
                medicationIds = pendingIds,
                scheduledAt = payload.scheduledAt,
                severity = "medium",
            )
        }
        return true
    }

    suspend fun requestHelp(payload: ReminderPayload): String? {
        val contact = familyContactRepository.getPrimaryContact(DEFAULT_PATIENT_ID)
        firebaseSyncManager?.enqueueAlert(
            type = "help_request",
            message = "El paciente pidió ayuda desde el recordatorio de las ${formatScheduleTime(payload.scheduleTime)}.",
            medicationIds = payload.medicationIds,
            scheduledAt = payload.scheduledAt,
            severity = "high",
        )
        return contact?.phone
    }
}
