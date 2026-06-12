package com.cuidavoz.mobile.data.backup

data class ContigoBackup(
    val app: String,
    val backupVersion: Int,
    val createdAt: Long,
    val deviceInfo: String?,
    val patient: BackupPatientDto?,
    val familyContact: BackupFamilyContactDto?,
    val healthSettings: BackupHealthSettingsDto?,
    val medications: List<BackupMedicationDto>,
    val medicationLogs: List<BackupMedicationLogDto>,
    val bloodPressureReadings: List<BackupBloodPressureDto>,
    val preferences: BackupPreferencesDto?,
    val images: List<BackupImageDto>,
)
