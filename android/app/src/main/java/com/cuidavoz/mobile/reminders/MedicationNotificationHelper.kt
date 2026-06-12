package com.cuidavoz.mobile.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.cuidavoz.mobile.R
import com.cuidavoz.mobile.util.ContigoLog

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
        if (!canPostNotifications()) {
            ContigoLog.w(TAG, "Permiso de notificaciones no concedido. No se muestra aviso.")
            return
        }

        val openIntent = ReminderActivity.createIntent(context, payload)
        val takenIntent = actionPendingIntent(ACTION_MARK_TAKEN, payload, "taken")
        val snoozeIntent = actionPendingIntent(ACTION_SNOOZE_REMINDER, payload, "snooze")
        val takenLabel = if (payload.medicationNames.size > 1) "Ya tomé todas" else "Ya tomé"

        val assistant = Person.Builder()
            .setName("Contigo")
            .setImportant(true)
            .build()
        val messagingStyle = NotificationCompat.MessagingStyle(assistant)
            .setConversationTitle(content.title)
            .addMessage(content.bigText, System.currentTimeMillis(), assistant)

        val builder = NotificationCompat.Builder(context, MEDICATION_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_contigo)
            .setColor(ContextCompat.getColor(context, R.color.contigo_primary))
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(messagingStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setGroup(MEDICATION_REMINDER_GROUP_KEY)
            .setOnlyAlertOnce(payload.attemptNumber > 1)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    reminderRequestCode(payload.reminderGroupId, payload.attemptNumber, "open"),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .addAction(
                NotificationCompat.Action.Builder(0, takenLabel, takenIntent)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                    .build(),
            )
            .addAction(0, "Posponerlo", snoozeIntent)
            .setOngoing(false)

        if (payload.maxAttempts > 1) {
            builder.setSubText("Intento ${payload.attemptNumber} de ${payload.maxAttempts}")
        }

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

        postNotificationSafely(
            notificationId = reminderNotificationId(payload.patientId, payload.scheduleTime),
            notification = builder.build(),
            logContext = payload.scheduleTime,
        )
        postGroupSummary(payload)
    }

    fun showConfirmationNotification(message: String, payload: ReminderPayload) {
        if (!canPostNotifications()) return

        val notification = NotificationCompat.Builder(context, MEDICATION_CONFIRMATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_contigo)
            .setColor(ContextCompat.getColor(context, R.color.contigo_primary))
            .setContentTitle("Contigo")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setTimeoutAfter(CONFIRMATION_TIMEOUT_MS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()

        postNotificationSafely(
            notificationId = confirmationNotificationId(payload.patientId, payload.scheduleTime),
            notification = notification,
            logContext = "confirmación",
        )
    }

    fun cancelMedicationReminderNotification(
        patientId: String,
        scheduleTime: String,
    ) {
        notificationManager.cancel(reminderNotificationId(patientId, scheduleTime))
        notificationManager.cancel(summaryNotificationId(patientId))
        ContigoLog.d(TAG, "Notificacion cancelada para $scheduleTime")
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
    private fun postGroupSummary(payload: ReminderPayload) {
        val medicationSummary = payload.medicationNames.joinToString(", ")
        val summary = NotificationCompat.Builder(context, MEDICATION_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_contigo)
            .setColor(ContextCompat.getColor(context, R.color.contigo_primary))
            .setContentTitle("Contigo: Recordatorios de hoy")
            .setContentText("Tienes pastillas pendientes")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setBigContentTitle("Resumen de recordatorios")
                    .setSummaryText("Contigo")
            )
            .setGroup(MEDICATION_REMINDER_GROUP_KEY)
            .setGroupSummary(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()

        runCatching {
            notificationManager.notify(summaryNotificationId(payload.patientId), summary)
        }.onFailure { error ->
            if (error is SecurityException) {
                ContigoLog.w(TAG, "No se pudo mostrar el resumen agrupado por falta de permiso.")
            } else {
                throw error
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotificationSafely(
        notificationId: Int,
        notification: android.app.Notification,
        logContext: String,
    ) {
        runCatching {
            notificationManager.notify(notificationId, notification)
            ContigoLog.d(TAG, "Notificacion mostrada para $logContext")
        }.onFailure { error ->
            if (error is SecurityException) {
                ContigoLog.w(TAG, "No se pudo mostrar la notificacion por falta de permiso.")
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

    private fun reminderNotificationId(
        patientId: String,
        scheduleTime: String,
    ): Int = "${patientId}_$scheduleTime".hashCode()

    private fun confirmationNotificationId(
        patientId: String,
        scheduleTime: String,
    ): Int = "confirm_${patientId}_$scheduleTime".hashCode()

    private fun summaryNotificationId(patientId: String): Int = "summary_$patientId".hashCode()

    private companion object {
        const val TAG = "[Contigo][Notification]"
    }
}
