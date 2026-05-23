package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.model.BloodPressureEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.MedicationLogEntity
import com.cuidavoz.mobile.data.repository.MedicationLogRepository
import com.cuidavoz.mobile.data.repository.MedicationRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.data.sync.FirebaseSyncManager
import com.cuidavoz.mobile.data.sync.LinkCaregiverResult
import com.cuidavoz.mobile.data.sync.SyncContextRepository
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
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

private data class DashboardSnapshot(
    val patientName: String,
    val latestPressure: String,
    val pressureStatus: String,
    val pendingMedicationsToday: Int,
    val recentMedicationEvents: List<String>,
    val syncStatus: String,
    val syncEnabled: Boolean,
    val familyLinked: Boolean,
    val familyId: String?,
)

data class CaregiverDashboardUiState(
    val patientName: String = "Paciente",
    val latestPressure: String = "Sin registros todavía",
    val pressureStatus: String = "Sin datos",
    val pendingMedicationsToday: Int = 0,
    val recentMedicationEvents: List<String> = emptyList(),
    val syncStatus: String = "Pendiente de sincronizar",
    val syncEnabled: Boolean = true,
    val familyLinked: Boolean = false,
    val familyId: String? = null,
    val linkCodeInput: String = "",
    val createdLinkCode: String? = null,
    val isWorking: Boolean = false,
    val message: String? = null,
)

class CaregiverDashboardViewModel(
    patientRepository: PatientRepository,
    pressureRepository: PressureRepository,
    medicationRepository: MedicationRepository,
    medicationLogRepository: MedicationLogRepository,
    private val syncContextRepository: SyncContextRepository,
    private val firebaseSyncManager: FirebaseSyncManager,
) : ViewModel() {
    private val controls = MutableStateFlow(CaregiverDashboardUiState())

    private val dashboardSnapshot = combine(
        combine(
            patientRepository.observeCurrentPatient(),
            pressureRepository.observeRecentReadings(DEFAULT_PATIENT_ID),
            medicationRepository.observeActiveMedications(DEFAULT_PATIENT_ID),
            medicationLogRepository.observeLogsForRange(DEFAULT_PATIENT_ID, 0L, Long.MAX_VALUE),
            firebaseSyncManager.syncStatusText,
        ) { patient, readings, medications, logs, syncStatus ->
            DashboardSnapshot(
                patientName = patient?.fullName ?: "Paciente",
                latestPressure = readings.firstOrNull()?.let(::formatPressure) ?: "Sin registros todavía",
                pressureStatus = readings.firstOrNull()?.status?.toDisplayStatus() ?: "Sin datos",
                pendingMedicationsToday = calculatePendingMedications(medications, logs),
                recentMedicationEvents = buildRecentMedicationEvents(medications, logs),
                syncStatus = syncStatus,
                syncEnabled = true,
                familyLinked = false,
                familyId = null,
            )
        },
        syncContextRepository.syncContextFlow,
    ) { snapshot, syncContext ->
        snapshot.copy(
            syncEnabled = syncContext.syncEnabled,
            familyLinked = !syncContext.familyId.isNullOrBlank(),
            familyId = syncContext.familyId,
        )
    }

    val uiState: StateFlow<CaregiverDashboardUiState> = combine(
        dashboardSnapshot,
        controls,
    ) { snapshot, local ->
        local.copy(
            patientName = snapshot.patientName,
            latestPressure = snapshot.latestPressure,
            pressureStatus = snapshot.pressureStatus,
            pendingMedicationsToday = snapshot.pendingMedicationsToday,
            recentMedicationEvents = snapshot.recentMedicationEvents,
            syncStatus = snapshot.syncStatus,
            syncEnabled = snapshot.syncEnabled,
            familyLinked = snapshot.familyLinked,
            familyId = snapshot.familyId,
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
                        "Firebase aún no está configurado en este celular."
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

    private fun calculatePendingMedications(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
    ): Int {
        val (startOfDay, endOfDay) = todayRangeMillis()
        val todayTakenKeys = logs
            .filter { it.status == "TAKEN" && it.scheduledFor in startOfDay until endOfDay }
            .associateBy { "${it.medicationId}_${it.scheduledFor}" }
        return medications.count { medication ->
            val scheduledFor = scheduleTimeToMillis(medication.scheduleTime)
            todayTakenKeys["${medication.id}_$scheduledFor"] == null
        }
    }

    private fun buildRecentMedicationEvents(
        medications: List<MedicationEntity>,
        logs: List<MedicationLogEntity>,
    ): List<String> {
        val namesByMedicationId = medications.associateBy(MedicationEntity::id)
        return logs
            .sortedByDescending(MedicationLogEntity::scheduledFor)
            .take(4)
            .map { log ->
                val name = namesByMedicationId[log.medicationId]?.name ?: "Pastilla"
                val takenLabel = log.takenAt?.let { "Tomada a las ${formatTimeForDisplay(epochToTime24(it))}" }
                    ?: "Pendiente"
                "$name · $takenLabel"
            }
    }

    private fun formatPressure(reading: BloodPressureEntity): String {
        val pulseText = reading.pulse?.let { " · Pulso $it" }.orEmpty()
        return "${reading.systolic}/${reading.diastolic}$pulseText"
    }

    private fun String.toDisplayStatus(): String = when (this) {
        "NORMAL" -> "Todo bien"
        "ELEVATED" -> "Valor elevado"
        "HIGH" -> "Valor alto"
        "CRITICAL" -> "Muy alta"
        "OUT_OF_RANGE" -> "Fuera de rango"
        else -> "Sin datos"
    }

    private fun epochToTime24(value: Long): String {
        val time = java.time.Instant.ofEpochMilli(value)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalTime()
        return "%02d:%02d".format(time.hour, time.minute)
    }
}
