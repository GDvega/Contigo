package com.cuidavoz.mobile.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.cuidavoz.mobile.ContigoApp
import com.cuidavoz.mobile.util.ContigoLog
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import java.util.concurrent.TimeUnit

/**
 * Reprograma todas las alarmas de medicamentos de forma periódica para que la
 * cadena de recordatorios no dependa de que el usuario abra la app o reinicie el teléfono.
 */
class DailyReminderRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val appContainer = (applicationContext as ContigoApp).appContainer
            if (appContainer.patientRepository.getCurrentPatient() != null) {
                appContainer.reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            }
            Result.success()
        } catch (error: Exception) {
            ContigoLog.e(TAG, "No se pudo reprogramar recordatorios periódicos", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "[Contigo][RefreshWorker]"
        private const val UNIQUE_NAME = "daily_reminder_refresh"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyReminderRefreshWorker>(12, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
