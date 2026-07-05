package com.cuidavoz.mobile.di

import android.content.Context
import com.cuidavoz.mobile.data.backup.BackupRepository
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.local.BloodPressureDao
import com.cuidavoz.mobile.data.local.ContigoDatabase
import com.cuidavoz.mobile.data.local.FamilyContactDao
import com.cuidavoz.mobile.data.local.HealthSettingsDao
import com.cuidavoz.mobile.data.local.MedicationDao
import com.cuidavoz.mobile.data.local.MedicationLogDao
import com.cuidavoz.mobile.data.local.MedicationReminderDao
import com.cuidavoz.mobile.data.local.PatientDao
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
import com.cuidavoz.mobile.data.sync.SyncManager
import com.cuidavoz.mobile.reminders.MedicationNotificationHelper
import com.cuidavoz.mobile.reminders.MedicationReminderActionHandler
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideOnboardingRepository(
        @ApplicationContext context: Context,
    ): OnboardingRepository = OnboardingRepository(context)

    @Provides
    @Singleton
    fun providePatientRepository(
        patientDao: PatientDao,
        syncManager: SyncManager,
    ): PatientRepository = PatientRepository(patientDao, syncManager)

    @Provides
    @Singleton
    fun provideFamilyContactRepository(
        familyContactDao: FamilyContactDao,
        syncManager: SyncManager,
    ): FamilyContactRepository = FamilyContactRepository(familyContactDao, syncManager)

    @Provides
    @Singleton
    fun provideMedicationLogRepository(
        medicationLogDao: MedicationLogDao,
    ): MedicationLogRepository = MedicationLogRepository(medicationLogDao)

    @Provides
    @Singleton
    fun provideMedicationReminderRepository(
        medicationReminderDao: MedicationReminderDao,
    ): MedicationReminderRepository = MedicationReminderRepository(medicationReminderDao)

    @Provides
    @Singleton
    fun providePressureRepository(
        bloodPressureDao: BloodPressureDao,
        healthSettingsDao: HealthSettingsDao,
        syncManager: SyncManager,
    ): PressureRepository = PressureRepository(bloodPressureDao, healthSettingsDao, syncManager)

    @Provides
    @Singleton
    fun provideSettingsRepository(
        healthSettingsDao: HealthSettingsDao,
        reminderPreferencesRepository: ReminderPreferencesRepository,
        syncManager: SyncManager,
    ): SettingsRepository = SettingsRepository(healthSettingsDao, reminderPreferencesRepository, syncManager)

    @Provides
    @Singleton
    fun provideMedicationRepository(
        medicationDao: MedicationDao,
        syncManager: SyncManager,
    ): MedicationRepository = MedicationRepository(medicationDao, syncManager)

    @Provides
    @Singleton
    fun provideDailyStatusRepository(
        medicationDao: MedicationDao,
        medicationLogDao: MedicationLogDao,
        bloodPressureDao: BloodPressureDao,
        syncManager: SyncManager,
    ): DailyStatusRepository = DailyStatusRepository(medicationDao, medicationLogDao, bloodPressureDao, syncManager)

    @Provides
    @Singleton
    fun provideMedicalReportRepository(
        patientDao: PatientDao,
        familyContactRepository: FamilyContactRepository,
        healthSettingsDao: HealthSettingsDao,
        medicationDao: MedicationDao,
        medicationLogDao: MedicationLogDao,
        bloodPressureDao: BloodPressureDao,
    ): MedicalReportRepository = MedicalReportRepository(
        patientDao, familyContactRepository, healthSettingsDao, medicationDao, medicationLogDao, bloodPressureDao
    )

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
        database: ContigoDatabase,
        medicationImageStorage: MedicationImageStorage,
        reminderPreferencesRepository: ReminderPreferencesRepository,
        syncManager: SyncManager,
    ): BackupRepository = BackupRepository(context, database, medicationImageStorage, reminderPreferencesRepository, syncManager)
}
