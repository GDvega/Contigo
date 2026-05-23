package com.cuidavoz.mobile.data.backup

data class BackupMedicationDto(
    val id: String,
    val patientId: String,
    val name: String,
    val dose: String,
    val color: String?,
    val shape: String?,
    val instructions: String?,
    val scheduleTime: String,
    val isActive: Boolean,
    val scheduleType: String,
    val startDate: String,
    val endDate: String?,
    val daysOfWeekJson: String,
    val specificDatesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
)
