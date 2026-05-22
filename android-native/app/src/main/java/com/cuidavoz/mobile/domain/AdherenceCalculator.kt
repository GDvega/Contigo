package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import java.time.Instant
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
    ): AdherenceSummary {
        return calculateForScheduledWindow(
            logs = logs,
            activeMedications = activeMedications,
            totalSlots = activeMedications.size,
        )
    }

    fun calculateAdherenceForRange(
        logs: List<MedicationLogEntity>,
        activeMedications: List<MedicationEntity>,
        days: Int,
    ): AdherenceSummary {
        return calculateForScheduledWindow(
            logs = logs,
            activeMedications = activeMedications,
            totalSlots = activeMedications.size * days.coerceAtLeast(0),
        )
    }

    fun getPendingMedicationCount(summary: AdherenceSummary): Int = summary.totalPending

    fun getTakenMedicationCount(summary: AdherenceSummary): Int = summary.totalTaken

    private fun calculateForScheduledWindow(
        logs: List<MedicationLogEntity>,
        activeMedications: List<MedicationEntity>,
        totalSlots: Int,
    ): AdherenceSummary {
        if (activeMedications.isEmpty()) {
            return AdherenceSummary(
                totalScheduled = 0,
                totalTaken = 0,
                totalPending = 0,
                adherencePercentage = 100,
                hasActiveMedications = false,
            )
        }

        val takenKeys = logs
            .filter { it.status == "TAKEN" }
            .map { log -> log.medicationId to dayKey(log.scheduledFor) }
            .toSet()

        val totalTaken = takenKeys.size.coerceAtMost(totalSlots)
        val totalPending = (totalSlots - totalTaken).coerceAtLeast(0)
        val percentage = if (totalSlots == 0) {
            100
        } else {
            ((totalTaken.toDouble() / totalSlots.toDouble()) * 100.0).toInt()
        }

        return AdherenceSummary(
            totalScheduled = totalSlots,
            totalTaken = totalTaken,
            totalPending = totalPending,
            adherencePercentage = percentage,
            hasActiveMedications = true,
        )
    }

    private fun dayKey(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }
}
