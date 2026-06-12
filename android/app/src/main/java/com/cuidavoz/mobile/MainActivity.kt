package com.cuidavoz.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cuidavoz.mobile.reminders.ACTION_CONFIRM_REMINDER
import com.cuidavoz.mobile.reminders.ACTION_OPEN_REMINDER
import com.cuidavoz.mobile.reminders.ReminderPrompt
import com.cuidavoz.mobile.reminders.toReminderPayload
import com.cuidavoz.mobile.ui.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appContainer: ContigoAppContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReminderIntent(intent)

        setContent {
            AppNavigation()
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
