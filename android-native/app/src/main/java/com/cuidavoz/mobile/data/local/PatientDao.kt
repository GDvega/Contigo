package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {
    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    fun observePatient(patientId: String): Flow<PatientEntity?>

    @Query("SELECT * FROM patients WHERE id = :patientId LIMIT 1")
    suspend fun getPatient(patientId: String): PatientEntity?

    @Query("SELECT * FROM patients ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentPatient(): PatientEntity?

    fun observeCurrentPatient(): Flow<PatientEntity?> = observePatient(DEFAULT_PATIENT_ID)

    suspend fun getCurrentPatient(): PatientEntity? = getPatient(DEFAULT_PATIENT_ID) ?: getMostRecentPatient()

    @Query("SELECT COUNT(*) FROM patients")
    suspend fun countPatients(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(patient: PatientEntity)

    @Query("DELETE FROM patients")
    suspend fun deleteAll()
}
