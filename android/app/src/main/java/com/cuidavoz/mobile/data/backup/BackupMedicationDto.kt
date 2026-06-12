package com.cuidavoz.mobile.data.backup

import java.time.LocalDate

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
    val daysOfWeek: List<Int>,
    val specificDates: List<LocalDate>,
    val createdAt: Long,
    val updatedAt: Long,
)
