package com.cuidavoz.mobile.data.backup

import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.domain.sync.MedicationImageSyncOperation

data class BackupRestoreSyncPlan(
    val strategy: ImportStrategy,
    val patient: PatientEntity?,
    val familyContact: FamilyContactEntity?,
    val healthSettings: HealthSettingsEntity?,
    val medications: List<RestoredMedicationSync>,
    val pressureReadings: List<BloodPressureEntity>,
    val medicationLogs: List<MedicationLogEntity>,
)

data class RestoredMedicationSync(
    val medication: MedicationEntity,
    val imageOperation: MedicationImageSyncOperation,
)
