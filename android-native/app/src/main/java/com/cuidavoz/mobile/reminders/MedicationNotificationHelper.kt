package com.cuidavoz.mobile.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class ReminderNotificationContent(
    val title: String,
    val body: String,
    val bigText: String,
)

class MedicationNotificationHelper(
    private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun showMedicationReminder(
        payload: ReminderPayload,
        content: ReminderNotificationContent,
        soundEnabled: Boolean,
        vibrationEnabled: Boolean,
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Permiso de notificaciones no concedido. No se muestra aviso.")
            return
        }

        val openIntent = ReminderActivity.createIntent(context, payload)
        val takenIntent = actionPendingIntent(ACTION_MARK_TAKEN, payload, "taken")
        val snoozeIntent = actionPendingIntent(ACTION_SNOOZE_REMINDER, payload, "snooze")
        val helpIntent = actionPendingIntent(ACTION_REQUEST_HELP, payload, "help")

        val builder = NotificationCompat.Builder(context, MEDICATION_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    reminderRequestCode(payload.reminderGroupId, payload.attemptNumber, "open"),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(0, if (payload.medicationNames.size > 1) "Ya tomé todas" else "Ya tomé", takenIntent)
            .addAction(0, "Después", snoozeIntent)
            .addAction(0, "Pedir ayuda", helpIntent)
            .setOngoing(false)

        if (soundEnabled) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSilent(true)
        }

        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 800, 400, 800))
        } else {
            builder.setVibrate(longArrayOf(0L))
        }

        postNotificationSafely(payload, builder.build())
    }

    fun showConfirmationNotification(message: String, payload: ReminderPayload) {
        val notification = NotificationCompat.Builder(context, MEDICATION_REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle("CuidaVoz")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        postNotificationSafely(payload, notification)
    }

    fun cancelMedicationReminderNotification(
        patientId: String,
        scheduleTime: String,
    ) {
        notificationManager.cancel(notificationId(patientId, scheduleTime))
        Log.d(TAG, "Notificacion cancelada para $scheduleTime")
    }

    fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotificationSafely(
        payload: ReminderPayload,
        notification: android.app.Notification,
    ) {
        runCatching {
            notificationManager.notify(notificationId(payload.patientId, payload.scheduleTime), notification)
            Log.d(TAG, "Notificacion mostrada para ${payload.scheduleTime}")
        }.onFailure { error ->
            if (error is SecurityException) {
                Log.w(TAG, "No se pudo mostrar la notificacion por falta de permiso.")
            } else {
                throw error
            }
        }
    }

    private fun actionPendingIntent(
        action: String,
        payload: ReminderPayload,
        suffix: String,
    ): PendingIntent {
        val intent = Intent(context, MedicationNotificationActionReceiver::class.java).apply {
            this.action = action
            putExtras(payload.toReceiverIntent().extras ?: android.os.Bundle())
        }
        return PendingIntent.getBroadcast(
            context,
            reminderRequestCode(payload.reminderGroupId, payload.attemptNumber, suffix),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(
        patientId: String,
        scheduleTime: String,
    ): Int = "${patientId}_$scheduleTime".hashCode()

    private companion object {
        const val TAG = "[CuidaVoz][Notification]"
    }
}
