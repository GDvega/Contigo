package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class MedicationGroup(
    val scheduleTime: String,
    val medications: List<MedicationEntity>,
    val pendingMedications: List<MedicationEntity>,
) {
    val totalCount: Int
        get() = medications.size

    val pendingCount: Int
        get() = pendingMedications.size
}

object MedicationGrouping {
    fun groupMedicationsByScheduleTime(
        medications: List<MedicationEntity>,
        date: LocalDate = LocalDate.now(),
    ): List<MedicationGroup> {
        return medications
            .filter { MedicationScheduleCalculator.isMedicationDueOnDate(it, date) }
            .groupBy { it.scheduleTime }
            .toSortedMap()
            .map { (scheduleTime, items) ->
                MedicationGroup(
                    scheduleTime = scheduleTime,
                    medications = items.sortedBy { it.name },
                    pendingMedications = items.sortedBy { it.name },
                )
            }
    }

    fun getNextMedicationGroup(
        medications: List<MedicationEntity>,
        medicationLogs: List<MedicationLogEntity>,
        today: LocalDate = LocalDate.now(),
        now: LocalTime = LocalTime.now(),
    ): MedicationGroup? {
        val groups = medications
            .filter { MedicationScheduleCalculator.isMedicationDueOnDate(it, today) }
            .groupBy { it.scheduleTime }
            .toSortedMap()
            .mapNotNull { (scheduleTime, items) ->
                val pending = getPendingMedicationsForTime(
                    scheduleTime = scheduleTime,
                    medications = items,
                    medicationLogs = medicationLogs,
                )
                if (pending.isEmpty()) {
                    null
                } else {
                    MedicationGroup(
                        scheduleTime = scheduleTime,
                        medications = items.sortedBy { it.name },
                        pendingMedications = pending.sortedBy { it.name },
                    )
                }
            }

        if (groups.isEmpty()) {
            return null
        }

        return groups.firstOrNull { group ->
            val groupTime = parseScheduleTime(group.scheduleTime) ?: return@firstOrNull false
            !groupTime.isBefore(now)
        } ?: groups.firstOrNull()
    }

    fun getPendingMedicationsForTime(
        scheduleTime: String,
        medications: List<MedicationEntity>,
        medicationLogs: List<MedicationLogEntity>,
        date: LocalDate = LocalDate.now(),
    ): List<MedicationEntity> {
        val takenMedicationIds = medicationLogs
            .filter { it.status == "TAKEN" }
            .map { it.medicationId }
            .toSet()

        return medications
            .filter {
                MedicationScheduleCalculator.isMedicationDueOnDate(it, date) &&
                    it.scheduleTime == scheduleTime &&
                    it.id !in takenMedicationIds
            }
            .sortedBy { it.name }
    }

    fun medicationsDueOnDate(
        medications: List<MedicationEntity>,
        date: LocalDate,
    ): List<MedicationEntity> {
        return medications
            .filter { MedicationScheduleCalculator.isMedicationDueOnDate(it, date) }
            .sortedWith(compareBy<MedicationEntity> { it.scheduleTime }.thenBy { it.name })
    }

    fun nextOccurrencesByScheduleTime(
        medications: List<MedicationEntity>,
        fromDateTime: LocalDateTime,
    ): Map<String, List<Pair<MedicationEntity, LocalDateTime>>> {
        return medications
            .mapNotNull { medication ->
                MedicationScheduleCalculator.getNextMedicationOccurrence(medication, fromDateTime)
                    ?.let { medication to it }
            }
            .groupBy(
                keySelector = { it.second.toLocalDate().toString() + "|" + it.first.scheduleTime },
                valueTransform = { it },
            )
    }

    private fun parseScheduleTime(scheduleTime: String): LocalTime? {
        val parts = scheduleTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return LocalTime.of(hour, minute)
    }
}
