package com.cuidavoz.mobile.reminders

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log

object MedicationNotificationChannel {
    private const val TAG = "[CuidaVoz][Notification]"

    fun create(context: Context) {
        val channel = NotificationChannel(
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
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
        Log.d(TAG, "Canal de recordatorios creado o actualizado")
    }
}
