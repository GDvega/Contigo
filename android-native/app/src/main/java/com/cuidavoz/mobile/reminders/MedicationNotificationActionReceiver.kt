package com.cuidavoz.mobile.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cuidavoz.mobile.CuidaVozApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val payload = intent.toReminderPayload() ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContainer = (context.applicationContext as CuidaVozApp).appContainer
                when (intent.action) {
                    ACTION_MARK_TAKEN -> {
                        val saved = appContainer.reminderScheduler.markReminderTaken(payload)
                        if (saved) {
                            appContainer.reminderScheduler.cancelReminderGroup(payload.reminderGroupId, payload.scheduleTime)
                            appContainer.notificationHelper.showConfirmationNotification("Listo. Toma registrada.", payload)
                        }
                    }
                    ACTION_SNOOZE_REMINDER -> {
                        appContainer.reminderScheduler.markReminderSnoozed(payload)
                        appContainer.notificationHelper.showConfirmationNotification("Te lo recordaré después.", payload)
                    }
                    ACTION_REQUEST_HELP -> {
                        appContainer.reminderScheduler.requestHelp(payload)
                        appContainer.notificationHelper.showConfirmationNotification("Se avisará al cuidador.", payload)
                    }
                }
            } catch (error: Exception) {
                Log.e(TAG, "No se pudo procesar acción de notificación", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "[CuidaVoz][NotificationAction]"
    }
}
