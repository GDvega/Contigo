package com.cuidavoz.mobile.reminders

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cuidavoz.mobile.CuidaVozApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MedicationReminderVoiceService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appContainer
        get() = (application as CuidaVozApp).appContainer

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(FOREGROUND_ID, foregroundNotification())
        val payload = intent?.toReminderPayload()
        if (payload == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        serviceScope.launch {
            val reminder = payload.reminderId?.let { appContainer.medicationReminderRepository.getReminderById(it) }
            if (reminder == null || reminder.status != "PENDING") {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
                return@launch
            }
            val medications = appContainer.medicationRepository.getMedicationsByIds(payload.medicationIds)
            val patientName = appContainer.patientRepository.getCurrentPatient()?.fullName?.substringBefore(" ") ?: "María"
            val message = MedicationReminderMessageFactory.build(patientName, payload, medications)
            val repeatCount = appContainer.settingsRepository.getVoicePreferences().voiceRepeatCount
            appContainer.textToSpeechManager.speakRepeated(message.speech, repeatCount) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun foregroundNotification(): Notification {
        return NotificationCompat.Builder(this, MEDICATION_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("CuidaVoz está hablando")
            .setContentText("CuidaVoz está leyendo el recordatorio de la pastilla.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private companion object {
        const val FOREGROUND_ID = 7101
    }
}
