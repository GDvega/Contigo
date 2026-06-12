package com.cuidavoz.mobile

import com.cuidavoz.mobile.data.backup.BackupRepository
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.firebase.FirebaseAuthRepository
import com.cuidavoz.mobile.data.report.PdfReportGenerator
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.MedicalReportRepository
import com.cuidavoz.mobile.data.repository.MedicationLogRepository
import com.cuidavoz.mobile.data.repository.MedicationReminderRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.OnboardingRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.data.sync.SyncContextRepository
import com.cuidavoz.mobile.reminders.MedicationNotificationHelper
import com.cuidavoz.mobile.reminders.MedicationReminderActionHandler
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderLaunchState
import com.cuidavoz.mobile.voice.SpeechRecognitionManager
import com.cuidavoz.mobile.voice.TextToSpeechManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContigoAppContainer @Inject constructor(
    val onboardingRepository: OnboardingRepository,
    val syncContextRepository: SyncContextRepository,
    val firebaseAuthRepository: FirebaseAuthRepository,
    val firebaseSyncManager: FirebaseSyncManager,
    val patientRepository: PatientRepository,
    val familyContactRepository: FamilyContactRepository,
    val medicationLogRepository: MedicationLogRepository,
    val medicationReminderRepository: MedicationReminderRepository,
    val pressureRepository: PressureRepository,
    val settingsRepository: SettingsRepository,
    val medicationRepository: MedicationRepository,
    val reminderLaunchState: ReminderLaunchState,
    val notificationHelper: MedicationNotificationHelper,
    val medicationImageStorage: MedicationImageStorage,
    val pdfReportGenerator: PdfReportGenerator,
    val textToSpeechManager: TextToSpeechManager,
    val speechRecognitionManager: SpeechRecognitionManager,
    val dailyStatusRepository: DailyStatusRepository,
    val reminderActionHandler: MedicationReminderActionHandler,
    val reminderScheduler: MedicationReminderScheduler,
    val medicalReportRepository: MedicalReportRepository,
    val backupRepository: BackupRepository,
)
