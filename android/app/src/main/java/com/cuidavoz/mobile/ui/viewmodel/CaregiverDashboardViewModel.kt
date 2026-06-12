package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.MedicationLogRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.data.sync.LinkCaregiverResult
import com.cuidavoz.mobile.data.sync.SyncContextRepository
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.domain.medicationSkipReasonLabel
import com.cuidavoz.mobile.util.formatDateTime
import com.cuidavoz.mobile.util.formatTimeForDisplay
import com.cuidavoz.mobile.util.scheduleTimeToMillis
import com.cuidavoz.mobile.util.todayRangeMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_VISIBLE_MEDICATIONS = 4
private const val MAX_RECENT_ACTIVITY = 5
private const val PENDING_NOW_WINDOW_MILLIS = 60 * 60 * 1_000L
private const val PRESSURE_OLD_AFTER_MILLIS = 24 * 60 * 60 * 1_000L

private data class HealthSnapshot(
    val patientName: String,
    val todayStatusText: String,
    val takenToday: Int,
    val totalToday: Int,
    val pendingToday: Int,
    val hasOverdueMedications: Boolean,
    val nextMedication: CaregiverMedicationUi?,
    val pendingMedications: List<CaregiverMedicationUi>,
    val remainingPendingCount: Int,
    val takenMedications: List<CaregiverMedicationUi>,
    val remainingTakenCount: Int,
    val latestPressure: CaregiverPressureUi?,
    val recentActivity: List<CaregiverActivityUi>,
)

private data class DashboardSnapshot(
    val health: HealthSnapshot,
    val syncStatusText: String,
    val syncDetailText: String?,
    val syncNeedsAttention: Boolean,
    val syncEnabled: Boolean,
    val familyLinked: Boolean,
    val familyId: String?,
)

private data class SyncPresentation(
    val statusText: String,
    val detailText: String?,
    val needsAttention: Boolean,
)

private data class TimedCaregiverActivity(
    val occurredAt: Long,
    val activity: CaregiverActivityUi,
)

enum class CaregiverMedicationStatus {
    OVERDUE,
    DUE_NOW,
    UPCOMING,
    TAKEN,
}

data class CaregiverMedicationUi(
    val id: String,
    val name: String,
    val dosage: String,
    val scheduledTimeText: String,
    val takenTimeText: String?,
    val imageUri: String?,
    val statusText: String,
    val status: CaregiverMedicationStatus,
) {
    val isOverdue: Boolean
        get() = status == CaregiverMedicationStatus.OVERDUE
}

enum class CaregiverActivityType {
    MEDICATION_TAKEN,
    MEDICATION_SKIPPED,
    PRESSURE_RECORDED,
}

enum class ActivityPriority {
    NORMAL,
    WARNING,
    CRITICAL,
}

data class CaregiverActivityUi(
    val id: String,
    val type: CaregiverActivityType,
    val title: String,
    val subtitle: String,
    val timeText: String,
    val priority: ActivityPriority,
)

data class CaregiverPressureUi(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val measuredAtText: String,
    val classificationText: String,
    val attentionText: String?,
    val isOld: Boolean,
    val priority: ActivityPriority,
)

data class CaregiverDashboardUiState(
    val patientName: String = "Paciente",
    val todayStatusText: String = "Cargando estado de hoy",
    val takenToday: Int = 0,
    val totalToday: Int = 0,
    val pendingToday: Int = 0,
    val hasOverdueMedications: Boolean = false,
    val nextMedication: CaregiverMedicationUi? = null,
    val pendingMedications: List<CaregiverMedicationUi> = emptyList(),
    val remainingPendingCount: Int = 0,
    val takenMedications: List<CaregiverMedicationUi> = emptyList(),
    val remainingTakenCount: Int = 0,
    val latestPressure: CaregiverPressureUi? = null,
    val recentActivity: List<CaregiverActivityUi> = emptyList(),
    val syncStatusText: String = "Pendiente de sincronizar",
    val syncDetailText: String? = null,
    val syncNeedsAttention: Boolean = true,
    val syncEnabled: Boolean = true,
    val familyLinked: Boolean = false,
    val familyId: String? = null,
    val isLoading: Boolean = true,
    val linkCodeInput: String = "",
    val createdLinkCode: String? = null,
    val isWorking: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class CaregiverDashboardViewModel @Inject constructor(
    patientRepository: PatientRepository,
    pressureRepository: PressureRepository,
    medicationRepository: MedicationRepository,
    medicationLogRepository: MedicationLogRepository,
    dailyStatusRepository: DailyStatusRepository,
    private val syncContextRepository: SyncContextRepository,
    private val firebaseSyncManager: FirebaseSyncManager,
) : ViewModel() {
    private val controls = MutableStateFlow(CaregiverDashboardUiState())

    private val healthSnapshot = combine(
        patientRepository.observeCurrentPatient(),
        pressureRepository.observeRecentReadings(DEFAULT_PATIENT_ID),
        medicationRepository.observeActiveMedications(DEFAULT_PATIENT_ID),
        medicationLogRepository.observeLogsForRange(DEFAULT_PATIENT_ID, 0L, Long.MAX_VALUE),
        dailyStatusRepository.observeDailyStatus(DEFAULT_PATIENT_ID),
    ) { patient, readings, medications, logs, dailyStatus ->
        val now = System.currentTimeMillis()
        val (startOfDay, endOfDay) = todayRangeMillis()
        val todayLogs = logs.filter { it.scheduledFor in startOfDay until endOfDay }
        val takenLogsByMedicationId = todayLogs
            .filter { it.status == "TAKEN" }
            .groupBy(MedicationLogEntity::medicationId)
            .mapValues { (_, medicationLogs) ->
                medicationLogs.maxByOrNull { it.takenAt ?: it.createdAt }
            }
        val medicationsDueToday = dailyStatus.medicationGroups
            .flatMap { it.medications }
            .distinctBy(MedicationEntity::id)
        val pendingMedicationEntities = dailyStatus.medicationGroups
            .flatMap { it.pendingMedications }
            .distinctBy(MedicationEntity::id)
            .sortedBy { scheduleTimeToMillis(it.scheduleTime) }
        val pendingMedicationItems = pendingMedicationEntities.map { medication ->
            medication.toCaregiverMedicationUi(now = now)
        }
        val takenMedicationItems = medicationsDueToday
            .mapNotNull { medication ->
                takenLogsByMedicationId[medication.id]?.let { log ->
                    medication.toCaregiverMedicationUi(
                        now = now,
                        takenAt = log.takenAt,
                    )
                }
            }
            .sortedByDescending { item ->
                takenLogsByMedicationId[item.id]?.takenAt ?: 0L
            }
        val hasOverdueMedications = pendingMedicationItems.any(CaregiverMedicationUi::isOverdue)

        HealthSnapshot(
            patientName = patient?.fullName ?: "Paciente",
            todayStatusText = todayStatusText(
                totalToday = dailyStatus.activeMedicationCount,
                pendingToday = dailyStatus.pendingMedicationCount,
                hasOverdueMedications = hasOverdueMedications,
            ),
            takenToday = dailyStatus.takenMedicationCount,
            totalToday = dailyStatus.activeMedicationCount,
            pendingToday = dailyStatus.pendingMedicationCount,
            hasOverdueMedications = hasOverdueMedications,
            nextMedication = pendingMedicationItems.firstOrNull(),
            pendingMedications = pendingMedicationItems.take(MAX_VISIBLE_MEDICATIONS),
            remainingPendingCount = (pendingMedicationItems.size - MAX_VISIBLE_MEDICATIONS).coerceAtLeast(0),
            takenMedications = takenMedicationItems.take(MAX_VISIBLE_MEDICATIONS),
            remainingTakenCount = (takenMedicationItems.size - MAX_VISIBLE_MEDICATIONS).coerceAtLeast(0),
            latestPressure = readings.firstOrNull()?.toCaregiverPressureUi(now),
            recentActivity = buildRecentActivity(
                medications = medications,
                logs = logs,
                readings = readings,
            ),
        )
    }

    private val dashboardSnapshot = combine(
        healthSnapshot,
        firebaseSyncManager.syncStatusText,
        syncContextRepository.syncContextFlow,
    ) { health, rawSyncStatus, syncContext ->
        val familyLinked = !syncContext.familyId.isNullOrBlank()
        val syncPresentation = buildSyncPresentation(
            rawSyncStatus = rawSyncStatus,
            syncEnabled = syncContext.syncEnabled,
            familyLinked = familyLinked,
        )
        DashboardSnapshot(
            health = health,
            syncStatusText = syncPresentation.statusText,
            syncDetailText = syncPresentation.detailText,
            syncNeedsAttention = syncPresentation.needsAttention,
            syncEnabled = syncContext.syncEnabled,
            familyLinked = familyLinked,
            familyId = syncContext.familyId,
        )
    }

    val uiState: StateFlow<CaregiverDashboardUiState> = combine(
        dashboardSnapshot,
        controls,
    ) { snapshot, local ->
        local.copy(
            patientName = snapshot.health.patientName,
            todayStatusText = snapshot.health.todayStatusText,
            takenToday = snapshot.health.takenToday,
            totalToday = snapshot.health.totalToday,
            pendingToday = snapshot.health.pendingToday,
            hasOverdueMedications = snapshot.health.hasOverdueMedications,
            nextMedication = snapshot.health.nextMedication,
            pendingMedications = snapshot.health.pendingMedications,
            remainingPendingCount = snapshot.health.remainingPendingCount,
            takenMedications = snapshot.health.takenMedications,
            remainingTakenCount = snapshot.health.remainingTakenCount,
            latestPressure = snapshot.health.latestPressure,
            recentActivity = snapshot.health.recentActivity,
            syncStatusText = snapshot.syncStatusText,
            syncDetailText = snapshot.syncDetailText,
            syncNeedsAttention = snapshot.syncNeedsAttention,
            syncEnabled = snapshot.syncEnabled,
            familyLinked = snapshot.familyLinked,
            familyId = snapshot.familyId,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CaregiverDashboardUiState(),
    )

    fun updateLinkCodeInput(value: String) {
        controls.update { it.copy(linkCodeInput = value.filter(Char::isDigit).take(6)) }
    }

    fun createLinkCode() {
        viewModelScope.launch {
            controls.update { it.copy(isWorking = true, message = null) }
            val code = firebaseSyncManager.createLinkCode()
            controls.update {
                it.copy(
                    isWorking = false,
                    createdLinkCode = code,
                    message = if (code == null) {
                        "No pude crear el código. Revisa internet y vuelve a intentar."
                    } else {
                        "Comparte este código con el cuidador. Vence en 10 minutos."
                    },
                )
            }
        }
    }

    fun linkWithCode() {
        val code = controls.value.linkCodeInput
        if (code.length != 6) {
            controls.update { it.copy(message = "Escribe el código de 6 dígitos.") }
            return
        }
        viewModelScope.launch {
            controls.update { it.copy(isWorking = true, message = null) }
            val result: LinkCaregiverResult = firebaseSyncManager.linkCaregiver(code)
            controls.update {
                it.copy(
                    isWorking = false,
                    createdLinkCode = null,
                    linkCodeInput = if (result.success) "" else it.linkCodeInput,
                    message = result.message,
                )
            }
        }
    }

    fun retrySync() {
        viewModelScope.launch {
            controls.update { it.copy(isWorking = true, message = null) }
            firebaseSyncManager.syncPendingNow()
            controls.update { it.copy(isWorking = false, message = "Intenté sincronizar otra vez.") }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            syncContextRepository.setSyncEnabled(enabled)
            if (enabled) {
                firebaseSyncManager.syncPendingNow()
            }
            controls.update {
                it.copy(
                    message = if (enabled) {
                        "La sincronización quedó activa."
                    } else {
                        "La sincronización quedó en pausa en este celular."
                    },
                )
            }
        }
    }

    fun dismissMessage() {
        controls.update { it.copy(message = null) }
    }

    private fun MedicationEntity.toCaregiverMedicationUi(
        now: Long,
        takenAt: Long? = null,
    ): CaregiverMedicationUi {
        val scheduledFor = scheduleTimeToMillis(scheduleTime)
        val isOverdue = takenAt == null && scheduledFor < now
        val status = when {
            takenAt != null -> CaregiverMedicationStatus.TAKEN
            isOverdue -> CaregiverMedicationStatus.OVERDUE
            scheduledFor <= now + PENDING_NOW_WINDOW_MILLIS -> CaregiverMedicationStatus.DUE_NOW
            else -> CaregiverMedicationStatus.UPCOMING
        }
        val statusText = when (status) {
            CaregiverMedicationStatus.TAKEN -> "Tomado"
            CaregiverMedicationStatus.OVERDUE -> "Atrasado"
            CaregiverMedicationStatus.DUE_NOW -> "Pendiente ahora"
            CaregiverMedicationStatus.UPCOMING -> "Próximo"
        }
        return CaregiverMedicationUi(
            id = id,
            name = name,
            dosage = dose,
            scheduledTimeText = formatTimeForDisplay(scheduleTime),
            takenTimeText = takenAt?.let { formatTimeForDisplay(epochToTime24(it)) },
            imageUri = imageUri,
            statusText = statusText,
            status = status,
        )
    }

    private fun BloodPressureEntity.toCaregiverPressureUi(now: Long): CaregiverPressureUi {
        val priority = pressurePriority(status)
        return CaregiverPressureUi(
            systolic = systolic,
            diastolic = diastolic,
            pulse = pulse,
            measuredAtText = formatDateTime(measuredAt),
            classificationText = status.toDisplayStatus(),
            attentionText = pressureAttentionText(status),
            isOld = now - measuredAt > PRESSURE_OLD_AFTER_MILLIS,
            priority = priority,
        )
    }

    private fun buildRecentActivity(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
        readings: List<BloodPressureEntity>,
    ): List<CaregiverActivityUi> {
        val namesByMedicationId = medications.associateBy(MedicationEntity::id)
        val medicationActivity = logs.mapNotNull { log ->
            val medication = namesByMedicationId[log.medicationId]
            val name = medication?.name ?: "Medicamento"
            val dosage = medication?.dose?.takeIf(String::isNotBlank)
            val occurredAt = log.takenAt ?: log.createdAt
            when (log.status) {
                "TAKEN" -> TimedCaregiverActivity(
                    occurredAt = occurredAt,
                    activity = CaregiverActivityUi(
                        id = "medication_taken_${log.id}",
                        type = CaregiverActivityType.MEDICATION_TAKEN,
                        title = "Toma registrada",
                        subtitle = listOfNotNull(name, dosage).joinToString(" · "),
                        timeText = formatDateTime(occurredAt),
                        priority = ActivityPriority.NORMAL,
                    ),
                )
                "SKIPPED" -> TimedCaregiverActivity(
                    occurredAt = occurredAt,
                    activity = CaregiverActivityUi(
                        id = "medication_skipped_${log.id}",
                        type = CaregiverActivityType.MEDICATION_SKIPPED,
                        title = "Medicamento omitido",
                        subtitle = buildString {
                            append(listOfNotNull(name, dosage).joinToString(" · "))
                            medicationSkipReasonLabel(log.skipReason)?.let { reason ->
                                append(" · ")
                                append(reason)
                            }
                        },
                        timeText = formatDateTime(occurredAt),
                        priority = ActivityPriority.WARNING,
                    ),
                )
                else -> null
            }
        }
        val pressureActivity = readings.map { reading ->
            TimedCaregiverActivity(
                occurredAt = reading.measuredAt,
                activity = CaregiverActivityUi(
                    id = "pressure_${reading.id}",
                    type = CaregiverActivityType.PRESSURE_RECORDED,
                    title = "Presión registrada",
                    subtitle = "${reading.systolic}/${reading.diastolic} · ${reading.status.toDisplayStatus()}",
                    timeText = formatDateTime(reading.measuredAt),
                    priority = pressurePriority(reading.status),
                ),
            )
        }
        return (medicationActivity + pressureActivity)
            .sortedByDescending(TimedCaregiverActivity::occurredAt)
            .take(MAX_RECENT_ACTIVITY)
            .map(TimedCaregiverActivity::activity)
    }

    private fun todayStatusText(
        totalToday: Int,
        pendingToday: Int,
        hasOverdueMedications: Boolean,
    ): String {
        return when {
            totalToday == 0 -> "No hay medicamentos programados para hoy"
            hasOverdueMedications -> "Medicamentos atrasados"
            pendingToday > 0 -> "Hay pendientes"
            else -> "Todo al día"
        }
    }

    private fun buildSyncPresentation(
        rawSyncStatus: String,
        syncEnabled: Boolean,
        familyLinked: Boolean,
    ): SyncPresentation {
        val normalizedStatus = rawSyncStatus.lowercase()
        return when {
            !syncEnabled -> SyncPresentation(
                statusText = "Sincronización pausada",
                detailText = "Los cambios permanecerán en este celular hasta volver a activarla.",
                needsAttention = true,
            )
            !familyLinked -> SyncPresentation(
                statusText = "Requiere atención",
                detailText = "Este celular todavía no está vinculado a un paciente.",
                needsAttention = true,
            )
            normalizedStatus.contains("sin internet") ||
                normalizedStatus.contains("sin conexión") -> SyncPresentation(
                statusText = "Sin conexión",
                detailText = "Los cambios se sincronizarán cuando vuelva internet.",
                needsAttention = true,
            )
            normalizedStatus.contains("pendiente") -> SyncPresentation(
                statusText = "Hay cambios pendientes",
                detailText = rawSyncStatus,
                needsAttention = true,
            )
            listOf("error", "fall", "no se pudo").any(normalizedStatus::contains) -> SyncPresentation(
                statusText = "Requiere atención",
                detailText = rawSyncStatus,
                needsAttention = true,
            )
            else -> SyncPresentation(
                statusText = "Sincronización activa",
                detailText = null,
                needsAttention = false,
            )
        }
    }

    private fun String.toDisplayStatus(): String = when (this) {
        "NORMAL" -> "Presión normal"
        "ELEVATED" -> "Presión elevada"
        "HIGH" -> "Presión alta"
        "CRITICAL" -> "Presión crítica"
        "OUT_OF_RANGE" -> "Fuera de rango"
        else -> "Sin datos"
    }

    private fun pressurePriority(status: String): ActivityPriority = when (status) {
        "HIGH", "CRITICAL" -> ActivityPriority.CRITICAL
        "ELEVATED", "OUT_OF_RANGE" -> ActivityPriority.WARNING
        else -> ActivityPriority.NORMAL
    }

    private fun pressureAttentionText(status: String): String? = when (status) {
        "CRITICAL" -> "Presión crítica: requiere atención inmediata."
        "HIGH" -> "Presión alta: requiere atención."
        "OUT_OF_RANGE" -> "Valor fuera del rango indicado."
        else -> null
    }

    private fun epochToTime24(value: Long): String {
        val time = java.time.Instant.ofEpochMilli(value)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        return "%02d:%02d".format(time.hour, time.minute)
    }
}
