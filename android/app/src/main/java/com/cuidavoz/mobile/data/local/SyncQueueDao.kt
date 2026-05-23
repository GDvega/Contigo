package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncQueueEntity)

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE status IN ('PENDING', 'FAILED')
        ORDER BY createdAt ASC
        """
    )
    suspend fun getPending(): List<SyncQueueEntity>

    @Query(
        """
        SELECT * FROM sync_queue
        WHERE status IN ('PENDING', 'FAILED')
        ORDER BY createdAt ASC
        """
    )
    fun observePending(): Flow<List<SyncQueueEntity>>

    @Query(
        """
        UPDATE sync_queue
        SET status = 'SYNCING', updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markSyncing(id: String, updatedAt: Long)

    @Query(
        """
        UPDATE sync_queue
        SET status = 'SYNCED', lastError = NULL, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markSynced(id: String, updatedAt: Long)

    @Query(
        """
        UPDATE sync_queue
        SET status = 'FAILED',
            retryCount = retryCount + 1,
            lastError = :lastError,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun markFailed(id: String, lastError: String?, updatedAt: Long)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED' AND updatedAt < :threshold")
    suspend fun deleteSyncedOlderThan(threshold: Long)
}
