package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.domain.DailyRiskLevel
import com.cuidavoz.mobile.domain.DailyStatusSnapshot
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.domain.MedicationGroup
import com.cuidavoz.mobile.domain.MedicationOutcomeResult
import com.cuidavoz.mobile.domain.medicationOutcomeUserMessage
import com.cuidavoz.mobile.domain.PressureClassifier
import com.cuidavoz.mobile.domain.PressureStatus
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderLaunchState
import com.cuidavoz.mobile.reminders.ReminderPayload
import com.cuidavoz.mobile.reminders.ReminderPrompt
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.formatScheduleTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeScreenState(
    val patient: PatientEntity? = null,
    val contact: FamilyContactEntity? = null,
    val dailyStatus: DailyStatusSnapshot? = null,
    val reminderPrompt: ReminderPrompt? = null,
    val actionMessage: String? = null,
    val pressureHelperMessage: String? = null,
    val isSavingPressure: Boolean = false,
) {
    val patientFirstName: String
        get() = patient?.fullName?.substringBefore(" ") ?: "Paciente"

    val greeting: String
        get() = "Hola, $patientFirstName"

    private val currentGroup: MedicationGroup?
        get() {
            val promptedGroup = reminderPrompt?.let { prompt ->
                dailyStatus?.medicationGroups?.firstOrNull {
                    it.scheduleTime == prompt.scheduleTime && it.pendingCount > 0
                }
            }
            return promptedGroup ?: dailyStatus?.nextMedicationGroup
        }

    val nextGroupSummary: String?
        get() {
            val group = currentGroup ?: return null
            val count = group.pendingCount
            if (count <= 0) return null
            return if (count == 1) {
                "${group.pendingMedications.first().name} a las ${formatScheduleTime(group.scheduleTime)}"
            } else {
                "$count pastillas a las ${formatScheduleTime(group.scheduleTime)}"
            }
        }

    val nextGroupNames: String?
        get() = currentGroup?.pendingMedications?.joinToString(", ") { it.name }

    val nextGroupMedicationNames: List<String>
        get() = currentGroup?.pendingMedications?.map { it.name } ?: emptyList()

    val nextGroupMedications: List<MedicationEntity>
        get() = currentGroup?.pendingMedications ?: emptyList()

    val nextGroupScheduleTime: String?
        get() = currentGroup?.scheduleTime

    val nextGroupImageUris: List<String>
        get() = currentGroup?.pendingMedications?.filter { !it.imageUri.isNullOrBlank() }?.mapNotNull { it.imageUri } ?: emptyList()

    val nextGroupImageLabels: List<String>
        get() = currentGroup?.pendingMedications?.filter { !it.imageUri.isNullOrBlank() }?.map { it.name } ?: emptyList()

    val primaryActionLabel: String?
        get() {
            val group = currentGroup ?: return null
            if (reminderPrompt != null) {
                return "Tomé mi pastilla"
            }
            return if (group.pendingCount <= 1) {
                "Tomé mi pastilla"
            } else {
                "Tomé mis pastillas"
            }
        }

    val reminderPromptText: String?
        get() {
            val prompt = reminderPrompt ?: return null
            val scheduleText = formatScheduleTime(prompt.scheduleTime)
            return if (prompt.requiresConfirmation) {
                "¿Confirmas que ya tomaste tus pastillas de las $scheduleText?"
            } else {
                "Es hora de tu pastilla de las $scheduleText."
            }
        }

    val latestPressureText: String
        get() {
            val pressure = dailyStatus?.latestPressureToday ?: return "Sin registro hoy"
            val pulseText = pressure.pulse?.let { " · Pulso $it" } ?: ""
            return "${pressure.systolic}/${pressure.diastolic}$pulseText"
        }

    val generalStatusText: String
        get() {
            val pressureStatus = dailyStatus?.latestPressureToday?.status
            return when {
                reminderPrompt != null -> "Es hora de tu pastilla"
                pressureStatus == PressureStatus.HIGH.name ||
                    pressureStatus == PressureStatus.CRITICAL.name ||
                    pressureStatus == PressureStatus.OUT_OF_RANGE.name ->
                    "Revisar presión"
                (dailyStatus?.pendingMedicationCount ?: 0) > 0 ->
                    "Hay pastillas pendientes"
                else -> "Todo bien"
            }
        }

    val medicationsPendingText: String
        get() = "${dailyStatus?.pendingMedicationCount ?: 0} pendientes"

    val adherencePercentageText: String
        get() {
            val total = dailyStatus?.activeMedicationCount ?: 0
            val taken = dailyStatus?.takenMedicationCount ?: 0
            if (total == 0) return "Sin medicamentos activos"
            return "${((taken.toDouble() / total.toDouble()) * 100.0).toInt()}%"
        }

    val helpPhone: String?
        get() = contact?.phone
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    patientRepository: PatientRepository,
    familyContactRepository: FamilyContactRepository,
    private val dailyStatusRepository: DailyStatusRepository,
    private val pressureRepository: PressureRepository,
    private val reminderLaunchState: ReminderLaunchState,
    private val reminderScheduler: MedicationReminderScheduler,
) : ViewModel() {
    private val feedbackState = MutableStateFlow(HomeFeedbackState())

    val uiState: StateFlow<HomeScreenState> = combine(
        patientRepository.observeCurrentPatient(),
        familyContactRepository.observePrimaryContact(DEFAULT_PATIENT_ID),
        dailyStatusRepository.observeDailyStatus(DEFAULT_PATIENT_ID),
        reminderLaunchState.prompt,
        feedbackState,
    ) { patient, contact, dailyStatus, reminderPrompt, feedback ->
        val pressureStatus = dailyStatus.latestPressureToday?.status?.let {
            runCatching { PressureStatus.valueOf(it) }.getOrNull()
        }
        HomeScreenState(
            patient = patient,
            contact = contact,
            dailyStatus = dailyStatus,
            reminderPrompt = reminderPrompt,
            actionMessage = feedback.actionMessage,
            pressureHelperMessage = pressureStatus?.let(PressureClassifier::safeMessage),
            isSavingPressure = feedback.isSavingPressure,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeScreenState(),
    )

    fun dismissMessage() {
        feedbackState.update { it.copy(actionMessage = null) }
    }

    fun registerPressure(
        systolicText: String,
        diastolicText: String,
        pulseText: String,
        notes: String,
        onCompleted: (SavedPressureResult) -> Unit,
    ) {
        val systolic = systolicText.toIntOrNull()
        val diastolic = diastolicText.toIntOrNull()
        val pulse = pulseText.toIntOrNull()

        if (systolicText.isBlank()) {
            feedbackState.update { it.copy(actionMessage = "Escribe la presión alta.") }
            return
        }
        if (diastolicText.isBlank()) {
            feedbackState.update { it.copy(actionMessage = "Escribe la presión baja.") }
            return
        }
        if (
            systolic == null ||
            diastolic == null ||
            systolic !in 50..250 ||
            diastolic !in 30..160 ||
            (pulse != null && pulse !in 30..220)
        ) {
            feedbackState.update { it.copy(actionMessage = "Revisa los valores.") }
            return
        }

        viewModelScope.launch {
            feedbackState.update { it.copy(isSavingPressure = true) }
            val reading = pressureRepository.recordPressure(
                patientId = DEFAULT_PATIENT_ID,
                systolic = systolic,
                diastolic = diastolic,
                pulse = pulse,
                notes = notes.ifBlank { null },
            )
            feedbackState.update {
                it.copy(
                    isSavingPressure = false,
                    actionMessage = "Presión registrada correctamente.",
                )
            }
            onCompleted(
                SavedPressureResult(
                    systolic = reading.systolic,
                    diastolic = reading.diastolic,
                    pulse = reading.pulse,
                    measuredAt = reading.measuredAt,
                    status = reading.status,
                ),
            )
        }
    }

    fun recordNextMedicationGroupOutcomes(outcomes: List<MedicationDoseOutcome>) {
        val state = uiState.value
        val nextGroup = state.reminderPrompt?.let { prompt ->
            state.dailyStatus?.medicationGroups?.firstOrNull {
                it.scheduleTime == prompt.scheduleTime && it.pendingCount > 0
            }
        } ?: state.dailyStatus?.nextMedicationGroup ?: return
        viewModelScope.launch {
            val result = dailyStatusRepository.recordMedicationOutcomes(
                patientId = DEFAULT_PATIENT_ID,
                scheduleTime = nextGroup.scheduleTime,
                outcomes = outcomes,
            )
            if (result.groupResolved) {
                reminderScheduler.cancelMedicationGroupReminder(
                    patientId = DEFAULT_PATIENT_ID,
                    scheduleTime = nextGroup.scheduleTime,
                )
                reminderLaunchState.clearPrompt()
            }

            feedbackState.update {
                it.copy(actionMessage = medicationOutcomeUserMessage(result))
            }
        }
    }

    fun missingContactMessage() {
        feedbackState.update {
            it.copy(actionMessage = "Configura un contacto familiar en Ajustes.")
        }
    }

    fun dismissReminderPrompt() {
        val prompt = uiState.value.reminderPrompt
        if (prompt == null) {
            reminderLaunchState.clearPrompt()
            return
        }
        viewModelScope.launch {
            reminderScheduler.markReminderSnoozed(prompt.toReminderPayload())
            reminderLaunchState.clearPrompt()
            feedbackState.update {
                it.copy(actionMessage = "Te lo recordaré después.")
            }
        }
    }
}

private fun ReminderPrompt.toReminderPayload(): ReminderPayload {
    return ReminderPayload(
        reminderId = reminderId,
        reminderGroupId = reminderGroupId,
        patientId = patientId,
        scheduleTime = scheduleTime,
        targetDate = targetDate,
        scheduledAt = scheduledAt,
        medicationIds = medicationIds,
        medicationNames = medicationNames,
        attemptNumber = 0,
        maxAttempts = 3,
        repeatEveryMinutes = 10,
        requiresConfirmation = requiresConfirmation,
    )
}

private data class HomeFeedbackState(
    val actionMessage: String? = null,
    val isSavingPressure: Boolean = false,
)

data class SavedPressureResult(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val measuredAt: Long,
    val status: String,
)
