package com.cuidavoz.mobile.data.backup

data class BackupBloodPressureDto(
    val id: String,
    val patientId: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val status: String,
    val notes: String?,
    val measuredAt: Long,
    val createdAt: Long,
)
