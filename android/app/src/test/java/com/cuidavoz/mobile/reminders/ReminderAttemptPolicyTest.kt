package com.cuidavoz.mobile.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderAttemptPolicyTest {
    @Test
    fun doesNotRepeatIfAlreadyTaken() {
        assertFalse(
            ReminderAttemptPolicy.shouldScheduleNextAttempt(
                attemptNumber = 1,
                maxAttempts = 3,
                currentStatus = "TAKEN",
            ),
        )
    }

    @Test
    fun repeatsIfStillPending() {
        assertTrue(
            ReminderAttemptPolicy.shouldScheduleNextAttempt(
                attemptNumber = 1,
                maxAttempts = 3,
                currentStatus = "PENDING",
            ),
        )
    }

    @Test
    fun marksMissedAfterMaxAttempts() {
        assertTrue(
            ReminderAttemptPolicy.shouldMarkMissed(
                attemptNumber = 3,
                maxAttempts = 3,
                currentStatus = "PENDING",
            ),
        )
    }
}
