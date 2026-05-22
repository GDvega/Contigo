package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthSettingsDao {
    @Query("SELECT * FROM health_settings WHERE patientId = :patientId LIMIT 1")
    fun observeSettings(patientId: String): Flow<HealthSettingsEntity?>

    @Query("SELECT * FROM health_settings WHERE patientId = :patientId LIMIT 1")
    suspend fun getSettings(patientId: String): HealthSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: HealthSettingsEntity)

    @Query(
        """
        UPDATE health_settings
        SET patientId = :patientId
        WHERE TRIM(patientId) = ''
        """
    )
    suspend fun reassignBlankPatientIds(patientId: String)

    @Query("DELETE FROM health_settings")
    suspend fun deleteAll()
}
