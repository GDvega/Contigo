package com.cuidavoz.mobile.data.repository

import com.cuidavoz.mobile.data.local.MedicationReminderDao
import com.cuidavoz.mobile.data.model.MedicationReminderEntity

class MedicationReminderRepository(
    private val medicationReminderDao: MedicationReminderDao,
) {
    suspend fun insertReminder(reminder: MedicationReminderEntity) =
        medicationReminderDao.insertReminder(reminder)

    suspend fun getScheduledReminders(patientId: String) =
        medicationReminderDao.getScheduledReminders(patientId)

    suspend fun getRemindersByGroupId(reminderGroupId: String) =
        medicationReminderDao.getRemindersByGroupId(reminderGroupId)

    suspend fun getReminder(
        reminderGroupId: String,
        attemptNumber: Int,
    ) = medicationReminderDao.getReminder(reminderGroupId, attemptNumber)

    suspend fun getReminderById(id: String) = medicationReminderDao.getReminderById(id)

    suspend fun updateReminderStatus(
        id: String,
        status: String,
        respondedAt: Long? = null,
        updatedAt: Long = System.currentTimeMillis(),
    ) = medicationReminderDao.updateReminderStatus(id, status, respondedAt, updatedAt)

    suspend fun updateGroupStatus(
        reminderGroupId: String,
        status: String,
        respondedAt: Long? = null,
        updatedAt: Long = System.currentTimeMillis(),
    ) = medicationReminderDao.updateGroupStatus(reminderGroupId, status, respondedAt, updatedAt)

    suspend fun cancelAllReminders(patientId: String) =
        medicationReminderDao.cancelAllReminders(patientId, System.currentTimeMillis())

    suspend fun getRemindersForSchedule(
        patientId: String,
        scheduleTime: String,
        targetDate: String,
    ) = medicationReminderDao.getRemindersForSchedule(patientId, scheduleTime, targetDate)

    suspend fun deleteOldReminders(cutoffTime: Long) =
        medicationReminderDao.deleteOldReminders(cutoffTime)
}
