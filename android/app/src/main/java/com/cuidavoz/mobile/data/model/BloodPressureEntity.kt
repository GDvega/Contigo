package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_pressure_readings")
data class BloodPressureEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val status: String,
    val notes: String?,
    val measuredAt: Long,
    val createdAt: Long,
)
