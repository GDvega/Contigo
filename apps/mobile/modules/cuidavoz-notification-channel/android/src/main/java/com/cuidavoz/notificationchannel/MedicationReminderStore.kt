package com.cuidavoz.notificationchannel

import android.content.Context
import androidx.core.app.NotificationManagerCompat

private const val STORE_NAME = "cuidavoz_medication_reminders"
private const val KEY_PENDING_CODES_PREFIX = "pending_codes:"
private const val KEY_TAKEN_PREFIX = "taken:"
private const val KEY_NOTIFICATION_IDS_PREFIX = "notification_ids:"

class MedicationReminderStore(context: Context) {
  private val appContext = context.applicationContext
  private val preferences = appContext.getSharedPreferences(STORE_NAME, Context.MODE_PRIVATE)

  fun isTaken(medicationGroupId: String): Boolean {
    return preferences.getBoolean(buildTakenKey(medicationGroupId), false)
  }

  fun markTaken(medicationGroupId: String) {
    preferences.edit()
      .putBoolean(buildTakenKey(medicationGroupId), true)
      .apply()
  }

  fun clearTaken(medicationGroupId: String) {
    preferences.edit()
      .remove(buildTakenKey(medicationGroupId))
      .apply()
  }

  fun getPendingRequestCodes(medicationGroupId: String): Set<Int> {
    return preferences
      .getStringSet(buildPendingCodesKey(medicationGroupId), emptySet())
      .orEmpty()
      .mapNotNull { value -> value.toIntOrNull() }
      .toSet()
  }

  fun addPendingRequestCode(medicationGroupId: String, requestCode: Int) {
    registerGroupId(medicationGroupId)
    val next = getPendingRequestCodes(medicationGroupId).toMutableSet()
    next.add(requestCode)
    savePendingRequestCodes(medicationGroupId, next)
  }

  fun removePendingRequestCode(medicationGroupId: String, requestCode: Int) {
    val next = getPendingRequestCodes(medicationGroupId).toMutableSet()
    next.remove(requestCode)
    savePendingRequestCodes(medicationGroupId, next)
  }

  fun getNotificationIds(medicationGroupId: String): Set<Int> {
    return preferences
      .getStringSet(buildNotificationIdsKey(medicationGroupId), emptySet())
      .orEmpty()
      .mapNotNull { value -> value.toIntOrNull() }
      .toSet()
  }

  fun addNotificationId(medicationGroupId: String, notificationId: Int) {
    registerGroupId(medicationGroupId)
    val next = getNotificationIds(medicationGroupId).toMutableSet()
    next.add(notificationId)
    saveNotificationIds(medicationGroupId, next)
  }

  fun clearGroup(medicationGroupId: String) {
    preferences.edit()
      .remove(buildPendingCodesKey(medicationGroupId))
      .remove(buildNotificationIdsKey(medicationGroupId))
      .remove(buildTakenKey(medicationGroupId))
      .apply()
    unregisterGroupId(medicationGroupId)
  }

  fun clearPendingState(medicationGroupId: String) {
    preferences.edit()
      .remove(buildPendingCodesKey(medicationGroupId))
      .remove(buildNotificationIdsKey(medicationGroupId))
      .apply()
  }

  fun dismissNotifications(medicationGroupId: String) {
    val notificationManager = NotificationManagerCompat.from(appContext)
    getNotificationIds(medicationGroupId).forEach(notificationManager::cancel)
    preferences.edit()
      .remove(buildNotificationIdsKey(medicationGroupId))
      .apply()
  }

  fun getRegisteredGroupIds(): Set<String> {
    return preferences.getStringSet("registered_group_ids", emptySet()).orEmpty()
  }

  private fun registerGroupId(medicationGroupId: String) {
    val next = getRegisteredGroupIds().toMutableSet()
    next.add(medicationGroupId)
    preferences.edit()
      .putStringSet("registered_group_ids", next)
      .apply()
  }

  private fun unregisterGroupId(medicationGroupId: String) {
    val next = getRegisteredGroupIds().toMutableSet()
    next.remove(medicationGroupId)
    preferences.edit()
      .putStringSet("registered_group_ids", next)
      .apply()
  }

  private fun savePendingRequestCodes(medicationGroupId: String, requestCodes: Set<Int>) {
    preferences.edit()
      .putStringSet(
        buildPendingCodesKey(medicationGroupId),
        requestCodes.map(Int::toString).toSet()
      )
      .apply()
  }

  private fun saveNotificationIds(medicationGroupId: String, notificationIds: Set<Int>) {
    preferences.edit()
      .putStringSet(
        buildNotificationIdsKey(medicationGroupId),
        notificationIds.map(Int::toString).toSet()
      )
      .apply()
  }

  private fun buildPendingCodesKey(medicationGroupId: String): String {
    return "$KEY_PENDING_CODES_PREFIX$medicationGroupId"
  }

  private fun buildTakenKey(medicationGroupId: String): String {
    return "$KEY_TAKEN_PREFIX$medicationGroupId"
  }

  private fun buildNotificationIdsKey(medicationGroupId: String): String {
    return "$KEY_NOTIFICATION_IDS_PREFIX$medicationGroupId"
  }
}
