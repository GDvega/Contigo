package com.cuidavoz.notificationchannel

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class MedicationReminderChannelModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("CuidaVozNotificationChannel")

    OnCreate {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return@OnCreate
      }

      val context = appContext.reactContext?.applicationContext ?: return@OnCreate
      val notificationManager =
        context.getSystemService(NotificationManager::class.java) ?: return@OnCreate

      val channel = NotificationChannel(
        MEDICATION_REMINDER_CHANNEL_ID,
        MEDICATION_REMINDER_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        enableVibration(true)
        vibrationPattern = longArrayOf(0L, 800L, 400L, 800L)
        setSound(
          RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
      }

      notificationManager.createNotificationChannel(channel)
    }

    AsyncFunction(
      "scheduleMedicationGroupReminder"
    ) { medicationGroupId: String, scheduleTimeIso: String, medicationNames: List<String>, repeatCount: Int ->
      val context = appContext.reactContext?.applicationContext
        ?: throw IllegalStateException("React context not available")

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
          ?: throw IllegalStateException("Alarm manager not available")

        if (!alarmManager.canScheduleExactAlarms()) {
          throw IllegalStateException("Exact alarms are not allowed on this device")
        }
      }

      val triggerAtMillis = MedicationReminderScheduler.scheduleMedicationGroupReminder(
        context = context,
        medicationGroupId = medicationGroupId,
        scheduleTimeIso = scheduleTimeIso,
        medicationNames = medicationNames,
        repeatCount = repeatCount
      )

      mapOf(
        "scheduled" to true,
        "medicationGroupId" to medicationGroupId,
        "triggerAtMillis" to triggerAtMillis
      )
    }

    AsyncFunction("confirmMedicationGroupTakenAsync") { medicationGroupId: String ->
      val context = appContext.reactContext?.applicationContext
        ?: throw IllegalStateException("React context not available")

      val store = MedicationReminderStore(context)
      store.markTaken(medicationGroupId)
      MedicationReminderScheduler.cancelGroup(
        context,
        medicationGroupId,
        preserveTakenState = true
      )

      mapOf(
        "confirmed" to true,
        "medicationGroupId" to medicationGroupId
      )
    }

    AsyncFunction("cancelMedicationGroupReminder") { medicationGroupId: String ->
      val context = appContext.reactContext?.applicationContext
        ?: throw IllegalStateException("React context not available")

      MedicationReminderScheduler.cancelGroup(context, medicationGroupId)

      mapOf(
        "cancelled" to true,
        "medicationGroupId" to medicationGroupId
      )
    }

    AsyncFunction("cancelAllMedicationReminders") {
      val context = appContext.reactContext?.applicationContext
        ?: throw IllegalStateException("React context not available")

      MedicationReminderScheduler.cancelAll(context)

      mapOf(
        "cancelled" to true
      )
    }
  }
}
