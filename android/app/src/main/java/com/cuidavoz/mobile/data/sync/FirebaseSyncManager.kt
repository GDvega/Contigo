package com.cuidavoz.mobile.data.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.cuidavoz.mobile.util.ContigoLog
import androidx.room.withTransaction
import com.cuidavoz.mobile.data.backup.BackupRestoreSyncPlan
import com.cuidavoz.mobile.data.files.MedicationImageStorage
import com.cuidavoz.mobile.data.firebase.FirebaseAuthRepository
import com.cuidavoz.mobile.data.firebase.FirestoreHealthSettingsRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationLogRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationRepository
import com.cuidavoz.mobile.data.firebase.FirebaseStorageRepository
import com.cuidavoz.mobile.data.firebase.FirestorePaths
import com.cuidavoz.mobile.data.firebase.FirestorePatientRepository
import com.cuidavoz.mobile.data.firebase.FirestorePressureRepository
import com.cuidavoz.mobile.data.local.ContigoDatabase
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.model.SyncQueueEntity
import com.cuidavoz.mobile.domain.LinkCodeGenerator
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.domain.sync.MedicationImageSyncOperation
import com.cuidavoz.mobile.domain.sync.SyncEntityType
import com.cuidavoz.mobile.domain.sync.SyncOperation
import com.cuidavoz.mobile.domain.sync.SyncStatus
import com.cuidavoz.mobile.reminders.MedicationNotificationHelper
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderPreferences
import com.cuidavoz.mobile.reminders.ReminderPreferencesRepository
import com.cuidavoz.mobile.reminders.VoicePreferences
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.createLocalId
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.io.File
import java.util.UUID

data class LinkCaregiverResult(
    val success: Boolean,
    val message: String,
)

class FirebaseSyncManager(
    private val context: Context,
    private val database: ContigoDatabase,
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
    private val syncContextRepository: SyncContextRepository,
    private val authRepository: FirebaseAuthRepository,
    private val patientRepository: FirestorePatientRepository,
    private val medicationRepository: FirestoreMedicationRepository,
    private val pressureRepository: FirestorePressureRepository,
    private val medicationLogRepository: FirestoreMedicationLogRepository,
    private val healthSettingsRepository: FirestoreHealthSettingsRepository,
    private val storageRepository: FirebaseStorageRepository,
    private val notificationHelper: MedicationNotificationHelper,
    private val linkCodeRateLimiter: LinkCodeRateLimiter,
) : SyncManager {
    private val appContext = context.applicationContext
    private val medicationImageStorage = MedicationImageStorage(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore: FirebaseFirestore? by lazy {
        if (FirebaseApp.getApps(appContext).isEmpty()) null else FirebaseFirestore.getInstance()
    }
    private val listeners = mutableListOf<ListenerRegistration>()
    private var reminderScheduler: MedicationReminderScheduler? = null
    private var alertsListener: ListenerRegistration? = null

    override val syncStatusText: Flow<String> =
        database.syncQueueDao().observePending().map { pending ->
            when {
                !isInternetAvailable() -> "Sin internet. Se sincronizará luego."
                pending.isEmpty() -> "Sincronizado"
                else -> "Pendiente de sincronizar"
            }
        }

    override fun onStart(owner: LifecycleOwner) {
        start()
    }

    override fun onStop(owner: LifecycleOwner) {
        stop()
    }

    override fun start() {
        scope.launch {
            try {
                ensureSignedIn()
                refreshFcmToken()
                syncPendingNow()
                startRealtimeListeners()
            } catch (error: FirebaseException) {
                ContigoLog.w(TAG, "Firebase no está disponible; la app continuará en modo local.", error)
            }
        }
    }

    override fun stop() {
        listeners.forEach { it.remove() }
        listeners.clear()
        alertsListener?.remove()
        alertsListener = null
        scope.launch {
            // Optional: any cleanup needed for in-progress sync
        }
    }

    override fun attachReminderScheduler(scheduler: MedicationReminderScheduler) {
        reminderScheduler = scheduler
    }

    override suspend fun ensureSignedIn(): String? {
        if (!authRepository.isConfigured()) return null
        val uid = authRepository.signInAnonymously()
        syncContextRepository.setFirebaseUserId(uid)
        return uid
    }

    override suspend fun enqueuePatient(patient: PatientEntity) {
        enqueue(
            entityType = SyncEntityType.PATIENT,
            entityId = patient.id,
            operation = SyncOperation.UPDATE,
            payload = JSONObject()
                .put("id", patient.id)
                .put("name", patient.fullName)
                .put("age", patient.age)
                .put("notes", patient.notes)
                .put("updatedAt", patient.updatedAt),
        )
    }

    override suspend fun enqueueMedication(
        medication: MedicationEntity,
        operation: SyncOperation,
        imageSyncOperation: MedicationImageSyncOperation,
    ) {
        enqueue(
            entityType = SyncEntityType.MEDICATION,
            entityId = medication.id,
            operation = operation,
            payload = JSONObject()
                .put("id", medication.id)
                .put("name", medication.name)
                .put("dose", medication.dose)
                .put("color", medication.color)
                .put("shape", medication.shape)
                .put("instructions", medication.instructions)
                .put("scheduleTime", medication.scheduleTime)
                .put("imageUri", medication.imageUri)
                .put("imageSyncOperation", imageSyncOperation.name)
                .put("isActive", medication.isActive)
                .put("scheduleType", medication.scheduleType)
                .put("startDate", medication.startDate)
                .put("endDate", medication.endDate)
                .put("daysOfWeek", org.json.JSONArray(medication.daysOfWeek))
                .put("specificDates", org.json.JSONArray(medication.specificDates.map { it.toString() }))
                .put("createdAt", medication.createdAt)
                .put("updatedAt", medication.updatedAt),
        )
    }

    override suspend fun enqueuePressureReading(reading: BloodPressureEntity) {
        enqueue(
            entityType = SyncEntityType.PRESSURE_READING,
            entityId = reading.id,
            operation = SyncOperation.CREATE,
            payload = JSONObject()
                .put("id", reading.id)
                .put("systolic", reading.systolic)
                .put("diastolic", reading.diastolic)
                .put("pulse", reading.pulse)
                .put("status", reading.status)
                .put("notes", reading.notes)
                .put("measuredAt", reading.measuredAt),
        )
    }

    override suspend fun enqueueDeletePressureReading(reading: BloodPressureEntity) {
        enqueue(
            entityType = SyncEntityType.PRESSURE_READING,
            entityId = reading.id,
            operation = SyncOperation.DELETE,
            payload = JSONObject()
                .put("id", reading.id),
        )
    }

    override suspend fun enqueueMedicationLog(log: MedicationLogEntity) {
        enqueue(
            entityType = SyncEntityType.MEDICATION_LOG,
            entityId = log.id,
            operation = SyncOperation.CREATE,
            payload = JSONObject()
                .put("id", log.id)
                .put("medicationId", log.medicationId)
                .put("scheduledFor", log.scheduledFor)
                .put("takenAt", log.takenAt)
                .put("status", log.status)
                .put("skipReason", log.skipReason)
                .put("createdAt", log.createdAt),
        )
    }

    override suspend fun enqueueHealthSettings(settings: HealthSettingsEntity) {
        enqueue(
            entityType = SyncEntityType.HEALTH_SETTINGS,
            entityId = settings.id,
            operation = SyncOperation.UPDATE,
            payload = JSONObject()
                .put("id", settings.id)
                .put("systolicMinNormal", settings.systolicMinNormal)
                .put("systolicMaxNormal", settings.systolicMaxNormal)
                .put("diastolicMinNormal", settings.diastolicMinNormal)
                .put("diastolicMaxNormal", settings.diastolicMaxNormal)
                .put("pulseMinNormal", settings.pulseMinNormal)
                .put("pulseMaxNormal", settings.pulseMaxNormal)
                .put("doctorRecommendation", settings.doctorRecommendation)
                .put("updatedAt", settings.updatedAt),
        )
    }

    override suspend fun enqueueReminderPreferences(
        reminderPrefs: ReminderPreferences,
        voicePrefs: VoicePreferences,
    ) {
        enqueue(
            entityType = SyncEntityType.REMINDER_PREFERENCES,
            entityId = "reminders",
            operation = SyncOperation.UPDATE,
            payload = JSONObject()
                .put("remindersEnabled", reminderPrefs.remindersEnabled)
                .put("repeatIntervalMinutes", reminderPrefs.repeatIntervalMinutes)
                .put("maxRepeatCount", reminderPrefs.maxRepeatCount)
                .put("soundEnabled", reminderPrefs.soundEnabled)
                .put("vibrationEnabled", reminderPrefs.vibrationEnabled)
                .put("notifyCaregiverOnMissed", reminderPrefs.notifyCaregiverOnMissed)
                .put("voiceAssistantEnabled", voicePrefs.voiceAssistantEnabled)
                .put("voiceReminderEnabled", voicePrefs.voiceReminderEnabled)
                .put("voiceRepeatCount", voicePrefs.voiceRepeatCount)
                .put("easyModeEnabled", voicePrefs.easyModeEnabled)
                .put("voiceGuidanceEnabled", voicePrefs.voiceGuidanceEnabled)
                .put("updatedAt", System.currentTimeMillis()),
        )
    }

    override suspend fun enqueueFamilyContact(contact: FamilyContactEntity) {
        enqueue(
            entityType = SyncEntityType.FAMILY_CONTACT,
            entityId = contact.id,
            operation = SyncOperation.UPDATE,
            payload = JSONObject()
                .put("id", contact.id)
                .put("fullName", contact.fullName)
                .put("phone", contact.phone)
                .put("relationship", contact.relationship)
                .put("updatedAt", contact.updatedAt),
        )
    }

    override suspend fun enqueueBackupRestore(plan: BackupRestoreSyncPlan) {
        val entries = buildBackupRestoreQueueEntries(plan)
        if (entries.isEmpty()) return

        database.withTransaction {
            entries.forEach { entry ->
                database.syncQueueDao().deletePendingOrFailedEquivalent(
                    entityType = entry.entityType,
                    entityId = entry.entityId,
                    operation = entry.operation,
                )
            }
            database.syncQueueDao().upsertAll(entries)
        }
        syncPendingNow()
    }

    override suspend fun enqueueAlert(
        type: String,
        message: String,
        medicationIds: List<String>,
        scheduledAt: Long?,
        severity: String,
    ) {
        enqueue(
            entityType = SyncEntityType.ALERT,
            entityId = createLocalId("alert"),
            operation = SyncOperation.CREATE,
            payload = JSONObject()
                .put("type", type)
                .put("message", message)
                .put("medicationIds", medicationIds)
                .put("scheduledAt", scheduledAt)
                .put("createdAt", System.currentTimeMillis())
                .put("seen", false)
                .put("severity", severity),
        )
    }

    override suspend fun createLinkCode(): String? {
        val db = firestore ?: return null
        return runCatching {
            val uid = ensureSignedIn() ?: return null
            val context = syncContextRepository.getCurrent()
            val patientId = context.patientId ?: DEFAULT_PATIENT_ID
            val familyId = context.familyId ?: "family_${UUID.randomUUID().toString().take(8)}"

            db.document(FirestorePaths.familyDocument(familyId))
                .set(
                    mapOf(
                        "name" to "Familia Contigo",
                        "createdAt" to System.currentTimeMillis(),
                        "createdBy" to uid,
                    ),
                ).await()
            db.document("${FirestorePaths.familyMembersCollection(familyId)}/$uid")
                .set(
                    mapOf(
                        "role" to "patient",
                        "displayName" to "Paciente",
                        "phone" to null,
                        "linkedAt" to System.currentTimeMillis(),
                    ),
                ).await()
            database.patientDao().getPatient(DEFAULT_PATIENT_ID)?.let { patient ->
                patientRepository.upsertPatient(familyId, patient, null)
            }
            pushLocalSnapshot(familyId = familyId, remotePatientId = patientId, userId = uid)

            val code = LinkCodeGenerator.generate()
            db.document(FirestorePaths.linkCodeDocument(code))
                .set(
                    mapOf(
                        "familyId" to familyId,
                        "patientId" to patientId,
                        "expiresAt" to System.currentTimeMillis() + 10 * 60 * 1000,
                        "createdBy" to uid,
                    ),
                ).await()
            syncContextRepository.updateFamilyContext(familyId, patientId, memberRole = "patient")
            refreshFcmToken()
            startRealtimeListeners()
            code
        }.onFailure { error ->
            val detail = if (error is FirebaseFirestoreException) {
                " (${error.code})"
            } else {
                ""
            }
            ContigoLog.w(TAG, "createLinkCode failed$detail", error)
        }.getOrNull()
    }

    override suspend fun linkCaregiver(code: String): LinkCaregiverResult {
        val db = firestore ?: return LinkCaregiverResult(false, "Firebase no está configurado todavía.")
        val trimmedCode = LinkCodeGenerator.normalizeInput(code)
        if (!LinkCodeGenerator.isValid(trimmedCode)) {
            return LinkCaregiverResult(false, "Escribe un código válido.")
        }
        if (linkCodeRateLimiter.isBlocked()) {
            return LinkCaregiverResult(false, linkCodeRateLimiter.blockedMessage())
        }
        return runCatching {
            val uid = ensureSignedIn() ?: return LinkCaregiverResult(false, "No pude iniciar la sesión remota.")
            val inviteRef = db.document(FirestorePaths.linkCodeDocument(trimmedCode))
            val linkedFamilyAndPatient = db.runTransaction { transaction ->
                val inviteSnapshot = transaction.get(inviteRef)
                if (!inviteSnapshot.exists()) {
                    throw IllegalStateException("Ese código no es válido.")
                }
                val expiresAt = inviteSnapshot.getLong("expiresAt") ?: 0L
                if (expiresAt < System.currentTimeMillis()) {
                    throw IllegalStateException("Ese código ya no está disponible.")
                }
                val familyId = inviteSnapshot.getString("familyId").orEmpty()
                val patientId = inviteSnapshot.getString("patientId").orEmpty()
                if (familyId.isBlank()) {
                    throw IllegalStateException("Ese código no es válido.")
                }
                val linkedAt = System.currentTimeMillis()
                val memberRef = db.document("${FirestorePaths.familyMembersCollection(familyId)}/$uid")
                transaction.set(
                    memberRef,
                    mapOf(
                        "role" to "caregiver",
                        "displayName" to "Cuidador",
                        "phone" to null,
                        "linkedAt" to linkedAt,
                        "linkCode" to trimmedCode,
                    ),
                )
                val remotePatientId = patientId.ifBlank { DEFAULT_PATIENT_ID }
                transaction.delete(inviteRef)
                Triple(familyId, remotePatientId, linkedAt)
            }.await()
            val familyId = linkedFamilyAndPatient.first
            val patientId = linkedFamilyAndPatient.second
            val linkedAt = linkedFamilyAndPatient.third
            db.document(FirestorePaths.patientDocument(familyId, patientId))
                .set(
                    mapOf(
                        "mainCaregiverId" to uid,
                        "updatedAt" to linkedAt,
                    ),
                    SetOptions.merge(),
                ).await()
            syncContextRepository.updateFamilyContext(
                familyId,
                patientId.ifBlank { DEFAULT_PATIENT_ID },
                memberRole = "caregiver",
            )
            refreshFcmToken()
            startRealtimeListeners()
            syncPendingNow()
            linkCodeRateLimiter.reset()
            LinkCaregiverResult(true, "Vinculación completada.")
        }.getOrElse {
            val message = linkCaregiverFailureMessage(it)
            ContigoLog.w(TAG, "linkCaregiver failed: $message", it)
            if (message.contains("no es válido", ignoreCase = true) ||
                message.contains("no está disponible", ignoreCase = true)
            ) {
                linkCodeRateLimiter.recordFailure()
            }
            LinkCaregiverResult(false, message)
        }
    }

    private fun linkCaregiverFailureMessage(error: Throwable): String {
        if (error is IllegalStateException && error.message?.isNotBlank() == true) {
            return error.message.orEmpty()
        }
        if (error is FirebaseFirestoreException && error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            return "No tengo permisos para completar la vinculación. Revisa las reglas de Firebase."
        }
        return "No pude completar la vinculación. Intenta otra vez."
    }

    override suspend fun syncPendingNow() {
        if (!authRepository.isConfigured() || !isInternetAvailable()) return
        val context = syncContextRepository.getCurrent()
        if (!context.syncEnabled || context.familyId.isNullOrBlank()) return
        val familyId = context.familyId
        val remotePatientId = context.patientId ?: DEFAULT_PATIENT_ID
        val userId = ensureSignedIn()
        val pending = database.syncQueueDao().getPending()
        var hadFailures = false
        pending.forEach { item ->
            val now = System.currentTimeMillis()
            runCatching {
                database.syncQueueDao().markSyncing(item.id, now)
                val payload = JSONObject(item.payloadJson)
                val entityType = SyncEntityType.valueOf(item.entityType)
                val skipIfRemoteNewer = payload.optBoolean("skipIfRemoteNewer", false)
                when (entityType) {
                    SyncEntityType.PATIENT -> {
                        val restoredUpdatedAt = payload.getLong("updatedAt")
                        if (!shouldSkipRemoteWriteIfRemoteNewer(
                                entityType = entityType,
                                familyId = familyId,
                                remotePatientId = remotePatientId,
                                entityId = payload.getString("id"),
                                restoredUpdatedAt = restoredUpdatedAt,
                                skipIfRemoteNewer = skipIfRemoteNewer,
                            )
                        ) {
                            patientRepository.upsertPatient(
                                familyId = familyId,
                                patient = PatientEntity(
                                    id = payload.getString("id"),
                                    fullName = payload.getString("name"),
                                    age = payload.optInt("age").takeIf { !payload.isNull("age") },
                                    notes = payload.optString("notes").takeIf { it.isNotBlank() },
                                    createdAt = now,
                                    updatedAt = restoredUpdatedAt,
                                ),
                                caregiverUserId = null,
                            )
                        }
                    }
                    SyncEntityType.MEDICATION -> {
                        val medicationId = payload.getString("id")
                        val imageUri = payload.optString("imageUri").takeIf { it.isNotBlank() }
                        val imageSyncOperation = runCatching {
                            MedicationImageSyncOperation.valueOf(payload.optString("imageSyncOperation"))
                        }.getOrDefault(MedicationImageSyncOperation.KEEP)
                        val restoredUpdatedAt = payload.getLong("updatedAt")
                        if (!shouldSkipRemoteWriteIfRemoteNewer(
                                entityType = entityType,
                                familyId = familyId,
                                remotePatientId = remotePatientId,
                                entityId = medicationId,
                                restoredUpdatedAt = restoredUpdatedAt,
                                skipIfRemoteNewer = skipIfRemoteNewer,
                            )
                        ) {
                            val imagePath = when (imageSyncOperation) {
                                MedicationImageSyncOperation.KEEP -> null
                                MedicationImageSyncOperation.UPLOAD -> {
                                    val localFile = checkNotNull(
                                        medicationImageStorage.resolveManagedImageFile(imageUri),
                                    ) {
                                        "No existe una imagen local administrada para subir"
                                    }
                                    check(localFile.exists()) { "La imagen local administrada ya no existe" }
                                    storageRepository.uploadMedicationImage(
                                        familyId = familyId,
                                        patientId = remotePatientId,
                                        medicationId = medicationId,
                                        localFile = localFile,
                                    )
                                }
                                MedicationImageSyncOperation.DELETE -> {
                                    storageRepository.deleteMedicationImage(
                                        familyId = familyId,
                                        patientId = remotePatientId,
                                        medicationId = medicationId,
                                    )
                                    null
                                }
                            }

                            medicationRepository.upsertMedication(
                                familyId = familyId,
                                patientId = remotePatientId,
                                medication = MedicationEntity(
                                    id = medicationId,
                                    patientId = DEFAULT_PATIENT_ID,
                                    name = payload.getString("name"),
                                    dose = payload.getString("dose"),
                                    color = payload.optString("color").takeIf { it.isNotBlank() },
                                    shape = payload.optString("shape").takeIf { it.isNotBlank() },
                                    instructions = payload.optString("instructions").takeIf { it.isNotBlank() },
                                    scheduleTime = payload.getString("scheduleTime"),
                                    imageUri = imageUri,
                                    isActive = payload.getBoolean("isActive"),
                                    scheduleType = payload.optString("scheduleType").ifBlank { "ALWAYS" },
                                    startDate = payload.optString("startDate").ifBlank { MedicationScheduleDefaults.todayIso() },
                                    endDate = payload.optString("endDate").takeIf { it.isNotBlank() && it != "null" },
                                    daysOfWeek = payload.optJSONArray("daysOfWeek")?.let { array ->
                                        List(array.length()) { array.getInt(it) }
                                    } ?: MedicationScheduleDefaults.allDaysOfWeek.toList(),
                                    specificDates = payload.optJSONArray("specificDates")?.let { array ->
                                        List(array.length()) { LocalDate.parse(array.getString(it)) }
                                    } ?: emptyList(),
                                    createdAt = payload.optLong("createdAt").takeIf { it > 0 } ?: now,
                                    updatedAt = restoredUpdatedAt,
                                ),
                                updatedBy = userId,
                                imageSyncOperation = imageSyncOperation,
                                imagePath = imagePath,
                            )
                        }
                    }
                    SyncEntityType.PRESSURE_READING -> {
                        if (SyncOperation.valueOf(item.operation) == SyncOperation.DELETE) {
                            pressureRepository.deletePressureReading(
                                familyId = familyId,
                                patientId = remotePatientId,
                                readingId = payload.getString("id"),
                            )
                        } else {
                            val reading = BloodPressureEntity(
                                id = payload.getString("id"),
                                patientId = DEFAULT_PATIENT_ID,
                                systolic = payload.getInt("systolic"),
                                diastolic = payload.getInt("diastolic"),
                                pulse = payload.optInt("pulse").takeIf { !payload.isNull("pulse") },
                                status = payload.getString("status"),
                                notes = payload.optString("notes").takeIf { it.isNotBlank() },
                                measuredAt = payload.getLong("measuredAt"),
                                createdAt = now,
                            )
                            pressureRepository.createPressureReading(familyId, remotePatientId, reading, userId)
                            if (reading.status == "HIGH" || reading.status == "CRITICAL" || reading.status == "OUT_OF_RANGE") {
                                firestore?.collection(FirestorePaths.alertsCollection(familyId, remotePatientId))
                                    ?.document(reading.id)
                                    ?.set(
                                        mapOf(
                                            "type" to "pressure_alert",
                                            "message" to "Se registró una presión fuera de rango.",
                                            "createdAt" to System.currentTimeMillis(),
                                            "seen" to false,
                                        ),
                                    )?.await()
                            }
                        }
                    }
                    SyncEntityType.MEDICATION_LOG -> {
                        medicationLogRepository.createMedicationLog(
                            familyId = familyId,
                            patientId = remotePatientId,
                            log = MedicationLogEntity(
                                id = payload.getString("id"),
                                medicationId = payload.getString("medicationId"),
                                patientId = DEFAULT_PATIENT_ID,
                                scheduledFor = payload.getLong("scheduledFor"),
                                takenAt = payload.optLong("takenAt").takeIf { !payload.isNull("takenAt") },
                                status = payload.getString("status"),
                                skipReason = payload.optString("skipReason").takeIf { it.isNotBlank() },
                                createdAt = payload.getLong("createdAt"),
                            ),
                            createdBy = userId,
                        )
                    }
                    SyncEntityType.HEALTH_SETTINGS -> {
                        val restoredUpdatedAt = payload.getLong("updatedAt")
                        if (!shouldSkipRemoteWriteIfRemoteNewer(
                                entityType = entityType,
                                familyId = familyId,
                                remotePatientId = remotePatientId,
                                entityId = payload.getString("id"),
                                restoredUpdatedAt = restoredUpdatedAt,
                                skipIfRemoteNewer = skipIfRemoteNewer,
                            )
                        ) {
                            healthSettingsRepository.upsertHealthSettings(
                                familyId = familyId,
                                patientId = remotePatientId,
                                settings = HealthSettingsEntity(
                                    id = payload.getString("id"),
                                    patientId = DEFAULT_PATIENT_ID,
                                    systolicMinNormal = payload.getInt("systolicMinNormal"),
                                    systolicMaxNormal = payload.getInt("systolicMaxNormal"),
                                    diastolicMinNormal = payload.getInt("diastolicMinNormal"),
                                    diastolicMaxNormal = payload.getInt("diastolicMaxNormal"),
                                    pulseMinNormal = payload.getInt("pulseMinNormal"),
                                    pulseMaxNormal = payload.getInt("pulseMaxNormal"),
                                    doctorRecommendation = payload.optString("doctorRecommendation").takeIf { it.isNotBlank() },
                                    updatedAt = restoredUpdatedAt,
                                ),
                            )
                        }
                    }
                    SyncEntityType.REMINDER_PREFERENCES -> {
                        healthSettingsRepository.upsertReminderPreferences(
                            familyId = familyId,
                            patientId = remotePatientId,
                            reminderPrefs = ReminderPreferences(
                                remindersEnabled = payload.getBoolean("remindersEnabled"),
                                repeatIntervalMinutes = payload.getInt("repeatIntervalMinutes"),
                                maxRepeatCount = payload.getInt("maxRepeatCount"),
                                soundEnabled = payload.getBoolean("soundEnabled"),
                                vibrationEnabled = payload.getBoolean("vibrationEnabled"),
                                notifyCaregiverOnMissed = payload.getBoolean("notifyCaregiverOnMissed"),
                            ),
                            voicePrefs = VoicePreferences(
                                voiceAssistantEnabled = payload.getBoolean("voiceAssistantEnabled"),
                                voiceReminderEnabled = payload.getBoolean("voiceReminderEnabled"),
                                voiceRepeatCount = payload.getInt("voiceRepeatCount"),
                                easyModeEnabled = payload.getBoolean("easyModeEnabled"),
                                voiceGuidanceEnabled = payload.getBoolean("voiceGuidanceEnabled"),
                            ),
                            updatedAt = payload.getLong("updatedAt"),
                        )
                    }
                    SyncEntityType.ALERT -> {
                        val alertData = mutableMapOf<String, Any?>(
                            "type" to payload.getString("type"),
                            "message" to payload.getString("message"),
                            "createdAt" to payload.getLong("createdAt"),
                            "seen" to payload.optBoolean("seen", false),
                            "severity" to payload.optString("severity").ifBlank { "medium" },
                        )
                        if (!payload.isNull("scheduledAt")) {
                            alertData["scheduledAt"] = payload.getLong("scheduledAt")
                        }
                        if (payload.has("medicationIds")) {
                            val medicationIds = payload.getJSONArray("medicationIds")
                            alertData["medicationIds"] = List(medicationIds.length()) { index ->
                                medicationIds.getString(index)
                            }
                        }
                        firestore?.collection(FirestorePaths.alertsCollection(familyId, remotePatientId))
                            ?.document(item.entityId)
                            ?.set(alertData)
                            ?.await()
                    }
                    SyncEntityType.FAMILY_CONTACT -> {
                        val restoredUpdatedAt = payload.getLong("updatedAt")
                        if (!shouldSkipRemoteWriteIfRemoteNewer(
                                entityType = entityType,
                                familyId = familyId,
                                remotePatientId = remotePatientId,
                                entityId = payload.getString("id"),
                                restoredUpdatedAt = restoredUpdatedAt,
                                skipIfRemoteNewer = skipIfRemoteNewer,
                            )
                        ) {
                            healthSettingsRepository.upsertFamilyContact(
                                familyId = familyId,
                                patientId = remotePatientId,
                                contact = FamilyContactEntity(
                                    id = payload.getString("id"),
                                    patientId = DEFAULT_PATIENT_ID,
                                    fullName = payload.getString("fullName"),
                                    phone = payload.getString("phone"),
                                    relationship = payload.optString("relationship").takeIf { it.isNotBlank() },
                                    createdAt = now,
                                    updatedAt = restoredUpdatedAt,
                                ),
                            )
                        }
                    }
                    SyncEntityType.LINK_CODE -> Unit
                }
                database.syncQueueDao().markSynced(item.id, System.currentTimeMillis())
            }.onFailure { error ->
                hadFailures = true
                ContigoLog.w(TAG, "sync failed for ${item.entityType}/${item.entityId}", error)
                database.syncQueueDao().markFailed(item.id, error.message, System.currentTimeMillis())
            }
        }
        if (!hadFailures) {
            syncContextRepository.markSynced(System.currentTimeMillis())
        }
        database.syncQueueDao().deleteSyncedOlderThan(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L)
    }

    private suspend fun shouldSkipRemoteWriteIfRemoteNewer(
        entityType: SyncEntityType,
        familyId: String,
        remotePatientId: String,
        entityId: String,
        restoredUpdatedAt: Long,
        skipIfRemoteNewer: Boolean,
    ): Boolean {
        if (!skipIfRemoteNewer) return false
        val remoteUpdatedAt = when (entityType) {
            SyncEntityType.PATIENT -> patientRepository.fetchPatientUpdatedAtFromServer(familyId, entityId)
            SyncEntityType.MEDICATION -> medicationRepository.fetchMedicationUpdatedAtFromServer(
                familyId = familyId,
                patientId = remotePatientId,
                medicationId = entityId,
            )
            SyncEntityType.HEALTH_SETTINGS -> healthSettingsRepository.fetchHealthSettingsUpdatedAtFromServer(
                familyId = familyId,
                patientId = remotePatientId,
            )
            SyncEntityType.FAMILY_CONTACT -> healthSettingsRepository.fetchFamilyContactUpdatedAtFromServer(
                familyId = familyId,
                patientId = remotePatientId,
            )
            else -> null
        }

        if (remoteUpdatedAt == null || remoteUpdatedAt <= restoredUpdatedAt) return false
        ContigoLog.w(
            TAG,
            "restore skipped for ${entityType.name}/$entityId: remote updatedAt $remoteUpdatedAt is newer than restored $restoredUpdatedAt",
        )
        return true
    }

    fun startRealtimeListeners() {
        listeners.forEach { it.remove() }
        listeners.clear()
        scope.launch {
            val context = syncContextRepository.getCurrent()
            val familyId = context.familyId ?: return@launch
            val remotePatientId = context.patientId ?: DEFAULT_PATIENT_ID
            val shouldScheduleLocalReminders = context.memberRole != "caregiver"

            patientRepository.listenToPatient(familyId, remotePatientId) { data ->
                if (data == null) return@listenToPatient
                scope.launch {
                    val patient = PatientEntity(
                        id = DEFAULT_PATIENT_ID,
                        fullName = data["name"]?.toString().orEmpty().ifBlank { "Paciente" },
                        age = (data["age"] as? Number)?.toInt(),
                        notes = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    )
                    database.patientDao().upsert(patient)
                }
            }?.let(listeners::add)

            medicationRepository.listenToMedications(familyId, remotePatientId) { remoteMedications ->
                scope.launch {
                    val remoteIds = remoteMedications.mapNotNull { it["id"]?.toString() }.toSet()
                    remoteMedications.forEach { item ->
                        val medicationId = item["id"]?.toString().orEmpty()
                        if (medicationId.isBlank()) return@forEach
                        val local = database.medicationDao().getMedicationById(medicationId)
                        val remoteUpdatedAt = (item["updatedAt"] as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                        val hasRemoteImagePath = item.containsKey("imagePath")
                        val remoteImagePath = item["imagePath"]
                        val remoteImagePathString = remoteImagePath as? String
                        val needsImageRetry = !remoteImagePathString.isNullOrBlank() &&
                            local?.imageUri.isNullOrBlank()
                        if (local != null && remoteUpdatedAt < local.updatedAt && !needsImageRetry) {
                            return@forEach
                        }

                        val downloadedImageUri = remoteImagePathString
                            ?.takeIf(String::isNotBlank)
                            ?.let { imagePath ->
                                downloadRemoteMedicationImageWithRetry(imagePath, medicationId)
                            }

                        if (local != null && remoteUpdatedAt < local.updatedAt) {
                            if (downloadedImageUri != null) {
                                database.medicationDao().updateMedicationImage(
                                    medicationId,
                                    downloadedImageUri,
                                    local.updatedAt,
                                )
                            }
                            return@forEach
                        }
                        val resolvedImageUri = when {
                            !hasRemoteImagePath -> local?.imageUri
                            remoteImagePath == null -> null
                            !remoteImagePathString.isNullOrBlank() ->
                                downloadedImageUri ?: local?.imageUri
                            else -> local?.imageUri
                        }
                        val medication = MedicationEntity(
                            id = medicationId,
                            patientId = DEFAULT_PATIENT_ID,
                            name = item["name"]?.toString().orEmpty(),
                            dose = item["dose"]?.toString().orEmpty(),
                            color = item["color"]?.toString(),
                            shape = item["shape"]?.toString(),
                            instructions = item["instructions"]?.toString(),
                            scheduleTime = item["time24"]?.toString().orEmpty(),
                            imageUri = resolvedImageUri,
                            isActive = item["active"] as? Boolean ?: true,
                            scheduleType = item["scheduleType"]?.toString().orEmpty().ifBlank { "ALWAYS" },
                            startDate = item["startDate"].toIsoDateOrDefault(),
                            endDate = item["endDate"].toIsoDateOrNull(),
                            daysOfWeek = (item["daysOfWeek"] as? List<*>)?.mapNotNull { (it as? Number)?.toInt() }
                                ?: MedicationScheduleDefaults.allDaysOfWeek.toList(),
                            specificDates = (item["specificDates"] as? List<*>)?.mapNotNull { runCatching { LocalDate.parse(it.toString()) }.getOrNull() }
                                ?: emptyList(),
                            createdAt = (item["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = remoteUpdatedAt,
                        )
                        database.medicationDao().upsert(medication)
                        when {
                            remoteImagePath == null && hasRemoteImagePath -> {
                                medicationImageStorage.deleteManagedMedicationImage(local?.imageUri)
                            }
                            downloadedImageUri != null && downloadedImageUri != local?.imageUri -> {
                                medicationImageStorage.deleteManagedMedicationImage(local?.imageUri)
                            }
                        }
                    }
                    if (context.memberRole == "caregiver" && remoteIds.isNotEmpty()) {
                        database.medicationDao().getAllMedications(DEFAULT_PATIENT_ID)
                            .filter { it.id !in remoteIds }
                            .forEach { stale ->
                                database.medicationDao().softDelete(stale.id, System.currentTimeMillis())
                            }
                    }
                    if (shouldScheduleLocalReminders) {
                        reminderScheduler?.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
                    }
                }
            }?.let(listeners::add)

            if (context.memberRole == "caregiver") {
                alertsListener?.remove()
                alertsListener = firestore?.collection(FirestorePaths.alertsCollection(familyId, remotePatientId))
                    ?.whereEqualTo("seen", false)
                    ?.addSnapshotListener { snapshot, _ ->
                        snapshot?.documentChanges?.filter {
                            it.type == com.google.firebase.firestore.DocumentChange.Type.ADDED
                        }?.forEach { change ->
                            val alert = change.document.data
                            val message = alert["message"]?.toString() ?: "Alerta del paciente"
                            val type = alert["type"]?.toString() ?: "alert"

                            notificationHelper.showConfirmationNotification(
                                message = message,
                                payload = com.cuidavoz.mobile.reminders.ReminderPayload(
                                    reminderGroupId = "alert_${change.document.id}",
                                    patientId = remotePatientId,
                                    scheduleTime = "",
                                    targetDate = "",
                                    scheduledAt = System.currentTimeMillis(),
                                    medicationIds = emptyList(),
                                    medicationNames = emptyList(),
                                    attemptNumber = 1,
                                    maxAttempts = 1,
                                    repeatEveryMinutes = 0
                                )
                            )
                        }
                    }
            }

            healthSettingsRepository.listenToHealthSettings(familyId, remotePatientId) { data ->
                if (data == null) return@listenToHealthSettings
                scope.launch {
                    val settings = HealthSettingsEntity(
                        id = "health_settings_remote",
                        patientId = DEFAULT_PATIENT_ID,
                        systolicMinNormal = (data["systolicMin"] as? Number)?.toInt() ?: 100,
                        systolicMaxNormal = (data["systolicMax"] as? Number)?.toInt() ?: 130,
                        diastolicMinNormal = (data["diastolicMin"] as? Number)?.toInt() ?: 60,
                        diastolicMaxNormal = (data["diastolicMax"] as? Number)?.toInt() ?: 85,
                        pulseMinNormal = (data["pulseMin"] as? Number)?.toInt() ?: 60,
                        pulseMaxNormal = (data["pulseMax"] as? Number)?.toInt() ?: 100,
                        doctorRecommendation = data["doctorRecommendation"]?.toString(),
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    )
                    val local = database.healthSettingsDao().getSettings(DEFAULT_PATIENT_ID)
                    if (local == null || settings.updatedAt >= local.updatedAt) {
                        database.healthSettingsDao().upsert(settings)
                    }
                }
            }?.let(listeners::add)

            healthSettingsRepository.listenToReminderPreferences(familyId, remotePatientId) { data ->
                if (data == null) return@listenToReminderPreferences
                scope.launch {
                    val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
                    val currentVoicePrefs = reminderPreferencesRepository.getCurrentVoicePreferences()
                    // We don't have a local "last updated" for DataStore easily,
                    // but we can assume remote wins if it's recent or handle conflicts later.
                    // For now, simple override if remote data is present.
                    reminderPreferencesRepository.setAllPreferences(
                        remindersEnabled = data["remindersEnabled"] as? Boolean ?: false,
                        repeatIntervalMinutes = (data["repeatIntervalMinutes"] as? Number)?.toInt() ?: 10,
                        maxRepeatCount = (data["maxRepeatCount"] as? Number)?.toInt() ?: 3,
                        soundEnabled = data["soundEnabled"] as? Boolean ?: true,
                        vibrationEnabled = data["vibrationEnabled"] as? Boolean ?: true,
                        notifyCaregiverOnMissed = data["notifyCaregiverOnMissed"] as? Boolean ?: true,
                        voiceAssistantEnabled = data["voiceAssistantEnabled"] as? Boolean
                            ?: currentVoicePrefs.voiceAssistantEnabled,
                        voiceReminderEnabled = data["voiceReminderEnabled"] as? Boolean ?: false,
                        voiceRepeatCount = (data["voiceRepeatCount"] as? Number)?.toInt() ?: 2,
                        easyModeEnabled = data["easyModeEnabled"] as? Boolean ?: true,
                        voiceGuidanceEnabled = data["voiceGuidanceEnabled"] as? Boolean ?: false,
                    )
                }
            }?.let(listeners::add)

            firestore?.document(FirestorePaths.patientDocument(familyId, remotePatientId) + "/contact/main")
                ?.addSnapshotListener { snapshot, _ ->
                    val data = snapshot?.data ?: return@addSnapshotListener
                    scope.launch {
                        database.familyContactDao().upsertContact(
                            FamilyContactEntity(
                                id = "family_contact_remote",
                                patientId = DEFAULT_PATIENT_ID,
                                fullName = data["fullName"]?.toString().orEmpty(),
                                phone = data["phone"]?.toString().orEmpty(),
                                relationship = data["relationship"]?.toString(),
                                createdAt = System.currentTimeMillis(),
                                updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            ),
                        )
                    }
                }?.let(listeners::add)

            backfillHistoricalData(familyId, remotePatientId, context.lastSyncAt)

            firestore?.collection(FirestorePaths.pressureCollection(familyId, remotePatientId))
                ?.orderBy("measuredAt", Query.Direction.DESCENDING)
                ?.limit(50)
                ?.addSnapshotListener { snapshot, _ ->
                    val changes = snapshot?.documentChanges.orEmpty()
                    scope.launch {
                        changes.forEach { change ->
                            val doc = change.document
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                val existsOnServer = runCatching {
                                    doc.reference.get(Source.SERVER).await().exists()
                                }.onFailure { error ->
                                    ContigoLog.w(TAG, "Could not verify removed pressure reading ${doc.id}", error)
                                }.getOrNull()
                                if (existsOnServer != false) return@forEach

                                val createPending = database.syncQueueDao().hasPendingOperation(
                                    entityType = SyncEntityType.PRESSURE_READING.name,
                                    entityId = doc.id,
                                    operation = SyncOperation.CREATE.name,
                                )
                                if (!createPending) {
                                    database.bloodPressureDao().deleteById(doc.id)
                                }
                                return@forEach
                            }

                            val deletePending = database.syncQueueDao().hasPendingOperation(
                                entityType = SyncEntityType.PRESSURE_READING.name,
                                entityId = doc.id,
                                operation = SyncOperation.DELETE.name,
                            )
                            if (deletePending) return@forEach
                            val local = database.bloodPressureDao().getById(doc.id)
                            val measuredAt = doc.getLong("measuredAt") ?: return@forEach
                            if (local == null || measuredAt >= local.measuredAt) {
                                database.bloodPressureDao().insert(
                                    BloodPressureEntity(
                                        id = doc.id,
                                        patientId = DEFAULT_PATIENT_ID,
                                        systolic = doc.getLong("systolic")?.toInt() ?: 0,
                                        diastolic = doc.getLong("diastolic")?.toInt() ?: 0,
                                        pulse = doc.getLong("pulse")?.toInt(),
                                        status = doc.getString("status").orEmpty(),
                                        notes = doc.getString("note"),
                                        measuredAt = measuredAt,
                                        createdAt = measuredAt,
                                    ),
                                )
                            }
                        }
                    }
                }?.let(listeners::add)

            firestore?.collection(FirestorePaths.medicationLogsCollection(familyId, remotePatientId))
                ?.orderBy("scheduledAt", Query.Direction.DESCENDING)
                ?.limit(200)
                ?.addSnapshotListener { snapshot, _ ->
                    val docs = snapshot?.documents.orEmpty()
                    scope.launch {
                        docs.forEach { doc ->
                            val log = MedicationLogEntity(
                                id = doc.id,
                                medicationId = doc.getString("medicationId").orEmpty(),
                                patientId = DEFAULT_PATIENT_ID,
                                scheduledFor = doc.getLong("scheduledAt") ?: 0L,
                                takenAt = doc.getLong("takenAt"),
                                status = doc.getString("status").orEmpty(),
                                skipReason = doc.getString("skipReason")?.takeIf { it.isNotBlank() },
                                createdAt = doc.getLong("syncedAt") ?: System.currentTimeMillis(),
                            )
                            database.medicationLogDao().insert(log)
                        }
                    }
                }?.let(listeners::add)
        }
    }

    private suspend fun downloadRemoteMedicationImageWithRetry(
        imagePath: String,
        medicationId: String,
    ): String? {
        repeat(REMOTE_IMAGE_DOWNLOAD_MAX_ATTEMPTS) { attempt ->
            val downloaded = downloadRemoteMedicationImage(imagePath, medicationId)
            if (downloaded != null) return downloaded
            if (attempt < REMOTE_IMAGE_DOWNLOAD_MAX_ATTEMPTS - 1) {
                delay(REMOTE_IMAGE_DOWNLOAD_RETRY_DELAY_MS)
            }
        }
        return null
    }

    private suspend fun downloadRemoteMedicationImage(
        imagePath: String,
        medicationId: String,
    ): String? {
        var tempFile: File? = null
        return try {
            val createdTempFile = medicationImageStorage.createDownloadedImageTempFile(medicationId)
            tempFile = createdTempFile
            val downloaded = storageRepository.downloadMedicationImage(imagePath, createdTempFile)
            if (!downloaded) return null
            medicationImageStorage.commitDownloadedImage(createdTempFile, medicationId)
        } catch (error: Throwable) {
            ContigoLog.w(TAG, "Could not download medication image $medicationId from $imagePath", error)
            null
        } finally {
            tempFile?.takeIf(File::exists)?.delete()
        }
    }

    private suspend fun backfillHistoricalData(
        familyId: String,
        remotePatientId: String,
        sinceSyncedAt: Long?,
    ) {
        runCatching {
            backfillPressureReadings(familyId, remotePatientId, sinceSyncedAt)
        }.onFailure { error ->
            ContigoLog.w(TAG, "Pressure history backfill failed", error)
        }
        runCatching {
            backfillMedicationLogs(familyId, remotePatientId, sinceSyncedAt)
        }.onFailure { error ->
            ContigoLog.w(TAG, "Medication log history backfill failed", error)
        }
    }

    private suspend fun backfillPressureReadings(
        familyId: String,
        remotePatientId: String,
        sinceSyncedAt: Long?,
    ) {
        var cursor: com.google.firebase.firestore.DocumentSnapshot? = null
        while (true) {
            val page = pressureRepository.fetchPressureReadingsPage(
                familyId = familyId,
                patientId = remotePatientId,
                pageSize = BACKFILL_PAGE_SIZE,
                after = cursor,
                sinceSyncedAt = sinceSyncedAt,
            )
            if (page.documents.isEmpty()) return

            val readings = page.documents.mapNotNull { doc ->
                val deletePending = database.syncQueueDao().hasPendingOperation(
                    entityType = SyncEntityType.PRESSURE_READING.name,
                    entityId = doc.id,
                    operation = SyncOperation.DELETE.name,
                )
                if (deletePending) return@mapNotNull null

                val measuredAt = doc.getLong("measuredAt") ?: return@mapNotNull null
                val local = database.bloodPressureDao().getById(doc.id)
                if (local != null && measuredAt < local.measuredAt) return@mapNotNull null

                BloodPressureEntity(
                    id = doc.id,
                    patientId = DEFAULT_PATIENT_ID,
                    systolic = doc.getLong("systolic")?.toInt() ?: 0,
                    diastolic = doc.getLong("diastolic")?.toInt() ?: 0,
                    pulse = doc.getLong("pulse")?.toInt(),
                    status = doc.getString("status").orEmpty(),
                    notes = doc.getString("note"),
                    measuredAt = measuredAt,
                    createdAt = measuredAt,
                )
            }
            if (readings.isNotEmpty()) {
                database.bloodPressureDao().insertAll(readings)
            }

            val nextCursor = page.nextCursor
            if (page.documents.size < BACKFILL_PAGE_SIZE || nextCursor == null || nextCursor.id == cursor?.id) return
            cursor = nextCursor
        }
    }

    private suspend fun backfillMedicationLogs(
        familyId: String,
        remotePatientId: String,
        sinceSyncedAt: Long?,
    ) {
        var cursor: com.google.firebase.firestore.DocumentSnapshot? = null
        while (true) {
            val page = medicationLogRepository.fetchMedicationLogsPage(
                familyId = familyId,
                patientId = remotePatientId,
                pageSize = BACKFILL_PAGE_SIZE,
                after = cursor,
                sinceSyncedAt = sinceSyncedAt,
            )
            if (page.documents.isEmpty()) return

            database.medicationLogDao().insertAll(
                page.documents.map { doc ->
                    MedicationLogEntity(
                        id = doc.id,
                        medicationId = doc.getString("medicationId").orEmpty(),
                        patientId = DEFAULT_PATIENT_ID,
                        scheduledFor = doc.getLong("scheduledAt") ?: 0L,
                        takenAt = doc.getLong("takenAt"),
                        status = doc.getString("status").orEmpty(),
                        skipReason = doc.getString("skipReason")?.takeIf { it.isNotBlank() },
                        createdAt = doc.getLong("syncedAt") ?: System.currentTimeMillis(),
                    )
                },
            )

            val nextCursor = page.nextCursor
            if (page.documents.size < BACKFILL_PAGE_SIZE || nextCursor == null || nextCursor.id == cursor?.id) return
            cursor = nextCursor
        }
    }

    private suspend fun enqueue(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncOperation,
        payload: JSONObject,
    ) {
        database.syncQueueDao().upsert(
            createQueueEntity(entityType, entityId, operation, payload),
        )
        scope.launch { syncPendingNow() }
    }

    private fun buildBackupRestoreQueueEntries(plan: BackupRestoreSyncPlan): List<SyncQueueEntity> {
        val entries = mutableListOf<SyncQueueEntity>()
        fun add(
            entityType: SyncEntityType,
            entityId: String,
            operation: SyncOperation,
            payload: JSONObject,
            skipIfRemoteNewer: Boolean = false,
        ) {
            payload.put("restoreStrategy", plan.strategy.name)
            if (skipIfRemoteNewer) {
                payload.put("skipIfRemoteNewer", true)
            }
            entries += createQueueEntity(entityType, entityId, operation, payload)
        }

        plan.patient?.let { patient ->
            add(
                SyncEntityType.PATIENT,
                patient.id,
                SyncOperation.UPDATE,
                JSONObject()
                    .put("id", patient.id)
                    .put("name", patient.fullName)
                    .put("age", patient.age)
                    .put("notes", patient.notes)
                    .put("updatedAt", patient.updatedAt),
                skipIfRemoteNewer = true,
            )
        }
        plan.familyContact?.let { contact ->
            add(
                SyncEntityType.FAMILY_CONTACT,
                contact.id,
                SyncOperation.UPDATE,
                JSONObject()
                    .put("id", contact.id)
                    .put("fullName", contact.fullName)
                    .put("phone", contact.phone)
                    .put("relationship", contact.relationship)
                    .put("updatedAt", contact.updatedAt),
                skipIfRemoteNewer = true,
            )
        }
        plan.healthSettings?.let { settings ->
            add(
                SyncEntityType.HEALTH_SETTINGS,
                settings.id,
                SyncOperation.UPDATE,
                JSONObject()
                    .put("id", settings.id)
                    .put("systolicMinNormal", settings.systolicMinNormal)
                    .put("systolicMaxNormal", settings.systolicMaxNormal)
                    .put("diastolicMinNormal", settings.diastolicMinNormal)
                    .put("diastolicMaxNormal", settings.diastolicMaxNormal)
                    .put("pulseMinNormal", settings.pulseMinNormal)
                    .put("pulseMaxNormal", settings.pulseMaxNormal)
                    .put("doctorRecommendation", settings.doctorRecommendation)
                    .put("updatedAt", settings.updatedAt),
                skipIfRemoteNewer = true,
            )
        }
        plan.medications.forEach { restored ->
            val medication = restored.medication
            add(
                SyncEntityType.MEDICATION,
                medication.id,
                SyncOperation.UPDATE,
                JSONObject()
                    .put("id", medication.id)
                    .put("name", medication.name)
                    .put("dose", medication.dose)
                    .put("color", medication.color)
                    .put("shape", medication.shape)
                    .put("instructions", medication.instructions)
                    .put("scheduleTime", medication.scheduleTime)
                    .put("imageUri", medication.imageUri)
                    .put("imageSyncOperation", restored.imageOperation.name)
                    .put("isActive", medication.isActive)
                    .put("scheduleType", medication.scheduleType)
                    .put("startDate", medication.startDate)
                    .put("endDate", medication.endDate)
                    .put("daysOfWeek", org.json.JSONArray(medication.daysOfWeek))
                    .put("specificDates", org.json.JSONArray(medication.specificDates.map { it.toString() }))
                    .put("createdAt", medication.createdAt)
                    .put("updatedAt", medication.updatedAt),
                skipIfRemoteNewer = true,
            )
        }
        plan.pressureReadings.forEach { reading ->
            add(
                SyncEntityType.PRESSURE_READING,
                reading.id,
                SyncOperation.CREATE,
                JSONObject()
                    .put("id", reading.id)
                    .put("systolic", reading.systolic)
                    .put("diastolic", reading.diastolic)
                    .put("pulse", reading.pulse)
                    .put("status", reading.status)
                    .put("notes", reading.notes)
                    .put("measuredAt", reading.measuredAt),
            )
        }
        plan.medicationLogs.forEach { log ->
            add(
                SyncEntityType.MEDICATION_LOG,
                log.id,
                SyncOperation.CREATE,
                JSONObject()
                    .put("id", log.id)
                    .put("medicationId", log.medicationId)
                    .put("scheduledFor", log.scheduledFor)
                    .put("takenAt", log.takenAt)
                    .put("status", log.status)
                    .put("skipReason", log.skipReason)
                    .put("createdAt", log.createdAt),
            )
        }
        return entries
    }

    private fun createQueueEntity(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncOperation,
        payload: JSONObject,
    ): SyncQueueEntity {
        val now = System.currentTimeMillis()
        return SyncQueueEntity(
            id = createLocalId("sync_queue"),
            entityType = entityType.name,
            entityId = entityId,
            operation = operation.name,
            payloadJson = payload.toString(),
            status = SyncStatus.PENDING.name,
            retryCount = 0,
            lastError = null,
            createdAt = now,
            updatedAt = now,
        )
    }

    private suspend fun refreshFcmToken() {
        val context = syncContextRepository.getCurrent()
        val familyId = context.familyId ?: return
        val uid = authRepository.getCurrentUserId() ?: return
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            firestore
                ?.document("${FirestorePaths.familyMembersCollection(familyId)}/$uid")
                ?.set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                ?.await()
        }
    }

    private suspend fun pushLocalSnapshot(
        familyId: String,
        remotePatientId: String,
        userId: String?,
    ) {
        database.patientDao().getPatient(DEFAULT_PATIENT_ID)?.let { patient ->
            patientRepository.upsertPatient(familyId, patient, caregiverUserId = null)
        }
        database.medicationDao().getAllMedications(DEFAULT_PATIENT_ID).forEach { medication ->
            medicationRepository.upsertMedication(familyId, remotePatientId, medication, userId)
        }
        database.healthSettingsDao().getSettings(DEFAULT_PATIENT_ID)?.let { settings ->
            healthSettingsRepository.upsertHealthSettings(familyId, remotePatientId, settings)
        }
        database.familyContactDao().getPrimaryContact(DEFAULT_PATIENT_ID)?.let { contact ->
            healthSettingsRepository.upsertFamilyContact(familyId, remotePatientId, contact)
        }
        uploadLocalPressureHistory(familyId, remotePatientId, userId)
        uploadLocalMedicationLogHistory(familyId, remotePatientId, userId)
        enqueueReminderPreferences(
            reminderPreferencesRepository.getCurrentPreferences(),
            reminderPreferencesRepository.getCurrentVoicePreferences(),
        )
    }

    private suspend fun uploadLocalPressureHistory(
        familyId: String,
        remotePatientId: String,
        userId: String?,
    ) {
        var beforeMeasuredAt: Long? = null
        var beforeId: String? = null
        while (true) {
            val page = database.bloodPressureDao().getReadingsPage(
                patientId = DEFAULT_PATIENT_ID,
                beforeMeasuredAt = beforeMeasuredAt,
                beforeId = beforeId,
                pageSize = LOCAL_SNAPSHOT_PAGE_SIZE,
            )
            if (page.isEmpty()) return

            pressureRepository.uploadPressureReadingsBatch(familyId, remotePatientId, page, userId)

            val last = page.last()
            if (
                page.size < LOCAL_SNAPSHOT_PAGE_SIZE ||
                (last.measuredAt == beforeMeasuredAt && last.id == beforeId)
            ) return
            beforeMeasuredAt = last.measuredAt
            beforeId = last.id
        }
    }

    private suspend fun uploadLocalMedicationLogHistory(
        familyId: String,
        remotePatientId: String,
        userId: String?,
    ) {
        var beforeScheduledFor: Long? = null
        var beforeId: String? = null
        while (true) {
            val page = database.medicationLogDao().getLogsPage(
                patientId = DEFAULT_PATIENT_ID,
                beforeScheduledFor = beforeScheduledFor,
                beforeId = beforeId,
                pageSize = LOCAL_SNAPSHOT_PAGE_SIZE,
            )
            if (page.isEmpty()) return

            medicationLogRepository.uploadMedicationLogsBatch(familyId, remotePatientId, page, userId)

            val last = page.last()
            if (
                page.size < LOCAL_SNAPSHOT_PAGE_SIZE ||
                (last.scheduledFor == beforeScheduledFor && last.id == beforeId)
            ) return
            beforeScheduledFor = last.scheduledFor
            beforeId = last.id
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val TAG = "[Contigo][Sync]"
        const val BACKFILL_PAGE_SIZE = 200L
        const val LOCAL_SNAPSHOT_PAGE_SIZE = 200
        const val REMOTE_IMAGE_DOWNLOAD_MAX_ATTEMPTS = 3
        const val REMOTE_IMAGE_DOWNLOAD_RETRY_DELAY_MS = 750L
    }
}

private fun Any?.toIsoDateOrNull(): String? {
    return when (this) {
        null -> null
        is com.google.firebase.Timestamp -> this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()
        is java.util.Date -> this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString()
        is Number -> Instant.ofEpochMilli(this.toLong()).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        is String -> runCatching { LocalDate.parse(this) }.getOrNull()?.toString()
        else -> null
    }
}

private fun Any?.toIsoDateOrDefault(): String {
    return this.toIsoDateOrNull() ?: MedicationScheduleDefaults.todayIso()
}
