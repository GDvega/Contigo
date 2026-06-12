package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_logs")
data class MedicationLogEntity(
    @PrimaryKey val id: String,
    val medicationId: String,
    val patientId: String,
    val scheduledFor: Long,
    val takenAt: Long?,
    val status: String,
    val skipReason: String? = null,
    val createdAt: Long,
)
