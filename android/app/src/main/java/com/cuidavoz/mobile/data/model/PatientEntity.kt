package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val age: Int?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
