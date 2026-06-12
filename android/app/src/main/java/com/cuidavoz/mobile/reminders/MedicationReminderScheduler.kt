package com.cuidavoz.mobile.reminders

import android.content.Context
import android.content.Intent
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.MedicationLogRepository
import com.cuidavoz.mobile.data.repository.MedicationReminderRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.domain.MedicationDoseStatus
import com.cuidavoz.mobile.domain.MedicationGrouping
import com.cuidavoz.mobile.domain.MedicationOutcomeResult
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import java.time.LocalDate

class MedicationReminderScheduler(
    context: Context,
    private val medicationRepository: MedicationRepository,
    private val medicationLogRepository: MedicationLogRepository,
    private val medicationReminderRepository: MedicationReminderRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: MedicationNotificationHelper,
    private val dailyStatusRepository: DailyStatusRepository,
    private val actionHandler: MedicationReminderActionHandler,
) {
    private val alarmScheduler = MedicationAlarmScheduler(
        context = context,
        medicationRepository = medicationRepository,
        medicationReminderRepository = medicationReminderRepository,
        settingsRepository = settingsRepository,
    )

    suspend fun scheduleAllMedicationReminders(patientId: String) {
        alarmScheduler.scheduleAllMedicationAlarms(patientId)
    }

    suspend fun cancelMedicationGroupReminder(
        patientId: String,
        scheduleTime: String,
    ) {
        alarmScheduler.cancelMedicationGroupReminder(patientId, scheduleTime)
        notificationHelper.cancelMedicationReminderNotification(patientId, scheduleTime)
    }

    suspend fun cancelReminderGroup(reminderGroupId: String, scheduleTime: String) {
        alarmScheduler.cancelMedicationGroupAlarms(reminderGroupId)
        notificationHelper.cancelMedicationReminderNotification(DEFAULT_PATIENT_ID, scheduleTime)
    }

    suspend fun cancelAllMedicationReminders(patientId: String) {
        alarmScheduler.cancelAllMedicationAlarms(patientId)
    }

    suspend fun rescheduleAllAfterBoot() {
        alarmScheduler.rescheduleAfterBoot(DEFAULT_PATIENT_ID)
    }

    suspend fun isMedicationGroupPendingToday(
        patientId: String,
        scheduleTime: String,
    ): Boolean {
        val medications = medicationRepository.getActiveMedications(patientId)
        val todayLogs = medicationLogRepository.getTodayMedicationLogs(patientId)
        return MedicationGrouping.getPendingMedicationsForTime(
            scheduleTime = scheduleTime,
            medications = medications,
            medicationLogs = todayLogs,
            date = LocalDate.now(),
        ).isNotEmpty()
    }

    suspend fun ensureNextRepeatScheduled(payload: ReminderPayload) {
        val reminder = medicationReminderRepository.getReminderById(payload.reminderId.orEmpty()) ?: return
        alarmScheduler.scheduleNextAttempt(reminder)
    }

    suspend fun markReminderFired(reminderId: String?) {
        actionHandler.markFired(reminderId)
    }

    suspend fun markReminderTaken(payload: ReminderPayload): Boolean {
        return actionHandler.markTaken(payload)
    }

    suspend fun recordReminderOutcomes(
        payload: ReminderPayload,
        outcomes: List<MedicationDoseOutcome>,
    ): MedicationOutcomeResult {
        return actionHandler.recordOutcomes(payload, outcomes)
    }

    suspend fun markReminderSnoozed(payload: ReminderPayload) {
        val reminder = medicationReminderRepository.getReminderById(payload.reminderId.orEmpty())
        actionHandler.markSnoozed(payload.reminderId)
        reminder?.let {
            alarmScheduler.scheduleSnoozedAttempt(reminder)
        }
    }

    suspend fun finalizeReminderAsMissed(payload: ReminderPayload): Boolean {
        return actionHandler.finalizeMissed(payload)
    }

    suspend fun requestHelp(payload: ReminderPayload): String? {
        return actionHandler.requestHelp(payload)
    }

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()

    fun exactAlarmSettingsIntent(): Intent? = alarmScheduler.exactAlarmSettingsIntent()
}
