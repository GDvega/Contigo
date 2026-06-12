package com.cuidavoz.mobile.di

import com.cuidavoz.mobile.data.migration.LegacyDemoDataCleaner
import com.cuidavoz.mobile.data.repository.OnboardingRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContigoAppInitializer @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val legacyDemoDataCleaner: LegacyDemoDataCleaner,
    private val onboardingRepository: OnboardingRepository,
    private val patientRepository: PatientRepository,
    private val reminderScheduler: MedicationReminderScheduler,
    private val firebaseSyncManager: FirebaseSyncManager,
) {
    fun start() {
        applicationScope.launch {
            legacyDemoDataCleaner.purgeLegacyDemoData()
            val setupCompleted = onboardingRepository.setupCompletedFlow.first()
            if (setupCompleted && patientRepository.getCurrentPatient() != null) {
                reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            }
            firebaseSyncManager.start()
        }
    }
}
