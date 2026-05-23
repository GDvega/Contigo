package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyContactDao {
    @Query("SELECT * FROM family_contacts WHERE patientId = :patientId LIMIT 1")
    fun observePrimaryContact(patientId: String): Flow<FamilyContactEntity?>

    @Query("SELECT * FROM family_contacts WHERE patientId = :patientId LIMIT 1")
    suspend fun getPrimaryContact(patientId: String): FamilyContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: FamilyContactEntity)

    @androidx.room.Update
    suspend fun updateContact(contact: FamilyContactEntity)

    @Query(
        """
        UPDATE family_contacts
        SET patientId = :patientId
        WHERE TRIM(patientId) = ''
        """
    )
    suspend fun reassignBlankPatientIds(patientId: String)

    @Query("DELETE FROM family_contacts")
    suspend fun deleteAll()
}
