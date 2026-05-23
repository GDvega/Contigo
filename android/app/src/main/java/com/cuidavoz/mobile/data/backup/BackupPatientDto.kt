package com.cuidavoz.mobile.data.backup

data class BackupPatientDto(
    val id: String,
    val fullName: String,
    val age: Int?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
