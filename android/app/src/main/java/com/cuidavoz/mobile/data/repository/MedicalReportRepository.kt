package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.BloodPressureDao
import com.cuidavoz.mobile.data.local.HealthSettingsDao
import com.cuidavoz.mobile.data.local.MedicationDao
import com.cuidavoz.mobile.data.local.MedicationLogDao
import com.cuidavoz.mobile.data.local.PatientDao
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.domain.AdherenceSummary
import com.cuidavoz.mobile.domain.MedicationScheduleCalculator
import com.cuidavoz.mobile.domain.treatmentSummary
import com.cuidavoz.mobile.domain.isExpired
import com.cuidavoz.mobile.domain.PressureStatus
import com.cuidavoz.mobile.domain.report.MedicalReportData
import com.cuidavoz.mobile.domain.report.MedicalReportPeriod
import com.cuidavoz.mobile.domain.report.MedicationReportEntry
import com.cuidavoz.mobile.domain.report.MedicationReportSummary
import com.cuidavoz.mobile.domain.report.PressureReportSummary
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class MedicalReportRepository(
    private val patientDao: PatientDao,
    private val familyContactRepository: FamilyContactRepository,
    private val healthSettingsDao: HealthSettingsDao,
    private val medicationDao: MedicationDao,
    private val medicationLogDao: MedicationLogDao,
    private val bloodPressureDao: BloodPressureDao,
) {
    suspend fun buildReportData(period: MedicalReportPeriod): MedicalReportData = withContext(Dispatchers.IO) {
        val generatedAt = System.currentTimeMillis()
        val patient = patientDao.getCurrentPatient()
        val familyContact = familyContactRepository.getPrimaryContact(DEFAULT_PATIENT_ID)
        val healthSettings = healthSettingsDao.getSettings(DEFAULT_PATIENT_ID)
        val activeMedications = medicationDao.getActiveMedications(DEFAULT_PATIENT_ID)

        val periodStart = resolvePeriodStart(
            period = period,
            generatedAt = generatedAt,
            patientCreatedAt = patient?.createdAt,
            activeMedications = activeMedications,
        )
        val queryEnd = generatedAt + 1L

        val pressureReadings = if (period == MedicalReportPeriod.ALL) {
            bloodPressureDao.getRecentReadings(DEFAULT_PATIENT_ID)
                .filter { it.measuredAt in periodStart until queryEnd }
        } else {
            bloodPressureDao.getReadingsForRange(DEFAULT_PATIENT_ID, periodStart, queryEnd)
        }.sortedByDescending { it.measuredAt }

        val medicationLogs = medicationLogDao.getLogsForRange(DEFAULT_PATIENT_ID, periodStart, queryEnd)
            .sortedByDescending { it.scheduledFor }

        val medicationEntries = buildMedicationEntries(
            medications = activeMedications,
            logs = medicationLogs,
            periodStart = periodStart,
            generatedAt = generatedAt,
        )

        val pressureSummary = buildPressureSummary(pressureReadings)
        val medicationSummary = buildMedicationSummary(
            medications = activeMedications,
            logs = medicationLogs,
            entries = medicationEntries,
        )
        val adherenceSummary = AdherenceSummary(
            totalScheduled = medicationSummary.takenCount + medicationSummary.pendingOrSkippedCount,
            totalTaken = medicationSummary.takenCount,
            totalPending = medicationSummary.pendingOrSkippedCount,
            adherencePercentage = medicationSummary.adherencePercentage,
            hasActiveMedications = activeMedications.isNotEmpty(),
        )

        MedicalReportData(
            patient = patient,
            familyContact = familyContact,
            healthSettings = healthSettings,
            period = period,
            periodStart = periodStart,
            periodEnd = generatedAt,
            generatedAt = generatedAt,
            pressureReadings = pressureReadings,
            pressureSummary = pressureSummary,
            activeMedications = activeMedications,
            medicationLogs = medicationLogs,
            medicationEntries = medicationEntries,
            medicationSummary = medicationSummary,
            adherenceSummary = adherenceSummary,
            doctorRecommendation = healthSettings?.doctorRecommendation,
        )
    }

    private suspend fun resolvePeriodStart(
        period: MedicalReportPeriod,
        generatedAt: Long,
        patientCreatedAt: Long?,
        activeMedications: List<MedicationEntity>,
    ): Long {
        val zoneId = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(generatedAt).atZone(zoneId).toLocalDate()

        return when (period) {
            MedicalReportPeriod.LAST_7_DAYS ->
                today.minusDays(6).atStartOfDay(zoneId).toInstant().toEpochMilli()
            MedicalReportPeriod.LAST_30_DAYS ->
                today.minusDays(29).atStartOfDay(zoneId).toInstant().toEpochMilli()
            MedicalReportPeriod.ALL -> {
                val firstPressure = bloodPressureDao.getRecentReadings(DEFAULT_PATIENT_ID)
                    .minOfOrNull(BloodPressureEntity::measuredAt)
                val firstLog = medicationLogDao.getLogsForRange(DEFAULT_PATIENT_ID, 0L, Long.MAX_VALUE)
                    .minOfOrNull(MedicationLogEntity::scheduledFor)
                val firstMedication = activeMedications.minOfOrNull(MedicationEntity::createdAt)
                listOfNotNull(patientCreatedAt, firstPressure, firstLog, firstMedication)
                    .minOrNull()
                    ?.let { timestamp ->
                        Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
                            .atStartOfDay(zoneId)
                            .toInstant()
                            .toEpochMilli()
                    }
                    ?: today.atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
        }
    }

    private fun buildPressureSummary(
        readings: List<BloodPressureEntity>,
    ): PressureReportSummary {
        val latestPressure = readings.firstOrNull()
        val averageSystolic = readings.takeIf { it.isNotEmpty() }
            ?.map(BloodPressureEntity::systolic)
            ?.average()
            ?.roundToInt()
        val averageDiastolic = readings.takeIf { it.isNotEmpty() }
            ?.map(BloodPressureEntity::diastolic)
            ?.average()
            ?.roundToInt()
        val averagePulse = readings.mapNotNull(BloodPressureEntity::pulse)
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()

        return PressureReportSummary(
            totalPressureReadings = readings.size,
            latestPressure = latestPressure,
            averageSystolic = averageSystolic,
            averageDiastolic = averageDiastolic,
            averagePulse = averagePulse,
            outOfRangeCount = readings.count { it.status == PressureStatus.OUT_OF_RANGE.name },
            highOrCriticalCount = readings.count {
                it.status == PressureStatus.HIGH.name || it.status == PressureStatus.CRITICAL.name
            },
        )
    }

    private fun buildMedicationSummary(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        entries: List<MedicationReportEntry>,
    ): MedicationReportSummary {
        val takenCount = entries.count { it.status == "TAKEN" }
        val pendingOrSkippedCount = entries.count { it.status != "TAKEN" }
        val totalScheduled = takenCount + pendingOrSkippedCount
        val adherencePercentage = if (totalScheduled == 0) {
            100
        } else {
            ((takenCount.toDouble() / totalScheduled.toDouble()) * 100.0).roundToInt()
        }

        return MedicationReportSummary(
            activeMedicationCount = medications.size,
            totalMedicationLogs = logs.size,
            takenCount = takenCount,
            pendingOrSkippedCount = pendingOrSkippedCount,
            adherencePercentage = adherencePercentage,
        )
    }

    private fun buildMedicationEntries(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        periodStart: Long,
        generatedAt: Long,
    ): List<MedicationReportEntry> {
        if (medications.isEmpty()) {
            return emptyList()
        }

        val zoneId = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(periodStart).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(generatedAt).atZone(zoneId).toLocalDate()
        val logByKey = logs.associateBy { "${it.medicationId}_${it.scheduledFor}" }
        val entries = mutableListOf<MedicationReportEntry>()

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            medications.forEach { medication ->
                val medicationStartDate = Instant.ofEpochMilli(maxOf(medication.createdAt, periodStart))
                    .atZone(zoneId)
                    .toLocalDate()
                if (currentDate.isBefore(medicationStartDate)) {
                    return@forEach
                }
                if (!MedicationScheduleCalculator.isMedicationDueOnDate(medication, currentDate)) {
                    return@forEach
                }

                val scheduledFor = scheduleTimeToMillis(
                    scheduleTime = medication.scheduleTime,
                    day = currentDate,
                    zoneId = zoneId,
                )
                if (scheduledFor < periodStart || scheduledFor > generatedAt) {
                    return@forEach
                }

                val log = logByKey["${medication.id}_$scheduledFor"]
                entries += MedicationReportEntry(
                    medicationId = medication.id,
                    medicationName = medication.name,
                    dose = medication.dose,
                    scheduleTime = medication.scheduleTime,
                    treatmentDuration = medication.treatmentSummary(),
                    activeStatus = if (medication.isExpired(currentDate)) "Vencido" else "Activo",
                    instructions = medication.instructions,
                    scheduledFor = scheduledFor,
                    status = log?.status ?: "PENDING",
                    skipReason = log?.skipReason,
                    takenAt = log?.takenAt,
                )
            }
            currentDate = currentDate.plusDays(1)
        }

        return entries.sortedByDescending { it.scheduledFor }
    }
}
