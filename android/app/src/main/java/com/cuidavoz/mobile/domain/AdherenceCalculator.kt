package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import java.time.LocalDate
import java.time.ZoneId

data class AdherenceSummary(
    val totalScheduled: Int,
    val totalTaken: Int,
    val totalPending: Int,
    val adherencePercentage: Int,
    val hasActiveMedications: Boolean,
)

object AdherenceCalculator {
    fun calculateTodayAdherence(
        logs: List<MedicationLogEntity>,
        activeMedications: List<MedicationEntity>,
        today: LocalDate = LocalDate.now(),
    ): AdherenceSummary {
        val dueToday = medicationsDueOnDate(activeMedications, today)
        return summarizeScheduledSlots(
            logs = logs,
            scheduledKeys = dueToday.map { medication ->
                medication.id to scheduleTimeToMillis(medication.scheduleTime, today, ZoneId.systemDefault())
            },
            hasActiveMedications = activeMedications.isNotEmpty(),
        )
    }

    fun calculateAdherenceForRange(
        logs: List<MedicationLogEntity>,
        activeMedications: List<MedicationEntity>,
        days: Int,
        endDate: LocalDate = LocalDate.now(),
    ): AdherenceSummary {
        val safeDays = days.coerceAtLeast(0)
        if (activeMedications.isEmpty() || safeDays == 0) {
            return AdherenceSummary(
                totalScheduled = 0,
                totalTaken = 0,
                totalPending = 0,
                adherencePercentage = 100,
                hasActiveMedications = activeMedications.isNotEmpty(),
            )
        }

        val zoneId = ZoneId.systemDefault()
        val startDate = endDate.minusDays(safeDays - 1L)
        val scheduledKeys = mutableListOf<Pair<String, Long>>()

        var current = startDate
        while (!current.isAfter(endDate)) {
            medicationsDueOnDate(activeMedications, current).forEach { medication ->
                scheduledKeys += medication.id to scheduleTimeToMillis(
                    scheduleTime = medication.scheduleTime,
                    day = current,
                    zoneId = zoneId,
                )
            }
            current = current.plusDays(1)
        }

        return summarizeScheduledSlots(
            logs = logs,
            scheduledKeys = scheduledKeys,
            hasActiveMedications = true,
        )
    }

    fun getPendingMedicationCount(summary: AdherenceSummary): Int = summary.totalPending

    fun getTakenMedicationCount(summary: AdherenceSummary): Int = summary.totalTaken

    private fun medicationsDueOnDate(
        medications: List<MedicationEntity>,
        date: LocalDate,
    ): List<MedicationEntity> {
        return medications
            .filter { !it.isExpired(date) }
            .filter { MedicationScheduleCalculator.isMedicationDueOnDate(it, date) }
    }

    private fun summarizeScheduledSlots(
        logs: List<MedicationLogEntity>,
        scheduledKeys: List<Pair<String, Long>>,
        hasActiveMedications: Boolean,
    ): AdherenceSummary {
        if (scheduledKeys.isEmpty()) {
            return AdherenceSummary(
                totalScheduled = 0,
                totalTaken = 0,
                totalPending = 0,
                adherencePercentage = 100,
                hasActiveMedications = hasActiveMedications,
            )
        }

        val logsByKey = logs.associateBy { "${it.medicationId}_${it.scheduledFor}" }
        var totalTaken = 0
        var totalPending = 0

        scheduledKeys.forEach { (medicationId, scheduledFor) ->
            when (logsByKey["${medicationId}_$scheduledFor"]?.status) {
                "TAKEN" -> totalTaken++
                null -> totalPending++
                else -> Unit
            }
        }

        val totalScheduled = scheduledKeys.size
        val adherencePercentage = ((totalTaken.toDouble() / totalScheduled.toDouble()) * 100.0).toInt()

        return AdherenceSummary(
            totalScheduled = totalScheduled,
            totalTaken = totalTaken,
            totalPending = totalPending,
            adherencePercentage = adherencePercentage,
            hasActiveMedications = hasActiveMedications,
        )
    }
}
