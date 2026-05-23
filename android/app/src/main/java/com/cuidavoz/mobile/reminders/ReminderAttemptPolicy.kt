package com.cuidavoz.mobile.reminders

object ReminderAttemptPolicy {
    fun shouldScheduleNextAttempt(
        attemptNumber: Int,
        maxAttempts: Int,
        currentStatus: String,
    ): Boolean {
        return currentStatus == "PENDING" && attemptNumber < maxAttempts
    }

    fun shouldMarkMissed(
        attemptNumber: Int,
        maxAttempts: Int,
        currentStatus: String,
    ): Boolean {
        return currentStatus == "PENDING" && attemptNumber >= maxAttempts
    }
}
