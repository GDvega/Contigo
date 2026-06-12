package com.cuidavoz.mobile.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import com.cuidavoz.mobile.util.ContigoLog

object MedicationNotificationChannels {
    private const val TAG = "[Contigo][Notification]"

    fun createAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(reminderChannel())
        manager.createNotificationChannel(confirmationChannel())
        manager.createNotificationChannel(voiceChannel())
        ContigoLog.d(TAG, "Canales de notificación creados o actualizados")
    }

    private fun reminderChannel(): NotificationChannel {
        return NotificationChannel(
            MEDICATION_REMINDER_CHANNEL_ID,
            "Recordatorios de pastillas",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Te avisa cuando sea hora de tomar tus medicamentos."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 400, 800)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
    }

    private fun confirmationChannel(): NotificationChannel {
        return NotificationChannel(
            MEDICATION_CONFIRMATION_CHANNEL_ID,
            "Confirmaciones",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Respuestas breves cuando registras una toma o pospones un recordatorio."
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
    }

    private fun voiceChannel(): NotificationChannel {
        return NotificationChannel(
            MEDICATION_VOICE_CHANNEL_ID,
            "Lectura en voz alta",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Indica que Contigo está leyendo un recordatorio en voz alta."
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(false)
        }
    }
}
