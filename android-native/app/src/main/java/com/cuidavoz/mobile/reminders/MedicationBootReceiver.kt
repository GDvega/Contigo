package com.cuidavoz.mobile.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.cuidavoz.mobile.CuidaVozApp
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
                Log.d(TAG, "Reprogramando recordatorios")
                val appContainer = (context.applicationContext as CuidaVozApp).appContainer
                appContainer.reminderScheduler.rescheduleAllAfterBoot()
            } catch (error: Exception) {
                Log.e(TAG, "Error al reprogramar recordatorios tras reinicio", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "[CuidaVoz][BootReceiver]"
    }
}
