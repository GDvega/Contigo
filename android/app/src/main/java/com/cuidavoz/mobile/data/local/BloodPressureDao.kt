package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BloodPressureDao {
    @Query(
        """
        SELECT * FROM blood_pressure_readings
        WHERE patientId = :patientId
        ORDER BY measuredAt DESC
        """
    )
    fun observeRecentReadings(patientId: String): Flow<List<BloodPressureEntity>>

    @Query(
        """
        SELECT * FROM blood_pressure_readings
        WHERE patientId = :patientId
        ORDER BY measuredAt DESC
        """
    )
    suspend fun getRecentReadings(patientId: String): List<BloodPressureEntity>

    @Query(
        """
        SELECT * FROM blood_pressure_readings
        WHERE patientId = :patientId
          AND (
            :beforeMeasuredAt IS NULL
            OR measuredAt < :beforeMeasuredAt
            OR (measuredAt = :beforeMeasuredAt AND id < :beforeId)
          )
        ORDER BY measuredAt DESC, id DESC
        LIMIT :pageSize
        """
    )
    suspend fun getReadingsPage(
        patientId: String,
        beforeMeasuredAt: Long?,
        beforeId: String?,
        pageSize: Int,
    ): List<BloodPressureEntity>

    @Query("SELECT * FROM blood_pressure_readings WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BloodPressureEntity?

    @Query(
        """
        SELECT * FROM blood_pressure_readings
        WHERE patientId = :patientId
          AND measuredAt >= :startAt
          AND measuredAt < :endAt
        ORDER BY measuredAt DESC
        """
    )
    fun observeReadingsForRange(
        patientId: String,
        startAt: Long,
        endAt: Long,
    ): Flow<List<BloodPressureEntity>>

    @Query(
        """
        SELECT * FROM blood_pressure_readings
        WHERE patientId = :patientId
          AND measuredAt >= :startAt
          AND measuredAt < :endAt
        ORDER BY measuredAt DESC
        """
    )
    suspend fun getReadingsForRange(
        patientId: String,
        startAt: Long,
        endAt: Long,
    ): List<BloodPressureEntity>

    @Query(
        """
        SELECT * FROM blood_pressure_readings
        WHERE patientId = :patientId
          AND measuredAt >= :startOfDay
          AND measuredAt < :endOfDay
        ORDER BY measuredAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatestReadingForDay(
        patientId: String,
        startOfDay: Long,
        endOfDay: Long,
    ): BloodPressureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BloodPressureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<BloodPressureEntity>)

    @Query(
        """
        UPDATE blood_pressure_readings
        SET patientId = :patientId
        WHERE TRIM(patientId) = ''
        """
    )
    suspend fun reassignBlankPatientIds(patientId: String)

    @Query(
        """
        UPDATE blood_pressure_readings
        SET patientId = :newPatientId
        WHERE patientId = :oldPatientId
        """
    )
    suspend fun migratePatientId(
        oldPatientId: String,
        newPatientId: String,
    ): Int

    @Query("DELETE FROM blood_pressure_readings")
    suspend fun deleteAll()

    @Query("DELETE FROM blood_pressure_readings WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Delete
    suspend fun delete(reading: BloodPressureEntity)
}
