package com.cuidavoz.mobile.di

import android.content.Context
import com.cuidavoz.mobile.BuildConfig
import com.cuidavoz.mobile.ContigoAppContainer
import com.cuidavoz.mobile.data.backup.BackupRepository
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.firebase.FirebaseAuthRepository
import com.cuidavoz.mobile.data.firebase.FirestoreHealthSettingsRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationLogRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationRepository
import com.cuidavoz.mobile.data.firebase.FirestorePatientRepository
import com.cuidavoz.mobile.data.firebase.FirestorePressureRepository
import com.cuidavoz.mobile.data.local.ContigoDatabase
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
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import com.cuidavoz.mobile.voice.SpeechRecognitionManager
import com.cuidavoz.mobile.voice.TextToSpeechManager
import com.cuidavoz.mobile.data.firebase.FirebaseStorageRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOnboardingRepository(
        @ApplicationContext context: Context,
    ): OnboardingRepository = OnboardingRepository(context)

    @Provides
    @Singleton
    fun provideSyncContextRepository(
        @ApplicationContext context: Context,
    ): SyncContextRepository = SyncContextRepository(context)

    @Provides
    @Singleton
    fun provideFirebaseAuthRepository(
        @ApplicationContext context: Context,
    ): FirebaseAuthRepository = FirebaseAuthRepository(context)

    @Provides
    @Singleton
    fun provideFirestorePatientRepository(
        firestore: FirebaseFirestore?,
    ): FirestorePatientRepository = FirestorePatientRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestoreMedicationRepository(
        firestore: FirebaseFirestore?,
    ): FirestoreMedicationRepository = FirestoreMedicationRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestorePressureRepository(
        firestore: FirebaseFirestore?,
    ): FirestorePressureRepository = FirestorePressureRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestoreMedicationLogRepository(
        firestore: FirebaseFirestore?,
    ): FirestoreMedicationLogRepository = FirestoreMedicationLogRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestoreHealthSettingsRepository(
        firestore: FirebaseFirestore?,
    ): FirestoreHealthSettingsRepository = FirestoreHealthSettingsRepository(firestore)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(
        @ApplicationContext context: Context,
    ): FirebaseFirestore? {
        return if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            null
        } else {
            FirebaseFirestore.getInstance().also { firestore ->
                if (BuildConfig.DEBUG && BuildConfig.FIREBASE_EMULATOR_HOST.isNotBlank()) {
                    firestore.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 8080)
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(
        @ApplicationContext context: Context,
    ): FirebaseStorage? {
        return if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            null
        } else {
            FirebaseStorage.getInstance().also { storage ->
                if (BuildConfig.DEBUG && BuildConfig.FIREBASE_EMULATOR_HOST.isNotBlank()) {
                    storage.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9199)
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseStorageRepository(
        storage: FirebaseStorage?,
    ): FirebaseStorageRepository = FirebaseStorageRepository(storage)

    @Provides
    @Singleton
    fun provideFirebaseSyncManager(
        @ApplicationContext context: Context,
        database: ContigoDatabase,
        reminderPreferencesRepository: ReminderPreferencesRepository,
        syncContextRepository: SyncContextRepository,
        authRepository: FirebaseAuthRepository,
        patientRepository: FirestorePatientRepository,
        medicationRepository: FirestoreMedicationRepository,
        pressureRepository: FirestorePressureRepository,
        medicationLogRepository: FirestoreMedicationLogRepository,
        healthSettingsRepository: FirestoreHealthSettingsRepository,
        storageRepository: FirebaseStorageRepository,
        notificationHelper: MedicationNotificationHelper,
    ): FirebaseSyncManager = FirebaseSyncManager(
        context = context,
        database = database,
        reminderPreferencesRepository = reminderPreferencesRepository,
        syncContextRepository = syncContextRepository,
        authRepository = authRepository,
        patientRepository = patientRepository,
        medicationRepository = medicationRepository,
        pressureRepository = pressureRepository,
        medicationLogRepository = medicationLogRepository,
        healthSettingsRepository = healthSettingsRepository,
        storageRepository = storageRepository,
        notificationHelper = notificationHelper,
    )

    @Provides
    @Singleton
    fun providePatientRepository(
        patientDao: com.cuidavoz.mobile.data.local.PatientDao,
        firebaseSyncManager: FirebaseSyncManager,
    ): PatientRepository = PatientRepository(
        patientDao = patientDao,
        firebaseSyncManager = firebaseSyncManager,
    )

    @Provides
    @Singleton
    fun provideFamilyContactRepository(
        familyContactDao: com.cuidavoz.mobile.data.local.FamilyContactDao,
        firebaseSyncManager: FirebaseSyncManager,
    ): FamilyContactRepository = FamilyContactRepository(
        familyContactDao = familyContactDao,
        firebaseSyncManager = firebaseSyncManager,
    )

    @Provides
    @Singleton
    fun provideMedicationLogRepository(
        medicationLogDao: com.cuidavoz.mobile.data.local.MedicationLogDao,
    ): MedicationLogRepository = MedicationLogRepository(medicationLogDao)

    @Provides
    @Singleton
    fun provideMedicationReminderRepository(
        medicationReminderDao: com.cuidavoz.mobile.data.local.MedicationReminderDao,
    ): MedicationReminderRepository = MedicationReminderRepository(medicationReminderDao)

    @Provides
    @Singleton
    fun providePressureRepository(
        bloodPressureDao: com.cuidavoz.mobile.data.local.BloodPressureDao,
        healthSettingsDao: com.cuidavoz.mobile.data.local.HealthSettingsDao,
        firebaseSyncManager: FirebaseSyncManager,
    ): PressureRepository = PressureRepository(
        bloodPressureDao = bloodPressureDao,
        healthSettingsDao = healthSettingsDao,
        firebaseSyncManager = firebaseSyncManager,
    )

    @Provides
    @Singleton
    fun provideReminderPreferencesRepository(
        @ApplicationContext context: Context,
    ): ReminderPreferencesRepository = ReminderPreferencesRepository(context)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        healthSettingsDao: com.cuidavoz.mobile.data.local.HealthSettingsDao,
        reminderPreferencesRepository: ReminderPreferencesRepository,
        firebaseSyncManager: FirebaseSyncManager,
    ): SettingsRepository = SettingsRepository(
        healthSettingsDao = healthSettingsDao,
        reminderPreferencesRepository = reminderPreferencesRepository,
        firebaseSyncManager = firebaseSyncManager,
    )

    @Provides
    @Singleton
    fun provideMedicationRepository(
        medicationDao: com.cuidavoz.mobile.data.local.MedicationDao,
        firebaseSyncManager: FirebaseSyncManager,
    ): MedicationRepository = MedicationRepository(
        medicationDao = medicationDao,
        firebaseSyncManager = firebaseSyncManager,
    )

    @Provides
    @Singleton
    fun provideReminderLaunchState(): ReminderLaunchState = ReminderLaunchState()

    @Provides
    @Singleton
    fun provideMedicationNotificationHelper(
        @ApplicationContext context: Context,
    ): MedicationNotificationHelper = MedicationNotificationHelper(context)

    @Provides
    @Singleton
    fun provideMedicationImageStorage(
        @ApplicationContext context: Context,
    ): MedicationImageStorage = MedicationImageStorage(context)

    @Provides
    @Singleton
    fun providePdfReportGenerator(
        @ApplicationContext context: Context,
    ): PdfReportGenerator = PdfReportGenerator(context)

    @Provides
    @Singleton
    fun provideTextToSpeechManager(
        @ApplicationContext context: Context,
    ): TextToSpeechManager = TextToSpeechManager(context)

    @Provides
    @Singleton
    fun provideSpeechRecognitionManager(
        @ApplicationContext context: Context,
    ): SpeechRecognitionManager = SpeechRecognitionManager(context)

    @Provides
    @Singleton
    fun provideDailyStatusRepository(
        medicationDao: com.cuidavoz.mobile.data.local.MedicationDao,
        medicationLogDao: com.cuidavoz.mobile.data.local.MedicationLogDao,
        bloodPressureDao: com.cuidavoz.mobile.data.local.BloodPressureDao,
        firebaseSyncManager: FirebaseSyncManager,
    ): DailyStatusRepository = DailyStatusRepository(
        medicationDao = medicationDao,
        medicationLogDao = medicationLogDao,
        bloodPressureDao = bloodPressureDao,
        firebaseSyncManager = firebaseSyncManager,
    )

    @Provides
    @Singleton
    fun provideMedicationReminderActionHandler(
        medicationReminderRepository: MedicationReminderRepository,
        medicationRepository: MedicationRepository,
        dailyStatusRepository: DailyStatusRepository,
        familyContactRepository: FamilyContactRepository,
        settingsRepository: SettingsRepository,
        firebaseSyncManager: FirebaseSyncManager,
    ): MedicationReminderActionHandler = MedicationReminderActionHandler(
        medicationReminderRepository = medicationReminderRepository,
        medicationRepository = medicationRepository,
        dailyStatusRepository = dailyStatusRepository,
        familyContactRepository = familyContactRepository,
        settingsRepository = settingsRepository,
        firebaseSyncManager = firebaseSyncManager,
    )

    @Provides
    @Singleton
    fun provideMedicationReminderScheduler(
        @ApplicationContext context: Context,
        medicationRepository: MedicationRepository,
        medicationLogRepository: MedicationLogRepository,
        medicationReminderRepository: MedicationReminderRepository,
        settingsRepository: SettingsRepository,
        notificationHelper: MedicationNotificationHelper,
        dailyStatusRepository: DailyStatusRepository,
        actionHandler: MedicationReminderActionHandler,
        firebaseSyncManager: FirebaseSyncManager,
    ): MedicationReminderScheduler {
        return MedicationReminderScheduler(
            context = context,
            medicationRepository = medicationRepository,
            medicationLogRepository = medicationLogRepository,
            medicationReminderRepository = medicationReminderRepository,
            settingsRepository = settingsRepository,
            notificationHelper = notificationHelper,
            dailyStatusRepository = dailyStatusRepository,
            actionHandler = actionHandler,
        ).also(firebaseSyncManager::attachReminderScheduler)
    }

    @Provides
    @Singleton
    fun provideMedicalReportRepository(
        patientDao: com.cuidavoz.mobile.data.local.PatientDao,
        familyContactRepository: FamilyContactRepository,
        healthSettingsDao: com.cuidavoz.mobile.data.local.HealthSettingsDao,
        medicationDao: com.cuidavoz.mobile.data.local.MedicationDao,
        medicationLogDao: com.cuidavoz.mobile.data.local.MedicationLogDao,
        bloodPressureDao: com.cuidavoz.mobile.data.local.BloodPressureDao,
    ): MedicalReportRepository = MedicalReportRepository(
        patientDao = patientDao,
        familyContactRepository = familyContactRepository,
        healthSettingsDao = healthSettingsDao,
        medicationDao = medicationDao,
        medicationLogDao = medicationLogDao,
        bloodPressureDao = bloodPressureDao,
    )

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        database: ContigoDatabase,
        medicationImageStorage: MedicationImageStorage,
        reminderPreferencesRepository: ReminderPreferencesRepository,
        firebaseSyncManager: FirebaseSyncManager,
    ): BackupRepository = BackupRepository(
        context = context,
        database = database,
        medicationImageStorage = medicationImageStorage,
        reminderPreferencesRepository = reminderPreferencesRepository,
        firebaseSyncManager = firebaseSyncManager,
    )
}
