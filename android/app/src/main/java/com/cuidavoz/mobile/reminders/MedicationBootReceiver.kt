package com.cuidavoz.mobile.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cuidavoz.mobile.util.ContigoLog
import com.cuidavoz.mobile.ContigoApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MedicationBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ContigoLog.d(TAG, "Reprogramando recordatorios")
                val appContainer = (context.applicationContext as ContigoApp).appContainer
                appContainer.reminderScheduler.rescheduleAllAfterBoot()
            } catch (error: Exception) {
                ContigoLog.e(TAG, "Error al reprogramar recordatorios tras reinicio", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "[Contigo][BootReceiver]"
    }
}
