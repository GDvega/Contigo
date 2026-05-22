package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val name: String,
    val dose: String,
    val color: String?,
    val shape: String?,
    val instructions: String?,
    val scheduleTime: String,
    val imageUri: String?,
    val isActive: Boolean,
    val scheduleType: String = "ALWAYS",
    val startDate: String = MedicationScheduleDefaults.todayIso(),
    val endDate: String? = null,
    val daysOfWeekJson: String = MedicationScheduleDefaults.allDaysJson(),
    val specificDatesJson: String = MedicationScheduleDefaults.emptyDatesJson(),
    val createdAt: Long,
    val updatedAt: Long,
)
