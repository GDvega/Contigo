package com.cuidavoz.mobile.reminders

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cuidavoz.mobile.ContigoApp
import com.cuidavoz.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MedicationReminderVoiceService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appContainer
        get() = (application as ContigoApp).appContainer

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
            val patientName = appContainer.patientRepository.getCurrentPatient()?.fullName?.substringBefore(" ") ?: "paciente"
            val message = MedicationReminderMessageFactory.build(patientName, payload, medications)
            val repeatCount = appContainer.settingsRepository.getVoicePreferences().voiceRepeatCount
            appContainer.textToSpeechManager.configureForMedicationReminders()
            appContainer.textToSpeechManager.speakRepeated(message.speech, repeatCount) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { appContainer.textToSpeechManager.stop() }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun foregroundNotification(): Notification {
        return NotificationCompat.Builder(this, MEDICATION_VOICE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_contigo)
            .setColor(ContextCompat.getColor(this, R.color.contigo_primary))
            .setContentTitle("Contigo está hablando")
            .setContentText("Contigo está leyendo el recordatorio de la pastilla.")
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        private const val FOREGROUND_ID = 7101
        
        /** Detiene la locución en curso y apaga el servicio de voz. */
        fun stop(context: android.content.Context) {
            context.stopService(android.content.Intent(context, MedicationReminderVoiceService::class.java))
        }
    }
}
