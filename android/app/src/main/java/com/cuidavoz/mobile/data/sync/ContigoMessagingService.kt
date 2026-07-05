package com.cuidavoz.mobile.data.sync

import com.cuidavoz.mobile.reminders.MedicationNotificationHelper
import com.cuidavoz.mobile.reminders.ReminderPayload
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ContigoMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        val notificationHelper = messagingEntryPoint().notificationHelper()

        val title = message.notification?.title ?: message.data["title"] ?: "Contigo"
        val body = message.notification?.body ?: message.data["body"] ?: "Tienes una nueva actualización"

        notificationHelper.showConfirmationNotification(
            message = body,
            payload = ReminderPayload(
                reminderGroupId = "push_${message.messageId}",
                patientId = "",
                scheduleTime = "",
                targetDate = "",
                scheduledAt = System.currentTimeMillis(),
                medicationIds = emptyList(),
                medicationNames = emptyList(),
                attemptNumber = 1,
                maxAttempts = 1,
                repeatEveryMinutes = 0
            )
        )
    }

    override fun onNewToken(token: String) {
        val syncManager = messagingEntryPoint().firebaseSyncManager()
        serviceScope.launch {
            syncManager.start() // Trigger token refresh logic inside sync manager
        }
    }

    private fun messagingEntryPoint(): MessagingServiceEntryPoint {
        return EntryPointAccessors.fromApplication(
            applicationContext,
            MessagingServiceEntryPoint::class.java,
        )
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MessagingServiceEntryPoint {
    fun notificationHelper(): MedicationNotificationHelper
    fun firebaseSyncManager(): SyncManager
}
