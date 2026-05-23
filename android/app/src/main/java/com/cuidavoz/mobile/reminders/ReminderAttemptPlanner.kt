package com.cuidavoz.mobile.reminders

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationScheduleCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class ReminderGroupPlan(
    val reminderGroupId: String,
    val patientId: String,
    val scheduleTime: String,
    val targetDate: LocalDate,
    val scheduledAt: Long,
    val medicationIds: List<String>,
    val medicationNames: List<String>,
)

object ReminderAttemptPlanner {
    fun groupUpcomingMedications(
        patientId: String,
        medications: List<MedicationEntity>,
        fromDateTime: LocalDateTime,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<ReminderGroupPlan> {
        val medicationOccurrences = medications.mapNotNull { medication ->
            MedicationScheduleCalculator.getNextMedicationOccurrence(medication, fromDateTime)
                ?.let { medication to it }
        }

        return medicationOccurrences
            .groupBy { (_, occurrence) -> occurrence.toLocalDate().toString() + "|" + occurrence.toLocalTime().toString() }
            .values
            .map { entries ->
                val targetDate = entries.first().second.toLocalDate()
                val scheduleTime = entries.first().first.scheduleTime
                ReminderGroupPlan(
                    reminderGroupId = buildGroupId(patientId, scheduleTime, targetDate),
                    patientId = patientId,
                    scheduleTime = scheduleTime,
                    targetDate = targetDate,
                    scheduledAt = entries.first().second.atZone(zoneId).toInstant().toEpochMilli(),
                    medicationIds = entries.map { it.first.id }.sorted(),
                    medicationNames = entries.map { it.first.name }.sorted(),
                )
            }
            .sortedBy { it.scheduledAt }
    }

    fun nextAttemptTime(
        scheduledAt: Long,
        repeatEveryMinutes: Int,
    ): Long = scheduledAt + repeatEveryMinutes * 60_000L

    fun buildGroupId(
        patientId: String,
        scheduleTime: String,
        targetDate: LocalDate,
    ): String = "${patientId}_${targetDate}_$scheduleTime"
}
