package com.cuidavoz.mobile.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.cuidavoz.mobile.data.model.MedicationReminderEntity
import com.cuidavoz.mobile.data.repository.MedicationReminderRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.util.ContigoLog
import java.time.LocalDate
import java.time.LocalDateTime

class MedicationAlarmScheduler(
    private val context: Context,
    private val medicationRepository: MedicationRepository,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    suspend fun scheduleAllMedicationAlarms(patientId: String) {
        val prefs = settingsRepository.getReminderPreferences()
        if (!prefs.remindersEnabled) {
            cancelAllMedicationAlarms(patientId)
            return
        }

        cancelAllMedicationAlarms(patientId)
        medicationReminderRepository.deleteOldReminders(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)

        val medications = medicationRepository.getActiveMedications(patientId)
        val plans = ReminderAttemptPlanner.groupUpcomingMedications(
            patientId = patientId,
            medications = medications,
            fromDateTime = LocalDateTime.now(),
        )

        plans.forEach { plan ->
            val reminder = MedicationReminderEntity(
                id = com.cuidavoz.mobile.util.createLocalId("medication_reminder"),
                patientId = patientId,
                reminderGroupId = plan.reminderGroupId,
                scheduleTime = plan.scheduleTime,
                targetDate = plan.targetDate.toString(),
                medicationIds = plan.medicationIds.encodeReminderList(),
                medicationNames = plan.medicationNames.encodeReminderList(),
                alarmRequestCode = reminderRequestCode(plan.reminderGroupId, 1, "show"),
                attemptNumber = 1,
                maxAttempts = prefs.maxRepeatCount,
                repeatEveryMinutes = prefs.repeatIntervalMinutes,
                scheduledAt = plan.scheduledAt,
                respondedAt = null,
                status = "PENDING",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            medicationReminderRepository.insertReminder(reminder)
            val payload = reminder.toPayload()
            setAlarm(
                triggerAt = reminder.scheduledAt,
                pendingIntent = servicePendingIntent(
                    action = ACTION_SHOW_REMINDER,
                    payload = payload,
                    requestCode = reminder.alarmRequestCode,
                ),
                showIntent = reminderShowIntent(payload),
            )
        }
    }

    suspend fun scheduleNextAttempt(reminder: MedicationReminderEntity): MedicationReminderEntity? {
        if (!ReminderAttemptPolicy.shouldScheduleNextAttempt(reminder.attemptNumber, reminder.maxAttempts, reminder.status)) {
            scheduleMissedCheck(reminder)
            return null
        }
        val next = reminder.copy(
            id = com.cuidavoz.mobile.util.createLocalId("medication_reminder"),
            alarmRequestCode = reminderRequestCode(reminder.reminderGroupId, reminder.attemptNumber + 1, "show"),
            attemptNumber = reminder.attemptNumber + 1,
            scheduledAt = ReminderAttemptPlanner.nextAttemptTime(reminder.scheduledAt, reminder.repeatEveryMinutes),
            status = "PENDING",
            respondedAt = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        medicationReminderRepository.insertReminder(next)
        val payload = next.toPayload()
        setAlarm(
            triggerAt = next.scheduledAt,
            pendingIntent = servicePendingIntent(
                action = ACTION_SHOW_REMINDER,
                payload = payload,
                requestCode = next.alarmRequestCode,
            ),
            showIntent = reminderShowIntent(payload),
        )
        return next
    }

    suspend fun scheduleSnoozedAttempt(reminder: MedicationReminderEntity): MedicationReminderEntity? {
        return scheduleNextAttempt(reminder)
    }

    suspend fun scheduleMissedCheck(reminder: MedicationReminderEntity) {
        val checkAt = ReminderAttemptPlanner.nextAttemptTime(reminder.scheduledAt, reminder.repeatEveryMinutes)
        val payload = reminder.toPayload()
        setAlarm(
            triggerAt = checkAt,
            pendingIntent = servicePendingIntent(
                action = ACTION_FINALIZE_MISSED,
                payload = payload,
                requestCode = reminderRequestCode(reminder.reminderGroupId, reminder.attemptNumber, "missed"),
            ),
            showIntent = reminderShowIntent(payload),
        )
    }

    suspend fun cancelMedicationGroupAlarms(reminderGroupId: String) {
        medicationReminderRepository.getRemindersByGroupId(reminderGroupId).forEach { reminder ->
            cancelPendingIntents(reminder)
        }
        // Force cleanup of any potential orphan attempts by request code convention
        for (i in 1..6) {
            cancelPendingIntentByCode(reminderRequestCode(reminderGroupId, i, "show"), ACTION_SHOW_REMINDER)
            cancelPendingIntentByCode(reminderRequestCode(reminderGroupId, i, "missed"), ACTION_FINALIZE_MISSED)
        }
        medicationReminderRepository.updateGroupStatus(
            reminderGroupId = reminderGroupId,
            status = "CANCELLED",
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun cancelMedicationGroupReminder(
        patientId: String,
        scheduleTime: String,
    ) {
        val targetDate = LocalDate.now().toString()
        val reminders = medicationReminderRepository.getRemindersForSchedule(patientId, scheduleTime, targetDate)
        reminders.forEach { cancelPendingIntents(it) }
        reminders.firstOrNull()?.let {
            cancelMedicationGroupAlarms(it.reminderGroupId)
        }
    }

    suspend fun cancelAllMedicationAlarms(patientId: String) {
        medicationReminderRepository.getScheduledReminders(patientId).forEach(::cancelPendingIntents)
        medicationReminderRepository.cancelAllReminders(patientId)
    }

    suspend fun rescheduleAfterBoot(patientId: String) {
        scheduleAllMedicationAlarms(patientId)
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun exactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            null
        }
    }

    private fun setAlarm(
        triggerAt: Long,
        pendingIntent: PendingIntent,
        showIntent: PendingIntent,
    ) {
        if (canScheduleExactAlarms()) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                pendingIntent,
            )
        } else {
            ContigoLog.w(
                TAG,
                "Sin permiso de alarma exacta: usando respaldo inexacto (setAndAllowWhileIdle); el recordatorio podría retrasarse."
            )
            // Respaldo si no hay permiso exacto (no debería ocurrir con USE_EXACT_ALARM).
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun reminderShowIntent(payload: ReminderPayload): PendingIntent {
        val intent = ReminderActivity.createIntent(context, payload)
        return PendingIntent.getActivity(
            context,
            reminderRequestCode(payload.reminderGroupId, payload.attemptNumber, "showclock"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun servicePendingIntent(
        action: String,
        payload: ReminderPayload,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            this.action = action
            putExtras(payload.toReceiverIntent().extras ?: android.os.Bundle())
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPendingIntents(reminder: MedicationReminderEntity) {
        cancelPendingIntentByCode(reminder.alarmRequestCode, ACTION_SHOW_REMINDER)
        cancelPendingIntentByCode(
            reminderRequestCode(reminder.reminderGroupId, reminder.attemptNumber, "missed"),
            ACTION_FINALIZE_MISSED,
        )
    }

    private fun cancelPendingIntentByCode(
        requestCode: Int,
        action: String,
    ) {
        val intent = Intent(context, MedicationAlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun MedicationReminderEntity.toPayload(): ReminderPayload {
        return ReminderPayload(
            reminderId = id,
            reminderGroupId = reminderGroupId,
            patientId = patientId,
            scheduleTime = scheduleTime,
            targetDate = targetDate,
            scheduledAt = scheduledAt,
            medicationIds = medicationIds.decodeReminderList(),
            medicationNames = medicationNames.decodeReminderList(),
            attemptNumber = attemptNumber,
            maxAttempts = maxAttempts,
            repeatEveryMinutes = repeatEveryMinutes,
            requiresConfirmation = false,
        )
    }

    private companion object {
        const val TAG = "[Contigo][AlarmScheduler]"
    }
}
