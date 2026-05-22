package com.cuidavoz.mobile.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.room.withTransaction
import com.cuidavoz.mobile.data.firebase.FirebaseAuthRepository
import com.cuidavoz.mobile.data.firebase.FirestoreHealthSettingsRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationLogRepository
import com.cuidavoz.mobile.data.firebase.FirestoreMedicationRepository
import com.cuidavoz.mobile.data.firebase.FirestorePaths
import com.cuidavoz.mobile.data.firebase.FirestorePatientRepository
import com.cuidavoz.mobile.data.firebase.FirestorePressureRepository
import com.cuidavoz.mobile.data.local.CuidaVozDatabase
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.model.SyncQueueEntity
import com.cuidavoz.mobile.domain.MedicationScheduleDefaults
import com.cuidavoz.mobile.domain.sync.SyncEntityType
import com.cuidavoz.mobile.domain.sync.SyncOperation
import com.cuidavoz.mobile.domain.sync.SyncStatus
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.createLocalId
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class LinkCaregiverResult(
    val success: Boolean,
    val message: String,
)

class FirebaseSyncManager(
    private val context: Context,
    private val database: CuidaVozDatabase,
    private val syncContextRepository: SyncContextRepository,
    private val authRepository: FirebaseAuthRepository,
    private val patientRepository: FirestorePatientRepository,
    private val medicationRepository: FirestoreMedicationRepository,
    private val pressureRepository: FirestorePressureRepository,
    private val medicationLogRepository: FirestoreMedicationLogRepository,
    private val healthSettingsRepository: FirestoreHealthSettingsRepository,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val firestore: FirebaseFirestore? by lazy {
        if (FirebaseApp.getApps(appContext).isEmpty()) null else FirebaseFirestore.getInstance()
    }
    private val listeners = mutableListOf<ListenerRegistration>()
    private var reminderScheduler: MedicationReminderScheduler? = null

    val syncStatusText: Flow<String> =
        database.syncQueueDao().observePending().map { pending ->
            when {
                !isInternetAvailable() -> "Sin internet. Se sincronizará luego."
                pending.isEmpty() -> "Sincronizado"
                else -> "Pendiente de sincronizar"
            }
        }

    fun start() {
        scope.launch {
            ensureSignedIn()
            refreshFcmToken()
            syncPendingNow()
            startRealtimeListeners()
        }
    }

    fun attachReminderScheduler(scheduler: MedicationReminderScheduler) {
        reminderScheduler = scheduler
    }

    suspend fun ensureSignedIn(): String? {
        if (!authRepository.isConfigured()) return null
        val uid = authRepository.signInAnonymously()
        syncContextRepository.setFirebaseUserId(uid)
        return uid
    }

    suspend fun enqueuePatient(patient: PatientEntity) {
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

    suspend fun enqueueMedication(
        medication: MedicationEntity,
        operation: SyncOperation = SyncOperation.UPDATE,
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
                .put("isActive", medication.isActive)
                .put("scheduleType", medication.scheduleType)
                .put("startDate", medication.startDate)
                .put("endDate", medication.endDate)
                .put("daysOfWeekJson", medication.daysOfWeekJson)
                .put("specificDatesJson", medication.specificDatesJson)
                .put("createdAt", medication.createdAt)
                .put("updatedAt", medication.updatedAt),
        )
    }

    suspend fun enqueuePressureReading(reading: BloodPressureEntity) {
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

    suspend fun enqueueMedicationLog(log: MedicationLogEntity) {
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
                .put("createdAt", log.createdAt),
        )
    }

    suspend fun enqueueHealthSettings(settings: HealthSettingsEntity) {
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

    suspend fun enqueueFamilyContact(contact: FamilyContactEntity) {
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

    suspend fun enqueueAlert(
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

    suspend fun createLinkCode(): String? {
        val db = firestore ?: return null
        return runCatching {
            val uid = ensureSignedIn() ?: return null
            val context = syncContextRepository.getCurrent()
            val patientId = context.patientId ?: DEFAULT_PATIENT_ID
            val familyId = context.familyId ?: "family_${UUID.randomUUID().toString().take(8)}"
            syncContextRepository.updateFamilyContext(familyId, patientId, memberRole = "patient")

            db.document(FirestorePaths.familyDocument(familyId))
                .set(
                    mapOf(
                        "name" to "Familia CuidaVoz",
                        "createdAt" to System.currentTimeMillis(),
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

            val code = (100000..999999).random().toString()
            db.collection("linkCodes")
                .document(code)
                .set(
                    mapOf(
                        "familyId" to familyId,
                        "patientId" to patientId,
                        "expiresAt" to System.currentTimeMillis() + 10 * 60 * 1000,
                        "used" to false,
                        "createdBy" to uid,
                    ),
                ).await()
            refreshFcmToken()
            code
        }.getOrNull()
    }

    suspend fun linkCaregiver(code: String): LinkCaregiverResult {
        val db = firestore ?: return LinkCaregiverResult(false, "Firebase no está configurado todavía.")
        return runCatching {
            val uid = ensureSignedIn() ?: return LinkCaregiverResult(false, "No pude iniciar la sesión remota.")
            val codeDoc = db.collection("linkCodes").document(code.trim()).get().await()
            if (!codeDoc.exists()) {
                return LinkCaregiverResult(false, "Ese código no es válido.")
            }
            val expiresAt = codeDoc.getLong("expiresAt") ?: 0L
            val used = codeDoc.getBoolean("used") ?: false
            if (used || expiresAt < System.currentTimeMillis()) {
                return LinkCaregiverResult(false, "Ese código ya no está disponible.")
            }
            val familyId = codeDoc.getString("familyId").orEmpty()
            val patientId = codeDoc.getString("patientId").orEmpty()
            db.document("${FirestorePaths.familyMembersCollection(familyId)}/$uid")
                .set(
                    mapOf(
                        "role" to "caregiver",
                        "displayName" to "Cuidador",
                        "phone" to null,
                        "linkedAt" to System.currentTimeMillis(),
                    ),
                ).await()
            db.collection("linkCodes").document(code.trim())
                .update("used", true)
                .await()
            syncContextRepository.updateFamilyContext(
                familyId,
                patientId.ifBlank { DEFAULT_PATIENT_ID },
                memberRole = "caregiver",
            )
            refreshFcmToken()
            startRealtimeListeners()
            syncPendingNow()
            LinkCaregiverResult(true, "Vinculación completada.")
        }.getOrElse {
            LinkCaregiverResult(false, "No pude completar la vinculación. Intenta otra vez.")
        }
    }

    suspend fun syncPendingNow() {
        if (!authRepository.isConfigured() || !isInternetAvailable()) return
        val context = syncContextRepository.getCurrent()
        if (!context.syncEnabled || context.familyId.isNullOrBlank()) return
        val familyId = context.familyId
        val remotePatientId = context.patientId ?: DEFAULT_PATIENT_ID
        val userId = ensureSignedIn()
        val pending = database.syncQueueDao().getPending()
        pending.forEach { item ->
            val now = System.currentTimeMillis()
            runCatching {
                database.syncQueueDao().markSyncing(item.id, now)
                val payload = JSONObject(item.payloadJson)
                when (SyncEntityType.valueOf(item.entityType)) {
                    SyncEntityType.PATIENT -> {
                        patientRepository.upsertPatient(
                            familyId = familyId,
                            patient = PatientEntity(
                                id = payload.getString("id"),
                                fullName = payload.getString("name"),
                                age = payload.optInt("age").takeIf { !payload.isNull("age") },
                                notes = payload.optString("notes").takeIf { it.isNotBlank() },
                                createdAt = now,
                                updatedAt = payload.getLong("updatedAt"),
                            ),
                            caregiverUserId = null,
                        )
                    }
                    SyncEntityType.MEDICATION -> {
                        medicationRepository.upsertMedication(
                            familyId = familyId,
                            patientId = remotePatientId,
                            medication = MedicationEntity(
                                id = payload.getString("id"),
                                patientId = DEFAULT_PATIENT_ID,
                                name = payload.getString("name"),
                                dose = payload.getString("dose"),
                                color = payload.optString("color").takeIf { it.isNotBlank() },
                                shape = payload.optString("shape").takeIf { it.isNotBlank() },
                                instructions = payload.optString("instructions").takeIf { it.isNotBlank() },
                                scheduleTime = payload.getString("scheduleTime"),
                                imageUri = payload.optString("imageUri").takeIf { it.isNotBlank() },
                                isActive = payload.getBoolean("isActive"),
                                scheduleType = payload.optString("scheduleType").ifBlank { "ALWAYS" },
                                startDate = payload.optString("startDate").ifBlank { MedicationScheduleDefaults.todayIso() },
                                endDate = payload.optString("endDate").takeIf { it.isNotBlank() && it != "null" },
                                daysOfWeekJson = payload.optString("daysOfWeekJson").ifBlank { MedicationScheduleDefaults.allDaysJson() },
                                specificDatesJson = payload.optString("specificDatesJson").ifBlank { MedicationScheduleDefaults.emptyDatesJson() },
                                createdAt = payload.optLong("createdAt").takeIf { it > 0 } ?: now,
                                updatedAt = payload.getLong("updatedAt"),
                            ),
                            updatedBy = userId,
                        )
                    }
                    SyncEntityType.PRESSURE_READING -> {
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
                                createdAt = payload.getLong("createdAt"),
                            ),
                            createdBy = userId,
                        )
                    }
                    SyncEntityType.HEALTH_SETTINGS -> {
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
                                updatedAt = payload.getLong("updatedAt"),
                            ),
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
                                updatedAt = payload.getLong("updatedAt"),
                            ),
                        )
                    }
                    SyncEntityType.LINK_CODE -> Unit
                }
                database.syncQueueDao().markSynced(item.id, System.currentTimeMillis())
            }.onFailure { error ->
                Log.w(TAG, "sync failed for ${item.entityType}/${item.entityId}", error)
                database.syncQueueDao().markFailed(item.id, error.message, System.currentTimeMillis())
            }
        }
        syncContextRepository.markSynced(System.currentTimeMillis())
        database.syncQueueDao().deleteSyncedOlderThan(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L)
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
                        val medication = MedicationEntity(
                            id = item["id"]?.toString().orEmpty(),
                            patientId = DEFAULT_PATIENT_ID,
                            name = item["name"]?.toString().orEmpty(),
                            dose = item["dose"]?.toString().orEmpty(),
                            color = item["color"]?.toString(),
                            shape = item["shape"]?.toString(),
                            instructions = item["instructions"]?.toString(),
                            scheduleTime = item["time24"]?.toString().orEmpty(),
                            imageUri = null,
                            isActive = item["active"] as? Boolean ?: true,
                            scheduleType = item["scheduleType"]?.toString().orEmpty().ifBlank { "ALWAYS" },
                            startDate = item["startDate"].toIsoDateOrDefault(),
                            endDate = item["endDate"].toIsoDateOrNull(),
                            daysOfWeekJson = (item["daysOfWeek"] as? List<*>)?.toDaysJson()
                                ?: MedicationScheduleDefaults.allDaysJson(),
                            specificDatesJson = (item["specificDates"] as? List<*>)?.toSpecificDatesJson()
                                ?: MedicationScheduleDefaults.emptyDatesJson(),
                            createdAt = (item["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            updatedAt = (item["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        )
                        val local = database.medicationDao().getMedicationById(medication.id)
                        if (local == null || medication.updatedAt >= local.updatedAt) {
                            database.medicationDao().upsert(medication)
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

            firestore?.collection(FirestorePaths.pressureCollection(familyId, remotePatientId))
                ?.limit(50)
                ?.addSnapshotListener { snapshot, _ ->
                    val docs = snapshot?.documents.orEmpty()
                    scope.launch {
                        docs.forEach { doc ->
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
                                createdAt = doc.getLong("syncedAt") ?: System.currentTimeMillis(),
                            )
                            database.medicationLogDao().insert(log)
                        }
                    }
                }?.let(listeners::add)
        }
    }

    private suspend fun enqueue(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncOperation,
        payload: JSONObject,
    ) {
        val now = System.currentTimeMillis()
        database.syncQueueDao().upsert(
            SyncQueueEntity(
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
            ),
        )
        scope.launch { syncPendingNow() }
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
        database.bloodPressureDao().getRecentReadings(DEFAULT_PATIENT_ID)
            .take(20)
            .forEach { reading ->
                pressureRepository.createPressureReading(familyId, remotePatientId, reading, userId)
            }
        database.medicationLogDao().getLogsForRange(DEFAULT_PATIENT_ID, 0L, Long.MAX_VALUE)
            .take(50)
            .forEach { log ->
                medicationLogRepository.createMedicationLog(familyId, remotePatientId, log, userId)
            }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private companion object {
        const val TAG = "[CuidaVoz][Sync]"
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

private fun List<*>.toDaysJson(): String {
    val days = mapNotNull { item -> (item as? Number)?.toInt() ?: item?.toString()?.toIntOrNull() }
    return if (days.isEmpty()) MedicationScheduleDefaults.allDaysJson() else days.sorted().joinToString(prefix = "[", postfix = "]", separator = ",")
}

private fun List<*>.toSpecificDatesJson(): String {
    val dates = mapNotNull { item -> item.toIsoDateOrNull() }.sorted()
    return if (dates.isEmpty()) {
        MedicationScheduleDefaults.emptyDatesJson()
    } else {
        dates.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }
    }
}
