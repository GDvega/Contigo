package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import java.time.LocalDate

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
    val daysOfWeek: List<Int> = MedicationScheduleDefaults.allDaysOfWeek.toList(),
    val specificDates: List<LocalDate> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)
