package com.cuidavoz.mobile

import android.content.Context
import androidx.room.withTransaction
import com.cuidavoz.mobile.data.backup.BackupRepository
import com.cuidavoz.mobile.data.firebase.FirebaseAuthRepository
import com.cuidavoz.mobile.data.firebase.FirestoreHealthSettingsRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationLogRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationRepository
import com.cuidavoz.mobile.data.firebase.FirestorePatientRepository
import com.cuidavoz.mobile.data.firebase.FirestorePressureRepository
import com.cuidavoz.mobile.data.local.CuidaVozDatabase
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.report.PdfReportGenerator
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.MedicalReportRepository
import com.cuidavoz.mobile.data.repository.MedicationLogRepository
import com.cuidavoz.mobile.data.repository.MedicationReminderRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.data.sync.SyncContextRepository
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.reminders.MedicationNotificationHelper
import com.cuidavoz.mobile.reminders.MedicationReminderActionHandler
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderLaunchState
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.voice.SpeechRecognitionManager
import com.cuidavoz.mobile.voice.TextToSpeechManager
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

private const val FAMILY_CONTACT_ID = "family_contact_juan"
private const val HEALTH_SETTINGS_ID = "health_settings_maria"

class CuidaVozAppContainer(context: Context) {
    private val database = CuidaVozDatabase.getDatabase(context)
    private val reminderPreferencesRepository = ReminderPreferencesRepository(context)
    private val firebaseFirestore = if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseFirestore.getInstance()

    val syncContextRepository = SyncContextRepository(context)
    val firebaseAuthRepository = FirebaseAuthRepository(context)
    private val firestorePatientRepository = FirestorePatientRepository(firebaseFirestore)
    private val firestoreMedicationRepository = FirestoreMedicationRepository(firebaseFirestore)
    private val firestorePressureRepository = FirestorePressureRepository(firebaseFirestore)
    private val firestoreMedicationLogRepository = FirestoreMedicationLogRepository(firebaseFirestore)
    private val firestoreHealthSettingsRepository = FirestoreHealthSettingsRepository(firebaseFirestore)

    val firebaseSyncManager = FirebaseSyncManager(
        context = context,
        database = database,
        syncContextRepository = syncContextRepository,
        authRepository = firebaseAuthRepository,
        patientRepository = firestorePatientRepository,
        medicationRepository = firestoreMedicationRepository,
        pressureRepository = firestorePressureRepository,
        medicationLogRepository = firestoreMedicationLogRepository,
        healthSettingsRepository = firestoreHealthSettingsRepository,
    )
    val patientRepository = PatientRepository(database.patientDao())
    val familyContactRepository = FamilyContactRepository(
        familyContactDao = database.familyContactDao(),
        firebaseSyncManager = firebaseSyncManager,
    )
    val medicationLogRepository = MedicationLogRepository(database.medicationLogDao())
    val medicationReminderRepository = MedicationReminderRepository(database.medicationReminderDao())
    val pressureRepository = PressureRepository(
        bloodPressureDao = database.bloodPressureDao(),
        healthSettingsDao = database.healthSettingsDao(),
        firebaseSyncManager = firebaseSyncManager,
    )
    val settingsRepository = SettingsRepository(
        healthSettingsDao = database.healthSettingsDao(),
        reminderPreferencesRepository = reminderPreferencesRepository,
        firebaseSyncManager = firebaseSyncManager,
    )
    val medicationRepository = MedicationRepository(
        medicationDao = database.medicationDao(),
        firebaseSyncManager = firebaseSyncManager,
    )
    val reminderLaunchState = ReminderLaunchState()
    val notificationHelper = MedicationNotificationHelper(context)
    val medicationImageStorage = MedicationImageStorage(context)
    val pdfReportGenerator = PdfReportGenerator(context)
    val textToSpeechManager = TextToSpeechManager(context)
    val speechRecognitionManager = SpeechRecognitionManager(context)
    val dailyStatusRepository = DailyStatusRepository(
        medicationDao = database.medicationDao(),
        medicationLogDao = database.medicationLogDao(),
        bloodPressureDao = database.bloodPressureDao(),
        firebaseSyncManager = firebaseSyncManager,
    )
    val reminderActionHandler = MedicationReminderActionHandler(
        medicationReminderRepository = medicationReminderRepository,
        medicationRepository = medicationRepository,
        dailyStatusRepository = dailyStatusRepository,
        familyContactRepository = familyContactRepository,
        settingsRepository = settingsRepository,
        firebaseSyncManager = firebaseSyncManager,
    )
    val reminderScheduler = MedicationReminderScheduler(
        context = context,
        medicationRepository = medicationRepository,
        medicationLogRepository = medicationLogRepository,
        medicationReminderRepository = medicationReminderRepository,
        settingsRepository = settingsRepository,
        notificationHelper = notificationHelper,
        dailyStatusRepository = dailyStatusRepository,
        actionHandler = reminderActionHandler,
    ).also(firebaseSyncManager::attachReminderScheduler)
    val medicalReportRepository = MedicalReportRepository(
        patientDao = database.patientDao(),
        familyContactRepository = familyContactRepository,
        healthSettingsDao = database.healthSettingsDao(),
        medicationDao = database.medicationDao(),
        medicationLogDao = database.medicationLogDao(),
        bloodPressureDao = database.bloodPressureDao(),
    )
    val backupRepository = BackupRepository(
        context = context,
        database = database,
        medicationImageStorage = medicationImageStorage,
        reminderPreferencesRepository = reminderPreferencesRepository,
    )

    suspend fun ensureBaselineData() {
        val now = System.currentTimeMillis()
        database.withTransaction {
            database.medicationDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
            database.medicationLogDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
            database.bloodPressureDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
            database.healthSettingsDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
            database.familyContactDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)
            database.medicationReminderDao().reassignBlankPatientIds(DEFAULT_PATIENT_ID)

            val existingPatient = database.patientDao().getPatient(DEFAULT_PATIENT_ID)
                ?: database.patientDao().getMostRecentPatient()
            if (existingPatient == null) {
                database.patientDao().upsert(
                    PatientEntity(
                        id = DEFAULT_PATIENT_ID,
                        fullName = "María Rojas",
                        age = 72,
                        notes = "Paciente de prueba para seguimiento diario offline.",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else if (existingPatient.id != DEFAULT_PATIENT_ID) {
                database.patientDao().upsert(
                    existingPatient.copy(
                        id = DEFAULT_PATIENT_ID,
                        updatedAt = now,
                    ),
                )
            }

            if (database.familyContactDao().getPrimaryContact(DEFAULT_PATIENT_ID) == null) {
                database.familyContactDao().upsertContact(
                    FamilyContactEntity(
                        id = FAMILY_CONTACT_ID,
                        patientId = DEFAULT_PATIENT_ID,
                        fullName = "Juan Rojas",
                        phone = "+51 999 999 999",
                        relationship = "Hijo",
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }

            if (database.healthSettingsDao().getSettings(DEFAULT_PATIENT_ID) == null) {
                database.healthSettingsDao().upsert(
                    HealthSettingsEntity(
                        id = HEALTH_SETTINGS_ID,
                        patientId = DEFAULT_PATIENT_ID,
                        systolicMinNormal = 100,
                        systolicMaxNormal = 130,
                        diastolicMinNormal = 60,
                        diastolicMaxNormal = 85,
                        pulseMinNormal = 60,
                        pulseMaxNormal = 100,
                        doctorRecommendation = "Mantener la presión cerca de 120/80 y avisar si supera 140/90.",
                        updatedAt = now,
                    ),
                )
            }

            if (database.medicationDao().getAllMedications(DEFAULT_PATIENT_ID).isEmpty()) {
                database.medicationDao().insertAll(
                    listOf(
                        MedicationEntity(
                            id = "med_aspirina",
                            patientId = DEFAULT_PATIENT_ID,
                            name = "Aspirina",
                            dose = "1 tableta",
                            color = "Blanca",
                            shape = "Ovalada",
                            instructions = "Tomar despues del desayuno.",
                            scheduleTime = "07:00",
                            imageUri = null,
                            isActive = true,
                            scheduleType = "ALWAYS",
                            startDate = MedicationScheduleDefaults.todayIso(),
                            endDate = null,
                            daysOfWeekJson = MedicationScheduleDefaults.allDaysJson(),
                            specificDatesJson = MedicationScheduleDefaults.emptyDatesJson(),
                            createdAt = now,
                            updatedAt = now,
                        ),
                        MedicationEntity(
                            id = "med_paracetamol",
                            patientId = DEFAULT_PATIENT_ID,
                            name = "Paracetamol",
                            dose = "1 tableta",
                            color = "Blanco",
                            shape = "Redonda",
                            instructions = "Tomar despues del almuerzo.",
                            scheduleTime = "13:45",
                            imageUri = null,
                            isActive = true,
                            scheduleType = "ALWAYS",
                            startDate = MedicationScheduleDefaults.todayIso(),
                            endDate = null,
                            daysOfWeekJson = MedicationScheduleDefaults.allDaysJson(),
                            specificDatesJson = MedicationScheduleDefaults.emptyDatesJson(),
                            createdAt = now,
                            updatedAt = now,
                        ),
                        MedicationEntity(
                            id = "med_losartan",
                            patientId = DEFAULT_PATIENT_ID,
                            name = "Losartan",
                            dose = "1 pastilla",
                            color = "Blanca",
                            shape = "Redonda",
                            instructions = "Tomar con agua despues de la cena.",
                            scheduleTime = "20:00",
                            imageUri = null,
                            isActive = true,
                            scheduleType = "ALWAYS",
                            startDate = MedicationScheduleDefaults.todayIso(),
                            endDate = null,
                            daysOfWeekJson = MedicationScheduleDefaults.allDaysJson(),
                            specificDatesJson = MedicationScheduleDefaults.emptyDatesJson(),
                            createdAt = now,
                            updatedAt = now,
                        ),
                    ),
                )
            }
        }
    }
}
