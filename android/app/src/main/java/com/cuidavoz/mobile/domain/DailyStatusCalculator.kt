package com.cuidavoz.mobile.domain

import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import java.time.LocalDate
import com.cuidavoz.mobile.domain.isExpired

enum class DailyRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
}

data class DailyStatusSnapshot(
    val latestPressureToday: BloodPressureEntity?,
    val medicationGroups: List<MedicationGroup>,
    val nextMedicationGroup: MedicationGroup?,
    val activeMedicationCount: Int,
    val takenMedicationCount: Int,
    val pendingMedicationCount: Int,
    val riskLevel: DailyRiskLevel,
    val statusTitle: String,
)

object DailyStatusCalculator {
    fun calculate(
        medications: List<MedicationEntity>,
        todayMedicationLogs: List<MedicationLogEntity>,
        latestPressureToday: BloodPressureEntity?,
        today: LocalDate = LocalDate.now(),
    ): DailyStatusSnapshot {
        val nonExpiredMedications = medications.filter { !it.isExpired(today) }

        val medicationGroups = groupMedicationsWithPending(
            medications = nonExpiredMedications,
            todayMedicationLogs = todayMedicationLogs,
            today = today,
        )
        val nextMedicationGroup = MedicationGrouping.getNextMedicationGroup(
            medications = nonExpiredMedications,
            medicationLogs = todayMedicationLogs,
            today = today,
        )

        val todayMedications = MedicationGrouping.medicationsDueOnDate(nonExpiredMedications, today)
        val loggedIds = todayMedicationLogs
            .map { it.medicationId }
            .toSet()
        val takenIds = todayMedicationLogs
            .filter { it.status == "TAKEN" }
            .map { it.medicationId }
            .toSet()
        val activeMedicationCount = todayMedications.size
        val takenMedicationCount = todayMedications.count { it.id in takenIds }
        val pendingMedicationCount = todayMedications.count { it.id !in loggedIds }

        val riskLevel = when (latestPressureToday?.status) {
            PressureStatus.CRITICAL.name,
            PressureStatus.HIGH.name -> DailyRiskLevel.HIGH
            PressureStatus.OUT_OF_RANGE.name -> DailyRiskLevel.MEDIUM
            else -> {
                if (pendingMedicationCount > 0) DailyRiskLevel.MEDIUM else DailyRiskLevel.LOW
            }
        }

        val statusTitle = when (riskLevel) {
            DailyRiskLevel.LOW -> "Todo en orden"
            DailyRiskLevel.MEDIUM -> "Hay pendientes"
            DailyRiskLevel.HIGH -> "Revisar con familiar o médico"
        }

        return DailyStatusSnapshot(
            latestPressureToday = latestPressureToday,
            medicationGroups = medicationGroups,
            nextMedicationGroup = nextMedicationGroup,
            activeMedicationCount = activeMedicationCount,
            takenMedicationCount = takenMedicationCount,
            pendingMedicationCount = pendingMedicationCount,
            riskLevel = riskLevel,
            statusTitle = statusTitle,
        )
    }

    fun getPendingMedicationsForTime(
        scheduleTime: String,
        medications: List<MedicationEntity>,
        todayMedicationLogs: List<MedicationLogEntity>,
        today: LocalDate = LocalDate.now(),
    ): List<MedicationEntity> {
        return MedicationGrouping.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            medicationLogs = todayMedicationLogs,
            date = today,
        )
    }

    private fun groupMedicationsWithPending(
        medications: List<MedicationEntity>,
        todayMedicationLogs: List<MedicationLogEntity>,
        today: LocalDate,
    ): List<MedicationGroup> {
        return MedicationGrouping.groupMedicationsByScheduleTime(medications, today).map { group ->
            group.copy(
                pendingMedications = MedicationGrouping.getPendingMedicationsForTime(
                    scheduleTime = group.scheduleTime,
                    medications = group.medications,
                    medicationLogs = todayMedicationLogs,
                    date = today,
                ),
            )
        }
    }
}
