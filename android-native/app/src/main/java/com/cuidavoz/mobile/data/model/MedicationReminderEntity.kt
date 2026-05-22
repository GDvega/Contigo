package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_reminders")
data class MedicationReminderEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val reminderGroupId: String,
    val scheduleTime: String,
    val targetDate: String,
    val medicationIds: String,
    val medicationNames: String,
    val alarmRequestCode: Int,
    val attemptNumber: Int,
    val maxAttempts: Int,
    val repeatEveryMinutes: Int,
    val scheduledAt: Long,
    val respondedAt: Long?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)
