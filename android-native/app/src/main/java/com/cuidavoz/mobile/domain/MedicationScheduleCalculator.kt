package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object MedicationScheduleCalculator {
    fun isMedicationDueOnDate(
        medication: MedicationEntity,
        date: LocalDate,
    ): Boolean {
        if (!medication.isActive) return false
        val schedule = medication.toMedicationSchedule()
        if (date.isBefore(schedule.startDate)) return false
        if (schedule.endDate?.let { date.isAfter(it) } == true) return false

        return when (schedule.scheduleType) {
            ScheduleType.ALWAYS -> true
            ScheduleType.DATE_RANGE -> schedule.endDate?.let { !date.isAfter(it) } ?: true
            ScheduleType.WEEKLY_DAYS -> date.dayOfWeek.value in schedule.daysOfWeek
            ScheduleType.SPECIFIC_DATES -> date in schedule.specificDates
        }
    }

    fun getNextMedicationOccurrence(
        medication: MedicationEntity,
        fromDateTime: LocalDateTime,
    ): LocalDateTime? {
        if (!medication.isActive) return null
        val schedule = medication.toMedicationSchedule()
        val time = parseScheduleTime(medication.scheduleTime) ?: return null
        val searchStart = fromDateTime.toLocalDate()
        val horizonEnd = schedule.endDate ?: searchStart.plusYears(5)

        var current = maxOf(searchStart, schedule.startDate)
        while (!current.isAfter(horizonEnd)) {
            if (isMedicationDueOnDate(medication, current)) {
                val candidate = LocalDateTime.of(current, time)
                if (!candidate.isBefore(fromDateTime)) {
                    return candidate
                }
            }
            current = current.plusDays(1)
        }
        return null
    }

    fun getTodayPendingMedications(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        today: LocalDate,
    ): List<MedicationEntity> {
        val takenIds = logs
            .filter { it.status == "TAKEN" }
            .map { it.medicationId }
            .toSet()

        return medications
            .filter { isMedicationDueOnDate(it, today) && it.id !in takenIds }
            .sortedWith(compareBy<MedicationEntity> { it.scheduleTime }.thenBy { it.name })
    }

    private fun parseScheduleTime(value: String): LocalTime? {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return runCatching { LocalTime.of(hour, minute) }.getOrNull()
    }
}
