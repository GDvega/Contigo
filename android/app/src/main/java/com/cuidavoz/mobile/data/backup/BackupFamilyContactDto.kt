package com.cuidavoz.mobile.data.backup

data class BackupFamilyContactDto(
    val id: String,
    val patientId: String,
    val fullName: String,
    val phone: String,
    val relationship: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
