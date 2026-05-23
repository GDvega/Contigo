package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_settings")
data class HealthSettingsEntity(
    @PrimaryKey val id: String,
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
