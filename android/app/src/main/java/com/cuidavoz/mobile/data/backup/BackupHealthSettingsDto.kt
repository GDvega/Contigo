package com.cuidavoz.mobile.data.backup

data class BackupHealthSettingsDto(
    val id: String,
    val patientId: String,
    val systolicMinNormal: Int,
    val systolicMaxNormal: Int,
    val diastolicMinNormal: Int,
    val diastolicMaxNormal: Int,
    val pulseMinNormal: Int,
    val pulseMaxNormal: Int,
    val doctorRecommendation: String?,
    val updatedAt: Long,
)
