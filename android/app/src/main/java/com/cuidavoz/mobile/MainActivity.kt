package com.cuidavoz.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cuidavoz.mobile.reminders.ACTION_CONFIRM_REMINDER
import com.cuidavoz.mobile.reminders.ACTION_OPEN_REMINDER
import com.cuidavoz.mobile.reminders.ReminderPrompt
import com.cuidavoz.mobile.reminders.toReminderPayload
import com.cuidavoz.mobile.ui.navigation.AppNavigation
import com.cuidavoz.mobile.ui.theme.CuidaVozTheme

class MainActivity : ComponentActivity() {
    private val appContainer: CuidaVozAppContainer
        get() = (application as CuidaVozApp).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReminderIntent(intent)

        setContent {
            CuidaVozTheme {
                AppNavigation(
                    appContainer = appContainer,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderIntent(intent)
    }

    private fun handleReminderIntent(intent: Intent?) {
        if (intent?.action != ACTION_OPEN_REMINDER && intent?.action != ACTION_CONFIRM_REMINDER) {
            return
        }

        val payload = intent.toReminderPayload() ?: return
        appContainer.reminderLaunchState.showPrompt(
            ReminderPrompt(
                reminderId = payload.reminderId,
                reminderGroupId = payload.reminderGroupId,
                patientId = payload.patientId,
                scheduleTime = payload.scheduleTime,
                targetDate = payload.targetDate,
                scheduledAt = payload.scheduledAt,
                medicationIds = payload.medicationIds,
                medicationNames = payload.medicationNames,
                requiresConfirmation = payload.requiresConfirmation,
            ),
        )
    }
}
