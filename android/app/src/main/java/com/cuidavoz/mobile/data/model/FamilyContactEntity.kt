package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_contacts")
data class FamilyContactEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val fullName: String,
    val phone: String,
    val relationship: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
