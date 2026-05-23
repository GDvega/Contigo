package com.cuidavoz.mobile.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.cuidavoz.mobile.CuidaVozApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val payload = intent.toReminderPayload() ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContainer = (context.applicationContext as CuidaVozApp).appContainer
                if (intent.action == ACTION_FINALIZE_MISSED) {
                    appContainer.reminderScheduler.finalizeReminderAsMissed(payload)
                    return@launch
                }
                val reminder = payload.reminderId?.let { appContainer.medicationReminderRepository.getReminderById(it) } ?: return@launch
                if (reminder.status != "PENDING") {
                    return@launch
                }
                val medications = appContainer.medicationRepository.getMedicationsByIds(payload.medicationIds)
                val patientName = appContainer.patientRepository.getCurrentPatient()?.fullName?.substringBefore(" ") ?: "María"
                val message = MedicationReminderMessageFactory.build(patientName, payload, medications)
                val activeToday = appContainer.reminderScheduler.isMedicationGroupPendingToday(payload.patientId, payload.scheduleTime)
                if (!activeToday && payload.targetDate == java.time.LocalDate.now().toString()) {
                    appContainer.reminderScheduler.cancelReminderGroup(payload.reminderGroupId, payload.scheduleTime)
                    return@launch
                }
                val reminderPrefs = appContainer.settingsRepository.getReminderPreferences()
                val voicePrefs = appContainer.settingsRepository.getVoicePreferences()
                appContainer.notificationHelper.showMedicationReminder(
                    payload = payload,
                    content = ReminderNotificationContent(
                        title = message.title,
                        body = message.body,
                        bigText = message.bigText,
                    ),
                    soundEnabled = reminderPrefs.soundEnabled,
                    vibrationEnabled = reminderPrefs.vibrationEnabled,
                )
                appContainer.reminderScheduler.markReminderFired(payload.reminderId)
                if (voicePrefs.voiceReminderEnabled) {
                    val voiceIntent = Intent(context, MedicationReminderVoiceService::class.java).apply {
                        putExtras(payload.toReceiverIntent().extras ?: android.os.Bundle())
                    }
                    ContextCompat.startForegroundService(context, voiceIntent)
                }
                appContainer.reminderScheduler.ensureNextRepeatScheduled(payload)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
