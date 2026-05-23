package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {
    @Query(
        """
        SELECT * FROM medication_logs
        WHERE patientId = :patientId
          AND scheduledFor >= :startOfDay
          AND scheduledFor < :endOfDay
        ORDER BY scheduledFor ASC, createdAt ASC
        """
    )
    fun observeLogsForDay(
        patientId: String,
        startOfDay: Long,
        endOfDay: Long,
    ): Flow<List<MedicationLogEntity>>

    @Query(
        """
        SELECT * FROM medication_logs
        WHERE patientId = :patientId
          AND scheduledFor >= :startOfDay
          AND scheduledFor < :endOfDay
        ORDER BY scheduledFor ASC, createdAt ASC
        """
    )
    suspend fun getLogsForDay(
        patientId: String,
        startOfDay: Long,
        endOfDay: Long,
    ): List<MedicationLogEntity>

    @Query(
        """
        SELECT * FROM medication_logs
        WHERE patientId = :patientId
          AND scheduledFor >= :startAt
          AND scheduledFor < :endAt
        ORDER BY scheduledFor DESC, createdAt DESC
        """
    )
    fun observeLogsForRange(
        patientId: String,
        startAt: Long,
        endAt: Long,
    ): Flow<List<MedicationLogEntity>>

    @Query(
        """
        SELECT * FROM medication_logs
        WHERE patientId = :patientId
          AND scheduledFor >= :startAt
          AND scheduledFor < :endAt
        ORDER BY scheduledFor DESC, createdAt DESC
        """
    )
    suspend fun getLogsForRange(
        patientId: String,
        startAt: Long,
        endAt: Long,
    ): List<MedicationLogEntity>

    @Query(
        """
        SELECT * FROM medication_logs
        WHERE medicationId = :medicationId
          AND patientId = :patientId
          AND scheduledFor = :scheduledFor
          AND status = 'TAKEN'
        LIMIT 1
        """
    )
    suspend fun getTakenLogForMedication(
        medicationId: String,
        patientId: String,
        scheduledFor: Long,
    ): MedicationLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MedicationLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<MedicationLogEntity>)

    @Query(
        """
        UPDATE medication_logs
        SET patientId = :patientId
        WHERE TRIM(patientId) = ''
        """
    )
    suspend fun reassignBlankPatientIds(patientId: String)

    @Query("DELETE FROM medication_logs")
    suspend fun deleteAll()
}
