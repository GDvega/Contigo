package com.cuidavoz.mobile.domain.report

import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.domain.AdherenceSummary

data class MedicalReportData(
    val patient: PatientEntity?,
    val familyContact: FamilyContactEntity?,
    val healthSettings: HealthSettingsEntity?,
    val period: MedicalReportPeriod,
    val periodStart: Long,
    val periodEnd: Long,
    val generatedAt: Long,
    val pressureReadings: List<BloodPressureEntity>,
    val pressureSummary: PressureReportSummary,
    val activeMedications: List<MedicationEntity>,
    val medicationLogs: List<MedicationLogEntity>,
    val medicationEntries: List<MedicationReportEntry>,
    val medicationSummary: MedicationReportSummary,
    val adherenceSummary: AdherenceSummary,
    val doctorRecommendation: String?,
)

data class MedicationReportEntry(
    val medicationId: String,
    val medicationName: String,
    val dose: String,
    val scheduleTime: String,
    val treatmentDuration: String,
    val activeStatus: String,
    val instructions: String?,
    val scheduledFor: Long,
    val status: String,
    val skipReason: String? = null,
    val takenAt: Long?,
)
