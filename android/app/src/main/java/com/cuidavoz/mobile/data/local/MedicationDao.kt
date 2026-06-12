package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.MedicationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query(
        """
        SELECT * FROM medications
        WHERE patientId = :patientId
        ORDER BY updatedAt DESC, name ASC
        """
    )
    suspend fun getAllMedications(patientId: String): List<MedicationEntity>

    @Query(
        """
        SELECT * FROM medications
        WHERE patientId = :patientId AND isActive = 1
        ORDER BY scheduleTime ASC, name ASC
        """
    )
    fun observeActiveMedications(patientId: String): Flow<List<MedicationEntity>>

    @Query(
        """
        SELECT * FROM medications
        WHERE patientId = :patientId AND isActive = 1
        ORDER BY scheduleTime ASC, name ASC
        """
    )
    suspend fun getActiveMedications(patientId: String): List<MedicationEntity>

    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    suspend fun getMedicationById(id: String): MedicationEntity?

    @Query("SELECT * FROM medications WHERE id IN (:ids)")
    suspend fun getMedicationsByIds(ids: List<String>): List<MedicationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(medication: MedicationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medications: List<MedicationEntity>)

    @Query(
        """
        UPDATE medications
        SET imageUri = :imageUri, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateMedicationImage(
        id: String,
        imageUri: String?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE medications
        SET imageUri = NULL, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun removeMedicationImage(
        id: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE medications
        SET isActive = 0, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun softDelete(id: String, updatedAt: Long)

    @Query(
        """
        UPDATE medications
        SET isActive = 0, imageUri = NULL, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun deactivateMedicationAndDeleteImage(
        id: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM medications WHERE id = :id AND name = :name")
    suspend fun deleteByIdAndName(
        id: String,
        name: String,
    ): Int

    @Query("UPDATE medications SET patientId = :newPatientId, updatedAt = :updatedAt WHERE patientId = :oldPatientId")
    suspend fun migratePatientId(
        oldPatientId: String,
        newPatientId: String,
        updatedAt: Long,
    ): Int

    @Query(
        """
        UPDATE medications
        SET patientId = :patientId
        WHERE TRIM(patientId) = ''
        """
    )
    suspend fun reassignBlankPatientIds(patientId: String)

    @Query("DELETE FROM medications")
    suspend fun deleteAll()
}
