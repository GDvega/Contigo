package com.cuidavoz.mobile.di

import android.content.Context
import com.cuidavoz.mobile.data.local.BloodPressureDao
import com.cuidavoz.mobile.data.local.ContigoDatabase
import com.cuidavoz.mobile.data.local.FamilyContactDao
import com.cuidavoz.mobile.data.local.HealthSettingsDao
import com.cuidavoz.mobile.data.local.MedicationDao
import com.cuidavoz.mobile.data.local.MedicationLogDao
import com.cuidavoz.mobile.data.local.MedicationReminderDao
import com.cuidavoz.mobile.data.local.PatientDao
import com.cuidavoz.mobile.data.local.SyncQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): ContigoDatabase = ContigoDatabase.getDatabase(context)

    @Provides fun providePatientDao(database: ContigoDatabase): PatientDao = database.patientDao()
    @Provides fun provideMedicationDao(database: ContigoDatabase): MedicationDao = database.medicationDao()
    @Provides fun provideBloodPressureDao(database: ContigoDatabase): BloodPressureDao = database.bloodPressureDao()
    @Provides fun provideMedicationLogDao(database: ContigoDatabase): MedicationLogDao = database.medicationLogDao()
    @Provides fun provideMedicationReminderDao(database: ContigoDatabase): MedicationReminderDao =
        database.medicationReminderDao()
    @Provides fun provideHealthSettingsDao(database: ContigoDatabase): HealthSettingsDao = database.healthSettingsDao()
    @Provides fun provideFamilyContactDao(database: ContigoDatabase): FamilyContactDao = database.familyContactDao()
    @Provides fun provideSyncQueueDao(database: ContigoDatabase): SyncQueueDao = database.syncQueueDao()
}
