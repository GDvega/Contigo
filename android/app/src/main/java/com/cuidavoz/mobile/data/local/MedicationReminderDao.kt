package com.cuidavoz.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuidavoz.mobile.data.model.MedicationReminderEntity

@Dao
interface MedicationReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminderEntity)

    @Query(
        """
        SELECT * FROM medication_reminders
        WHERE patientId = :patientId
          AND status = 'PENDING'
        ORDER BY scheduledAt ASC
        """
    )
    suspend fun getScheduledReminders(patientId: String): List<MedicationReminderEntity>

    @Query(
        """
        SELECT * FROM medication_reminders
        WHERE reminderGroupId = :reminderGroupId
        ORDER BY attemptNumber ASC, scheduledAt ASC
        """
    )
    suspend fun getRemindersByGroupId(reminderGroupId: String): List<MedicationReminderEntity>

    @Query(
        """
        SELECT * FROM medication_reminders
        WHERE reminderGroupId = :reminderGroupId
          AND attemptNumber = :attemptNumber
        LIMIT 1
        """
    )
    suspend fun getReminder(
        reminderGroupId: String,
        attemptNumber: Int,
    ): MedicationReminderEntity?

    @Query("SELECT * FROM medication_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): MedicationReminderEntity?

    @Query(
        """
        UPDATE medication_reminders
        SET status = :status,
            respondedAt = :respondedAt,
            updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateReminderStatus(
        id: String,
        status: String,
        respondedAt: Long?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE medication_reminders
        SET status = :status,
            respondedAt = COALESCE(respondedAt, :respondedAt),
            updatedAt = :updatedAt
        WHERE reminderGroupId = :reminderGroupId
          AND status = 'PENDING'
        """
    )
    suspend fun updateGroupStatus(
        reminderGroupId: String,
        status: String,
        respondedAt: Long?,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE medication_reminders
        SET status = 'CANCELLED',
            updatedAt = :updatedAt
        WHERE patientId = :patientId
          AND status = 'PENDING'
        """
    )
    suspend fun cancelAllReminders(patientId: String, updatedAt: Long)

    @Query(
        """
        SELECT * FROM medication_reminders
        WHERE patientId = :patientId
          AND scheduleTime = :scheduleTime
          AND targetDate = :targetDate
        ORDER BY attemptNumber ASC, scheduledAt ASC
        """
    )
    suspend fun getRemindersForSchedule(
        patientId: String,
        scheduleTime: String,
        targetDate: String,
    ): List<MedicationReminderEntity>

    @Query(
        """
        DELETE FROM medication_reminders
        WHERE scheduledAt < :cutoffTime
          AND status != 'SCHEDULED'
        """
    )
    suspend fun deleteOldReminders(cutoffTime: Long)

    @Query(
        """
        UPDATE medication_reminders
        SET patientId = :patientId
        WHERE TRIM(patientId) = ''
        """
    )
    suspend fun reassignBlankPatientIds(patientId: String)

    @Query(
        """
        UPDATE medication_reminders
        SET patientId = :newPatientId, updatedAt = :updatedAt
        WHERE patientId = :oldPatientId
        """
    )
    suspend fun migratePatientId(
        oldPatientId: String,
        newPatientId: String,
        updatedAt: Long,
    ): Int

    @Query("DELETE FROM medication_reminders")
    suspend fun deleteAll()
}
