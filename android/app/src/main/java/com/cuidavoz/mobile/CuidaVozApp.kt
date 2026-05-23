package com.cuidavoz.mobile

import android.app.Application
import com.cuidavoz.mobile.reminders.MedicationNotificationChannel
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CuidaVozApp : Application() {
    lateinit var appContainer: CuidaVozAppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appContainer = CuidaVozAppContainer(this)
        MedicationNotificationChannel.create(this)
        applicationScope.launch {
            appContainer.ensureBaselineData()
            appContainer.reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            appContainer.firebaseSyncManager.start()
        }
    }
}
