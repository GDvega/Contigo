package com.cuidavoz.mobile.data.backup

data class BackupMedicationLogDto(
    val id: String,
    val medicationId: String,
    val patientId: String,
    val scheduledFor: Long,
    val takenAt: Long?,
    val status: String,
    val createdAt: Long,
)
