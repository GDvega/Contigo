package com.cuidavoz.mobile.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val status: String,
    val retryCount: Int,
    val lastError: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
