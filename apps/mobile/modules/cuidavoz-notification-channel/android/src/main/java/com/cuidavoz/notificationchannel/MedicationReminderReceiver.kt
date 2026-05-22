package com.cuidavoz.notificationchannel

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class MedicationReminderReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != MEDICATION_REMINDER_ACTION) {
      return
    }

    val medicationGroupId = intent.getStringExtra(EXTRA_MEDICATION_GROUP_ID) ?: return
    val store = MedicationReminderStore(context)

    if (store.isTaken(medicationGroupId)) {
      MedicationReminderScheduler.cancelGroup(context, medicationGroupId)
      return
    }

    val scheduleTime = intent.getStringExtra(EXTRA_SCHEDULE_TIME) ?: "--:--"
    val medicationNames = intent.getStringArrayListExtra(EXTRA_MEDICATION_NAMES).orEmpty()
    val repeatIndex = intent.getIntExtra(EXTRA_REPEAT_INDEX, 1).coerceAtLeast(1)
    val maxRepeatCount = intent.getIntExtra("maxRepeatCount", MAX_REMINDER_ATTEMPTS)
      .coerceIn(1, MAX_REMINDER_ATTEMPTS)
    val requestCode = MedicationReminderScheduler.buildRequestCode(medicationGroupId, repeatIndex)

    store.removePendingRequestCode(medicationGroupId, requestCode)

    val title =
      if (medicationNames.size <= 1) {
        "Hora de tomar tu pastilla"
      } else {
        "Hora de tomar tus pastillas"
      }

    val body =
      if (medicationNames.isEmpty()) {
        "Horario $scheduleTime. Abre CuidaVoz para revisar el recordatorio."
      } else {
        "Horario $scheduleTime: ${medicationNames.joinToString(", ")}."
      }

    val launchIntent =
      context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      }

    val contentIntent =
      launchIntent?.let {
        PendingIntent.getActivity(
          context,
          medicationGroupId.hashCode(),
          it,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }

    val notification =
      NotificationCompat.Builder(context, MEDICATION_REMINDER_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setAutoCancel(true)
        .setOnlyAlertOnce(false)
        .setContentIntent(contentIntent)
        .build()

    val notificationId = medicationGroupId.hashCode() + repeatIndex
    NotificationManagerCompat.from(context).notify(notificationId, notification)
    store.addNotificationId(medicationGroupId, notificationId)

    if (!store.isTaken(medicationGroupId) && repeatIndex < maxRepeatCount) {
      MedicationReminderScheduler.scheduleNextReminder(
        context = context,
        medicationGroupId = medicationGroupId,
        scheduleTime = scheduleTime,
        medicationNames = medicationNames,
        repeatIndex = repeatIndex,
        maxRepeatCount = maxRepeatCount
      )
    }
  }
}
