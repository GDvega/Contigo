package com.cuidavoz.mobile.di

import android.content.Context
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.report.PdfReportGenerator
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.MedicationReminderRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.data.sync.SyncManager
import com.cuidavoz.mobile.reminders.MedicationNotificationHelper
import com.cuidavoz.mobile.reminders.MedicationReminderActionHandler
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderLaunchState
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import com.cuidavoz.mobile.voice.SpeechRecognitionManager
import com.cuidavoz.mobile.voice.TextToSpeechManager
import com.cuidavoz.mobile.data.sync.SyncContextRepository
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
    fun provideSyncContextRepository(
        @ApplicationContext context: Context,
    ): SyncContextRepository = SyncContextRepository(context)

    @Provides
    @Singleton
    fun provideReminderPreferencesRepository(
        @ApplicationContext context: Context,
    ): ReminderPreferencesRepository = ReminderPreferencesRepository(context)

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
    fun provideMedicationReminderActionHandler(
        medicationReminderRepository: MedicationReminderRepository,
        medicationRepository: MedicationRepository,
        dailyStatusRepository: DailyStatusRepository,
        familyContactRepository: FamilyContactRepository,
        settingsRepository: SettingsRepository,
        syncManager: SyncManager,
    ): MedicationReminderActionHandler = MedicationReminderActionHandler(
        medicationReminderRepository = medicationReminderRepository,
        medicationRepository = medicationRepository,
        dailyStatusRepository = dailyStatusRepository,
        familyContactRepository = familyContactRepository,
        settingsRepository = settingsRepository,
        firebaseSyncManager = syncManager,
    )

    @Provides
    @Singleton
    fun provideMedicationReminderScheduler(
        @ApplicationContext context: Context,
        medicationRepository: MedicationRepository,
        medicationLogRepository: com.cuidavoz.mobile.data.repository.MedicationLogRepository,
        medicationReminderRepository: MedicationReminderRepository,
        settingsRepository: SettingsRepository,
        notificationHelper: MedicationNotificationHelper,
        dailyStatusRepository: DailyStatusRepository,
        actionHandler: MedicationReminderActionHandler,
        syncManager: SyncManager,
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
        ).also(syncManager::attachReminderScheduler)
    }
}
