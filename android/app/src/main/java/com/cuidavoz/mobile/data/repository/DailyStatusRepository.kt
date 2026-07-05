package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.BloodPressureDao
import com.cuidavoz.mobile.data.local.MedicationDao
import com.cuidavoz.mobile.data.local.MedicationLogDao
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.sync.SyncManager
import com.cuidavoz.mobile.domain.DailyStatusCalculator
import com.cuidavoz.mobile.domain.DailyStatusSnapshot
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.domain.MedicationDoseStatus
import com.cuidavoz.mobile.domain.MedicationOutcomeResult
import com.cuidavoz.mobile.domain.MedicationScheduleCalculator
import com.cuidavoz.mobile.domain.isExpired
import com.cuidavoz.mobile.domain.MedicationSkipReason
import com.cuidavoz.mobile.util.createLocalId
import com.cuidavoz.mobile.util.formatScheduleTime
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import com.cuidavoz.mobile.util.todayRangeMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DailyStatusRepository(
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val bloodPressureDao: BloodPressureDao,
    private val firebaseSyncManager: SyncManager? = null,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDailyStatus(patientId: String): Flow<DailyStatusSnapshot> {
        return currentDateFlow().flatMapLatest { date ->
            val (startOfDay, endOfDay) = todayRangeMillis(date)

            combine(
                medicationDao.observeActiveMedications(patientId),
                medicationLogDao.observeLogsForDay(patientId, startOfDay, endOfDay),
                bloodPressureDao.observeRecentReadings(patientId),
            ) { medications, medicationLogs, readings ->
                DailyStatusCalculator.calculate(
                    medications = medications,
                    todayMedicationLogs = medicationLogs,
                    latestPressureToday = readings.firstOrNull { reading ->
                        reading.measuredAt in startOfDay until endOfDay
                    },
                    today = date,
                )
            }
        }
    }

    suspend fun markMedicationTaken(
        patientId: String,
        medication: MedicationEntity,
    ): Boolean {
        val result = recordMedicationOutcomes(
            patientId = patientId,
            scheduleTime = medication.scheduleTime,
            outcomes = listOf(
                MedicationDoseOutcome(
                    medicationId = medication.id,
                    status = MedicationDoseStatus.TAKEN,
                ),
            ),
        )
        return result.savedCount > 0
    }

    suspend fun markMedicationGroupTaken(
        patientId: String,
        scheduleTime: String,
    ): Boolean {
        val result = recordAllPendingAsTaken(patientId, scheduleTime)
        return result.savedCount > 0
    }

    suspend fun markReminderGroupTaken(
        patientId: String,
        medicationIds: List<String>,
        scheduledFor: Long,
    ): Boolean {
        if (medicationIds.isEmpty()) {
            return false
        }
        val result = recordMedicationOutcomesForSlot(
            patientId = patientId,
            scheduledFor = scheduledFor,
            outcomes = medicationIds.map {
                MedicationDoseOutcome(
                    medicationId = it,
                    status = MedicationDoseStatus.TAKEN,
                )
            },
        )
        return result.anyRecorded
    }

    suspend fun recordMedicationOutcomes(
        patientId: String,
        scheduleTime: String,
        outcomes: List<MedicationDoseOutcome>,
    ): MedicationOutcomeResult {
        if (outcomes.isEmpty()) {
            return emptyOutcomeForScheduleTime(patientId, scheduleTime)
        }
        val today = LocalDate.now()
        val scheduledFor = scheduleTimeToMillis(scheduleTime, today, ZoneId.systemDefault())
        val medications = medicationDao.getActiveMedications(patientId)
        val todayLogs = getTodayMedicationLogs(patientId)
        val pendingIds = DailyStatusCalculator.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            todayMedicationLogs = todayLogs,
            today = today,
        ).map { it.id }.toSet()
        val validOutcomes = outcomes.filter { outcome ->
            outcome.medicationId in pendingIds &&
                medications.any { medication ->
                    medication.id == outcome.medicationId &&
                        MedicationScheduleCalculator.isMedicationDueOnDate(medication, today)
                }
        }
        val slotResult = recordMedicationOutcomesForSlot(
            patientId = patientId,
            scheduledFor = scheduledFor,
            scheduleTime = scheduleTime,
            outcomes = validOutcomes,
        )
        val stillPending = countPendingForScheduleTime(patientId, scheduleTime)
        return slotResult.copy(stillPendingCount = stillPending)
    }

    suspend fun recordAllPendingAsTaken(
        patientId: String,
        scheduleTime: String,
    ): MedicationOutcomeResult {
        val today = LocalDate.now()
        val medications = medicationDao.getActiveMedications(patientId)
        val todayLogs = getTodayMedicationLogs(patientId)
        val pending = DailyStatusCalculator.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            todayMedicationLogs = todayLogs,
            today = today,
        )
        return recordMedicationOutcomes(
            patientId = patientId,
            scheduleTime = scheduleTime,
            outcomes = pending.map {
                MedicationDoseOutcome(
                    medicationId = it.id,
                    status = MedicationDoseStatus.TAKEN,
                )
            },
        )
    }

    suspend fun recordReminderOutcomes(
        patientId: String,
        scheduleTime: String,
        scheduledFor: Long,
        outcomes: List<MedicationDoseOutcome>,
    ): MedicationOutcomeResult {
        if (outcomes.isEmpty()) {
            return emptyOutcomeForScheduleTime(patientId, scheduleTime)
        }
        val today = LocalDate.now()
        val medications = medicationDao.getActiveMedications(patientId)
        val todayLogs = getTodayMedicationLogs(patientId)
        val pendingIds = DailyStatusCalculator.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            todayMedicationLogs = todayLogs,
            today = today,
        ).map { it.id }.toSet()
        val validOutcomes = outcomes.filter { outcome ->
            outcome.medicationId in pendingIds &&
                medications.any { medication ->
                    medication.id == outcome.medicationId &&
                        MedicationScheduleCalculator.isMedicationDueOnDate(medication, today)
                }
        }
        val slotResult = recordMedicationOutcomesForSlot(
            patientId = patientId,
            scheduledFor = scheduledFor,
            scheduleTime = scheduleTime,
            outcomes = validOutcomes,
        )
        val stillPending = countPendingForScheduleTime(patientId, scheduleTime)
        return slotResult.copy(stillPendingCount = stillPending)
    }

    suspend fun getPendingMedicationIdsForScheduleTime(
        patientId: String,
        scheduleTime: String,
    ): List<String> {
        val today = LocalDate.now()
        val medications = medicationDao.getActiveMedications(patientId)
        val todayLogs = getTodayMedicationLogs(patientId)
        return DailyStatusCalculator.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            todayMedicationLogs = todayLogs,
            today = today,
        ).map { it.id }
    }

    suspend fun countPendingForScheduleTime(
        patientId: String,
        scheduleTime: String,
    ): Int {
        return countPendingForScheduleTimeOnDate(patientId, scheduleTime, LocalDate.now())
    }

    suspend fun getTodayMedicationLogs(patientId: String): List<MedicationLogEntity> {
        val (startOfDay, endOfDay) = todayRangeMillis()
        return medicationLogDao.getLogsForDay(patientId, startOfDay, endOfDay)
    }

    suspend fun getMedicationAdherenceForToday(patientId: String): MedicationAdherence {
        val medications = medicationDao.getActiveMedications(patientId)
        val logs = getTodayMedicationLogs(patientId)
        val today = LocalDate.now()
        val dueToday = medications
            .filter { !it.isExpired(today) }
            .filter { MedicationScheduleCalculator.isMedicationDueOnDate(it, today) }
        val loggedIds = logs.map { it.medicationId }.toSet()
        val takenIds = logs.filter { it.status == "TAKEN" }.map { it.medicationId }.toSet()
        return MedicationAdherence(
            total = dueToday.size,
            taken = dueToday.count { it.id in takenIds },
            pending = dueToday.count { it.id !in loggedIds },
        )
    }

    private suspend fun recordMedicationOutcomesForSlot(
        patientId: String,
        scheduledFor: Long,
        outcomes: List<MedicationDoseOutcome>,
        scheduleTime: String? = null,
    ): MedicationOutcomeResult {
        if (outcomes.isEmpty()) {
            return MedicationOutcomeResult(
                savedCount = 0,
                skippedCount = 0,
                stillPendingCount = 0,
            )
        }
        val existingLogs = medicationLogDao.getLogsForRange(patientId, scheduledFor, scheduledFor + 1L)
            .map { it.medicationId }
            .toSet()
        val newOutcomes = outcomes.filterNot { it.medicationId in existingLogs }
        if (newOutcomes.isEmpty()) {
            return MedicationOutcomeResult(
                savedCount = 0,
                skippedCount = 0,
                stillPendingCount = 0,
            )
        }
        val now = System.currentTimeMillis()
        val normalizedOutcomes = newOutcomes.map { outcome ->
            if (outcome.status == MedicationDoseStatus.SKIPPED && outcome.skipReason == null) {
                outcome.copy(skipReason = MedicationSkipReason.OTHER)
            } else {
                outcome
            }
        }
        val logs = normalizedOutcomes.map { outcome ->
            MedicationLogEntity(
                id = createLocalId("medication_log"),
                medicationId = outcome.medicationId,
                patientId = patientId,
                scheduledFor = scheduledFor,
                takenAt = if (outcome.status == MedicationDoseStatus.TAKEN) now else null,
                status = outcome.status.name,
                skipReason = outcome.skipReason?.name,
                createdAt = now,
            )
        }
        medicationLogDao.insertAll(logs)
        logs.forEach { firebaseSyncManager?.enqueueMedicationLog(it) }
        notifyCaregiverForSkippedMedications(
            scheduledFor = scheduledFor,
            scheduleTime = scheduleTime,
            outcomes = normalizedOutcomes.filter { it.status == MedicationDoseStatus.SKIPPED },
        )
        val savedCount = normalizedOutcomes.count { it.status == MedicationDoseStatus.TAKEN }
        val skippedCount = normalizedOutcomes.count { it.status == MedicationDoseStatus.SKIPPED }
        return MedicationOutcomeResult(
            savedCount = savedCount,
            skippedCount = skippedCount,
            stillPendingCount = 0,
        )
    }

    private suspend fun emptyOutcomeForScheduleTime(
        patientId: String,
        scheduleTime: String,
    ): MedicationOutcomeResult {
        val stillPending = countPendingForScheduleTime(patientId, scheduleTime)
        return MedicationOutcomeResult(
            savedCount = 0,
            skippedCount = 0,
            stillPendingCount = stillPending,
        )
    }

    private suspend fun countPendingForScheduleTimeOnDate(
        patientId: String,
        scheduleTime: String,
        today: LocalDate,
    ): Int {
        val medications = medicationDao.getActiveMedications(patientId)
        val todayLogs = getTodayMedicationLogs(patientId)
        return DailyStatusCalculator.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            todayMedicationLogs = todayLogs,
            today = today,
        ).size
    }

    private suspend fun notifyCaregiverForSkippedMedications(
        scheduledFor: Long,
        scheduleTime: String?,
        outcomes: List<MedicationDoseOutcome>,
    ) {
        if (outcomes.isEmpty()) return
        val syncManager = firebaseSyncManager ?: return
        val medications = medicationDao.getMedicationsByIds(outcomes.map { it.medicationId })
        val scheduleTimeLabel = scheduleTime?.let(::formatScheduleTime)
            ?: medications.firstOrNull()?.scheduleTime?.let(::formatScheduleTime)
            ?: "hoy"
        outcomes.forEach { outcome ->
            val reason = outcome.skipReason ?: MedicationSkipReason.OTHER
            val medicationName = medications.firstOrNull { it.id == outcome.medicationId }?.name ?: "Medicamento"
            syncManager.enqueueAlert(
                type = reason.alertType(),
                message = reason.alertMessage(medicationName, scheduleTimeLabel),
                medicationIds = listOf(outcome.medicationId),
                scheduledAt = scheduledFor,
                severity = reason.alertSeverity(),
            )
        }
    }
}

data class MedicationAdherence(
    val total: Int,
    val taken: Int,
    val pending: Int,
)

private fun currentDateFlow(zoneId: ZoneId = ZoneId.systemDefault()): Flow<LocalDate> {
    return flow {
        while (true) {
            val now = ZonedDateTime.now(zoneId)
            emit(now.toLocalDate())
            val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(zoneId).plusSeconds(1)
            delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1L))
        }
    }.distinctUntilChanged()
}
