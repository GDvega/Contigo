package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.BloodPressureDao
import com.cuidavoz.mobile.data.local.MedicationDao
import com.cuidavoz.mobile.data.local.MedicationLogDao
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.domain.DailyRiskLevel
import com.cuidavoz.mobile.domain.DailyStatusCalculator
import com.cuidavoz.mobile.domain.DailyStatusSnapshot
import com.cuidavoz.mobile.domain.MedicationScheduleCalculator
import com.cuidavoz.mobile.util.createLocalId
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import com.cuidavoz.mobile.util.todayRangeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.ZoneId

class DailyStatusRepository(
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val bloodPressureDao: BloodPressureDao,
    private val firebaseSyncManager: FirebaseSyncManager? = null,
) {
    fun observeDailyStatus(patientId: String): Flow<DailyStatusSnapshot> {
        val (startOfDay, endOfDay) = todayRangeMillis()

        return combine(
            medicationDao.observeActiveMedications(patientId),
            medicationLogDao.observeLogsForDay(patientId, startOfDay, endOfDay),
            bloodPressureDao.observeRecentReadings(patientId),
        ) { medications, medicationLogs, readings ->
            val today = LocalDate.now()
            DailyStatusCalculator.calculate(
                medications = medications,
                todayMedicationLogs = medicationLogs,
                latestPressureToday = readings.firstOrNull { reading ->
                    reading.measuredAt in startOfDay until endOfDay
                },
                today = today,
            )
        }
    }

    suspend fun markMedicationTaken(
        patientId: String,
        medication: MedicationEntity,
    ): Boolean {
        val today = LocalDate.now()
        if (!MedicationScheduleCalculator.isMedicationDueOnDate(medication, today)) {
            return false
        }
        val scheduledFor = scheduleTimeToMillis(medication.scheduleTime, today, ZoneId.systemDefault())
        val existingLog = medicationLogDao.getTakenLogForMedication(
            medicationId = medication.id,
            patientId = patientId,
            scheduledFor = scheduledFor,
        )
        if (existingLog != null) {
            return false
        }

        val log = MedicationLogEntity(
                id = createLocalId("medication_log"),
                medicationId = medication.id,
                patientId = patientId,
                scheduledFor = scheduledFor,
                takenAt = System.currentTimeMillis(),
                status = "TAKEN",
                createdAt = System.currentTimeMillis(),
        )
        medicationLogDao.insert(log)
        firebaseSyncManager?.enqueueMedicationLog(log)
        return true
    }

    suspend fun markMedicationGroupTaken(
        patientId: String,
        scheduleTime: String,
    ): Boolean {
        val medications = medicationDao.getActiveMedications(patientId)
        val todayLogs = getTodayMedicationLogs(patientId)
        val pendingMedications = DailyStatusCalculator.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            todayMedicationLogs = todayLogs,
            today = LocalDate.now(),
        )

        if (pendingMedications.isEmpty()) {
            return false
        }

        val now = System.currentTimeMillis()
        val logs = pendingMedications.map { medication ->
            MedicationLogEntity(
                id = createLocalId("medication_log"),
                medicationId = medication.id,
                patientId = patientId,
                scheduledFor = scheduleTimeToMillis(scheduleTime, LocalDate.now(), ZoneId.systemDefault()),
                takenAt = now,
                status = "TAKEN",
                createdAt = now,
            )
        }
        medicationLogDao.insertAll(logs)
        logs.forEach { firebaseSyncManager?.enqueueMedicationLog(it) }
        return true
    }

    suspend fun markReminderGroupTaken(
        patientId: String,
        medicationIds: List<String>,
        scheduledFor: Long,
    ): Boolean {
        if (medicationIds.isEmpty()) {
            return false
        }
        val existingLogs = medicationLogDao.getLogsForRange(patientId, scheduledFor, scheduledFor + 1L)
            .filter { it.status == "TAKEN" }
            .map { it.medicationId }
            .toSet()
        val pendingIds = medicationIds.filterNot(existingLogs::contains)
        if (pendingIds.isEmpty()) {
            return false
        }
        val now = System.currentTimeMillis()
        val logs = pendingIds.map { medicationId ->
            MedicationLogEntity(
                id = createLocalId("medication_log"),
                medicationId = medicationId,
                patientId = patientId,
                scheduledFor = scheduledFor,
                takenAt = now,
                status = "TAKEN",
                createdAt = now,
            )
        }
        medicationLogDao.insertAll(logs)
        logs.forEach { firebaseSyncManager?.enqueueMedicationLog(it) }
        return true
    }

    suspend fun getTodayMedicationLogs(patientId: String): List<MedicationLogEntity> {
        val (startOfDay, endOfDay) = todayRangeMillis()
        return medicationLogDao.getLogsForDay(patientId, startOfDay, endOfDay)
    }

    suspend fun getMedicationAdherenceForToday(patientId: String): MedicationAdherence {
        val medications = medicationDao.getActiveMedications(patientId)
        val logs = getTodayMedicationLogs(patientId)
        val today = LocalDate.now()
        val dueToday = medications.filter { MedicationScheduleCalculator.isMedicationDueOnDate(it, today) }
        val takenIds = logs.filter { it.status == "TAKEN" }.map { it.medicationId }.toSet()
        val takenCount = dueToday.count { it.id in takenIds }
        return MedicationAdherence(
            total = dueToday.size,
            taken = takenCount,
            pending = (dueToday.size - takenCount).coerceAtLeast(0),
        )
    }
}

data class MedicationAdherence(
    val total: Int,
    val taken: Int,
    val pending: Int,
)
