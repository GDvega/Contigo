package com.cuidavoz.notificationchannel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant

object MedicationReminderScheduler {
  fun scheduleMedicationGroupReminder(
    context: Context,
    medicationGroupId: String,
    scheduleTimeIso: String,
    medicationNames: List<String>,
    repeatCount: Int
  ): Long {
    cancelGroup(context, medicationGroupId)
    val store = MedicationReminderStore(context)
    store.clearTaken(medicationGroupId)
    store.dismissNotifications(medicationGroupId)

    val initialRepeatIndex = 1
    val triggerAtMillis = parseTriggerAtMillis(scheduleTimeIso)
    val effectiveRepeatCount = repeatCount.coerceIn(1, MAX_REMINDER_ATTEMPTS)
    scheduleReminder(
      context = context,
      medicationGroupId = medicationGroupId,
      scheduleTime = scheduleTimeIso,
      medicationNames = medicationNames,
      repeatIndex = initialRepeatIndex,
      triggerAtMillis = triggerAtMillis,
      maxRepeatCount = effectiveRepeatCount
    )
    return triggerAtMillis
  }

  fun scheduleNextReminder(
    context: Context,
    medicationGroupId: String,
    scheduleTime: String,
    medicationNames: List<String>,
    repeatIndex: Int,
    maxRepeatCount: Int
  ): Long? {
    if (repeatIndex >= maxRepeatCount || repeatIndex >= MAX_REMINDER_ATTEMPTS) {
      return null
    }

    val nextRepeatIndex = repeatIndex + 1
    val triggerAtMillis = System.currentTimeMillis() + REMINDER_REPEAT_DELAY_MINUTES * 60 * 1000L
    scheduleReminder(
      context = context,
      medicationGroupId = medicationGroupId,
      scheduleTime = scheduleTime,
      medicationNames = medicationNames,
      repeatIndex = nextRepeatIndex,
      triggerAtMillis = triggerAtMillis,
      maxRepeatCount = maxRepeatCount
    )
    return triggerAtMillis
  }

  fun cancelGroup(context: Context, medicationGroupId: String) {
    cancelGroup(context, medicationGroupId, preserveTakenState = false)
  }

  fun cancelGroup(
    context: Context,
    medicationGroupId: String,
    preserveTakenState: Boolean
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val store = MedicationReminderStore(context)

    store.getPendingRequestCodes(medicationGroupId).forEach { requestCode ->
      val receiverIntent =
        Intent(context, MedicationReminderReceiver::class.java).apply {
          action = MEDICATION_REMINDER_ACTION
        }
      val pendingIntent =
        PendingIntent.getBroadcast(
          context,
          requestCode,
          receiverIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }

    store.dismissNotifications(medicationGroupId)
    if (preserveTakenState) {
      store.clearPendingState(medicationGroupId)
    } else {
      store.clearGroup(medicationGroupId)
    }
  }

  fun buildRequestCode(medicationGroupId: String, repeatIndex: Int): Int {
    return medicationGroupId.hashCode() * 31 + repeatIndex
  }

  fun cancelAll(context: Context) {
    val store = MedicationReminderStore(context)
    store.getRegisteredGroupIds().forEach { groupId ->
      cancelGroup(context, groupId)
    }
  }

  private fun scheduleReminder(
    context: Context,
    medicationGroupId: String,
    scheduleTime: String,
    medicationNames: List<String>,
    repeatIndex: Int,
    triggerAtMillis: Long,
    maxRepeatCount: Int
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val requestCode = buildRequestCode(medicationGroupId, repeatIndex)
    val store = MedicationReminderStore(context)

    if (store.isTaken(medicationGroupId) || store.getPendingRequestCodes(medicationGroupId).contains(requestCode)) {
      return
    }

    val receiverIntent =
      Intent(context, MedicationReminderReceiver::class.java).apply {
        action = MEDICATION_REMINDER_ACTION
        putExtra(EXTRA_MEDICATION_GROUP_ID, medicationGroupId)
        putExtra(EXTRA_SCHEDULE_TIME, scheduleTime)
        putStringArrayListExtra(EXTRA_MEDICATION_NAMES, ArrayList(medicationNames))
        putExtra(EXTRA_REPEAT_INDEX, repeatIndex)
        putExtra("maxRepeatCount", maxRepeatCount)
      }

    val pendingIntent =
      PendingIntent.getBroadcast(
        context,
        requestCode,
        receiverIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )

    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )

      Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
        alarmManager.setExact(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )

      else ->
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent
        )
    }
    store.addPendingRequestCode(medicationGroupId, requestCode)
  }

  private fun parseTriggerAtMillis(scheduleTimeIso: String): Long {
    val parsed = Instant.parse(scheduleTimeIso).toEpochMilli()
    return maxOf(parsed, System.currentTimeMillis() + 1_000L)
  }
}
