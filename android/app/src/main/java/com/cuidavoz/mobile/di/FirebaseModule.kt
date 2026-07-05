package com.cuidavoz.mobile.di

import android.content.Context
import com.cuidavoz.mobile.BuildConfig
import com.cuidavoz.mobile.data.firebase.FirebaseAuthRepository
import com.cuidavoz.mobile.data.firebase.FirebaseStorageRepository
import com.cuidavoz.mobile.data.firebase.FirestoreHealthSettingsRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationLogRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationRepository
import com.cuidavoz.mobile.data.firebase.FirestorePatientRepository
import com.cuidavoz.mobile.data.firebase.FirestorePressureRepository
import com.cuidavoz.mobile.data.local.ContigoDatabase
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.data.sync.LinkCodeRateLimiter
import com.cuidavoz.mobile.data.sync.SyncContextRepository
import com.cuidavoz.mobile.data.sync.SyncManager
import com.cuidavoz.mobile.reminders.MedicationNotificationHelper
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import com.google.firebase.FirebaseApp
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
object FirebaseModule {
    @Provides
    @Singleton
    fun provideFirebaseAuthRepository(
        @ApplicationContext context: Context,
    ): FirebaseAuthRepository = FirebaseAuthRepository(context)

    @Provides
    @Singleton
    fun provideFirebaseFirestore(
        @ApplicationContext context: Context,
    ): FirebaseFirestore? {
        return if (FirebaseApp.getApps(context).isEmpty()) {
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
        return if (FirebaseApp.getApps(context).isEmpty()) {
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
    fun provideLinkCodeRateLimiter(
        @ApplicationContext context: Context,
    ): LinkCodeRateLimiter = LinkCodeRateLimiter(context)

    @Provides
    @Singleton
    fun provideSyncManager(
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
        linkCodeRateLimiter: LinkCodeRateLimiter,
    ): SyncManager = FirebaseSyncManager(
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
        linkCodeRateLimiter = linkCodeRateLimiter,
    )

    @Provides
    @Singleton
    fun provideFirestorePatientRepository(firestore: FirebaseFirestore?) = FirestorePatientRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestoreMedicationRepository(firestore: FirebaseFirestore?) = FirestoreMedicationRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestorePressureRepository(firestore: FirebaseFirestore?) = FirestorePressureRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestoreMedicationLogRepository(firestore: FirebaseFirestore?) = FirestoreMedicationLogRepository(firestore)

    @Provides
    @Singleton
    fun provideFirestoreHealthSettingsRepository(firestore: FirebaseFirestore?) = FirestoreHealthSettingsRepository(firestore)
    
    @Provides
    @Singleton
    fun provideFirebaseStorageRepository(storage: FirebaseStorage?) = FirebaseStorageRepository(storage)
}
