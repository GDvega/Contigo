package com.cuidavoz.mobile.reminders

import android.content.Intent
import kotlin.math.absoluteValue

const val MEDICATION_REMINDER_CHANNEL_ID = "medication_reminders"
const val ACTION_OPEN_REMINDER = "com.cuidavoz.mobile.action.OPEN_REMINDER"
const val ACTION_CONFIRM_REMINDER = "com.cuidavoz.mobile.action.CONFIRM_REMINDER"
const val ACTION_SHOW_REMINDER = "com.cuidavoz.mobile.action.SHOW_REMINDER"
const val ACTION_MARK_TAKEN = "com.cuidavoz.mobile.action.MARK_TAKEN"
const val ACTION_SNOOZE_REMINDER = "com.cuidavoz.mobile.action.SNOOZE_REMINDER"
const val ACTION_REQUEST_HELP = "com.cuidavoz.mobile.action.REQUEST_HELP"
const val ACTION_RESPOND_BY_VOICE = "com.cuidavoz.mobile.action.RESPOND_BY_VOICE"
const val ACTION_FINALIZE_MISSED = "com.cuidavoz.mobile.action.FINALIZE_MISSED"
const val EXTRA_REMINDER_ID = "extra_reminder_id"
const val EXTRA_REMINDER_GROUP_ID = "extra_reminder_group_id"
const val EXTRA_PATIENT_ID = "extra_patient_id"
const val EXTRA_SCHEDULE_TIME = "extra_schedule_time"
const val EXTRA_TARGET_DATE = "extra_target_date"
const val EXTRA_SCHEDULED_AT = "extra_scheduled_at"
const val EXTRA_MEDICATION_IDS = "extra_medication_ids"
const val EXTRA_MEDICATION_NAMES = "extra_medication_names"
const val EXTRA_ATTEMPT_NUMBER = "extra_attempt_number"
const val EXTRA_MAX_ATTEMPTS = "extra_max_attempts"
const val EXTRA_REPEAT_EVERY_MINUTES = "extra_repeat_every_minutes"
const val EXTRA_REQUIRES_CONFIRMATION = "extra_requires_confirmation"
const val EXTRA_OPEN_VOICE = "extra_open_voice"
const val EXTRA_REQUEST_HELP = "extra_request_help"

private const val LIST_SEPARATOR = "||"

data class ReminderPayload(
    val reminderId: String? = null,
    val reminderGroupId: String,
    val patientId: String,
    val scheduleTime: String,
    val targetDate: String,
    val scheduledAt: Long,
    val medicationIds: List<String>,
    val medicationNames: List<String>,
    val attemptNumber: Int,
    val maxAttempts: Int,
    val repeatEveryMinutes: Int,
    val requiresConfirmation: Boolean = false,
)

data class ReminderPrompt(
    val reminderId: String?,
    val reminderGroupId: String,
    val patientId: String,
    val scheduleTime: String,
    val targetDate: String,
    val scheduledAt: Long,
    val medicationIds: List<String>,
    val medicationNames: List<String>,
    val requiresConfirmation: Boolean,
    val openVoice: Boolean = false,
    val requestHelp: Boolean = false,
    val nonce: Long = System.currentTimeMillis(),
)

fun reminderRequestCode(
    reminderGroupId: String,
    attemptNumber: Int,
    suffix: String = "",
): Int = "${reminderGroupId}_${attemptNumber}_$suffix".hashCode().absoluteValue

fun List<String>.encodeReminderList(): String = joinToString(LIST_SEPARATOR)

fun String.decodeReminderList(): List<String> =
    if (isBlank()) emptyList() else split(LIST_SEPARATOR).map(String::trim).filter(String::isNotEmpty)

fun ReminderPayload.toReceiverIntent(): Intent {
    return Intent().apply {
        putExtra(EXTRA_REMINDER_ID, reminderId)
        putExtra(EXTRA_REMINDER_GROUP_ID, reminderGroupId)
        putExtra(EXTRA_PATIENT_ID, patientId)
        putExtra(EXTRA_SCHEDULE_TIME, scheduleTime)
        putExtra(EXTRA_TARGET_DATE, targetDate)
        putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
        putExtra(EXTRA_MEDICATION_IDS, medicationIds.encodeReminderList())
        putExtra(EXTRA_MEDICATION_NAMES, medicationNames.encodeReminderList())
        putExtra(EXTRA_ATTEMPT_NUMBER, attemptNumber)
        putExtra(EXTRA_MAX_ATTEMPTS, maxAttempts)
        putExtra(EXTRA_REPEAT_EVERY_MINUTES, repeatEveryMinutes)
        putExtra(EXTRA_REQUIRES_CONFIRMATION, requiresConfirmation)
    }
}

fun Intent.toReminderPayload(): ReminderPayload? {
    val reminderGroupId = getStringExtra(EXTRA_REMINDER_GROUP_ID) ?: return null
    val patientId = getStringExtra(EXTRA_PATIENT_ID) ?: return null
    val scheduleTime = getStringExtra(EXTRA_SCHEDULE_TIME) ?: return null
    return ReminderPayload(
        reminderId = getStringExtra(EXTRA_REMINDER_ID),
        reminderGroupId = reminderGroupId,
        patientId = patientId,
        scheduleTime = scheduleTime,
        targetDate = getStringExtra(EXTRA_TARGET_DATE).orEmpty(),
        scheduledAt = getLongExtra(EXTRA_SCHEDULED_AT, 0L),
        medicationIds = getStringExtra(EXTRA_MEDICATION_IDS).orEmpty().decodeReminderList(),
        medicationNames = getStringExtra(EXTRA_MEDICATION_NAMES).orEmpty().decodeReminderList(),
        attemptNumber = getIntExtra(EXTRA_ATTEMPT_NUMBER, 0),
        maxAttempts = getIntExtra(EXTRA_MAX_ATTEMPTS, 3),
        repeatEveryMinutes = getIntExtra(EXTRA_REPEAT_EVERY_MINUTES, 10),
        requiresConfirmation = getBooleanExtra(EXTRA_REQUIRES_CONFIRMATION, false),
    )
}
