package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.cuidavoz.mobile.data.model.DeviceRole
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.OnboardingRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.data.sync.SyncContextRepository
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.ui.navigation.UserMode
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.DefaultHealthSettings
import com.cuidavoz.mobile.util.PhoneValidator
import com.cuidavoz.mobile.util.createLocalId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    ROLE_SELECTION,
    DETAILS,
}

data class OnboardingScreenState(
    val setupCompleted: Boolean? = null,
    val savedDeviceRole: DeviceRole? = null,
    val step: OnboardingStep = OnboardingStep.ROLE_SELECTION,
    val selectedRole: DeviceRole? = null,
    val patientName: String = "",
    val patientAge: String = "",
    val patientNotes: String = "",
    val caregiverName: String = "",
    val caregiverPhone: String = "",
    val caregiverRelationship: String = "",
    val remindersEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null,
)

enum class OnboardingField {
    PATIENT_NAME,
    PATIENT_AGE,
    PATIENT_NOTES,
    CAREGIVER_NAME,
    CAREGIVER_PHONE,
    CAREGIVER_RELATIONSHIP,
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val patientRepository: PatientRepository,
    private val familyContactRepository: FamilyContactRepository,
    private val settingsRepository: SettingsRepository,
    private val syncContextRepository: SyncContextRepository,
    private val reminderScheduler: MedicationReminderScheduler,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingScreenState())
    val uiState: StateFlow<OnboardingScreenState> = _uiState.asStateFlow()

    private var patientDataLoaded = false
    private var contactDataLoaded = false
    private var contactId: String? = null
    private var contactCreatedAt: Long? = null

    init {
        viewModelScope.launch {
            onboardingRepository.setupCompletedFlow.collect { completed ->
                _uiState.update { it.copy(setupCompleted = completed) }
            }
        }
        viewModelScope.launch {
            onboardingRepository.deviceRoleFlow.collect { role ->
                _uiState.update { it.copy(savedDeviceRole = role) }
            }
        }
        viewModelScope.launch {
            patientRepository.observeCurrentPatient().collect { patient ->
                if (patient == null || patientDataLoaded) return@collect
                patientDataLoaded = true
                _uiState.update { state ->
                    state.copy(
                        patientName = patient.fullName,
                        patientAge = patient.age?.toString().orEmpty(),
                        patientNotes = patient.notes.orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            familyContactRepository.observePrimaryContact(DEFAULT_PATIENT_ID).collect { contact ->
                if (contact == null || contactDataLoaded) return@collect
                contactDataLoaded = true
                contactId = contact.id
                contactCreatedAt = contact.createdAt
                _uiState.update { state ->
                    state.copy(
                        caregiverName = contact.fullName,
                        caregiverPhone = contact.phone,
                        caregiverRelationship = contact.relationship.orEmpty(),
                    )
                }
            }
        }
    }

    fun selectRole(role: DeviceRole) {
        _uiState.update { it.copy(selectedRole = role, message = null) }
    }

    fun continueFromRoleSelection() {
        val role = _uiState.value.selectedRole
        if (role == null) {
            _uiState.update { it.copy(message = "Elige si eres paciente o cuidador.") }
            return
        }
        _uiState.update { it.copy(step = OnboardingStep.DETAILS, message = null) }
    }

    fun backToRoleSelection() {
        _uiState.update { it.copy(step = OnboardingStep.ROLE_SELECTION, message = null) }
    }

    fun updateField(field: OnboardingField, value: String) {
        _uiState.update { state ->
            when (field) {
                OnboardingField.PATIENT_NAME -> state.copy(patientName = value)
                OnboardingField.PATIENT_AGE -> state.copy(patientAge = value.filter(Char::isDigit).take(3))
                OnboardingField.PATIENT_NOTES -> state.copy(patientNotes = value)
                OnboardingField.CAREGIVER_NAME -> state.copy(caregiverName = value)
                OnboardingField.CAREGIVER_PHONE -> state.copy(caregiverPhone = PhoneValidator.normalize(value))
                OnboardingField.CAREGIVER_RELATIONSHIP -> state.copy(caregiverRelationship = value)
            }
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        _uiState.update { it.copy(remindersEnabled = enabled) }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun completeAfterImport() {
        viewModelScope.launch {
            onboardingRepository.setDeviceRole(DeviceRole.PATIENT)
            syncContextRepository.setMemberRole(DeviceRole.PATIENT.toStorageValue())
            onboardingRepository.setSetupCompleted(true)
        }
    }

    fun saveInitialData() {
        when (_uiState.value.selectedRole) {
            DeviceRole.CAREGIVER -> saveCaregiverSetup()
            DeviceRole.PATIENT, null -> savePatientSetup()
        }
    }

    private fun savePatientSetup() {
        val state = _uiState.value
        val patientName = state.patientName.trim()
        val caregiverName = state.caregiverName.trim()
        val caregiverPhone = state.caregiverPhone.trim()
        val age = state.patientAge.toIntOrNull()

        when {
            patientName.length < 3 -> {
                _uiState.update { it.copy(message = "Escribe el nombre del paciente.") }
                return
            }
            state.patientAge.isNotBlank() && (age == null || age !in 1..120) -> {
                _uiState.update { it.copy(message = "Revisa la edad del paciente.") }
                return
            }
            caregiverName.length < 3 -> {
                _uiState.update { it.copy(message = "Escribe el nombre del familiar o cuidador.") }
                return
            }
            !PhoneValidator.isValid(caregiverPhone) -> {
                _uiState.update { it.copy(message = "Escribe un teléfono válido del cuidador.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            val now = System.currentTimeMillis()
            val existingPatient = patientRepository.getCurrentPatient()
            val existingContact = familyContactRepository.getPrimaryContact(DEFAULT_PATIENT_ID)
            patientRepository.upsertCurrentPatient(
                PatientEntity(
                    id = DEFAULT_PATIENT_ID,
                    fullName = patientName,
                    age = age,
                    notes = state.patientNotes.trim().ifBlank { null },
                    createdAt = existingPatient?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            familyContactRepository.upsertContact(
                FamilyContactEntity(
                    id = contactId ?: existingContact?.id ?: createLocalId("family_contact"),
                    patientId = DEFAULT_PATIENT_ID,
                    fullName = caregiverName,
                    phone = caregiverPhone,
                    relationship = state.caregiverRelationship.trim().ifBlank { "Cuidador" },
                    createdAt = contactCreatedAt ?: existingContact?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            if (settingsRepository.getHealthSettings(DEFAULT_PATIENT_ID) == null) {
                settingsRepository.upsertHealthSettings(
                    DefaultHealthSettings.createForPatient(
                        patientId = DEFAULT_PATIENT_ID,
                        now = now,
                    ),
                )
            }
            settingsRepository.setEasyModeEnabled(true)
            settingsRepository.setVoiceReminderEnabled(true)
            settingsRepository.setVoiceGuidanceEnabled(true)
            settingsRepository.setVoiceRepeatCount(2)
            settingsRepository.setRemindersEnabled(state.remindersEnabled)
            if (state.remindersEnabled) {
                reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            } else {
                reminderScheduler.cancelAllMedicationReminders(DEFAULT_PATIENT_ID)
            }
            finishSetup(DeviceRole.PATIENT)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun saveCaregiverSetup() {
        val caregiverName = _uiState.value.caregiverName.trim()
        if (caregiverName.length < 3) {
            _uiState.update { it.copy(message = "Escribe tu nombre para identificar este celular.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            onboardingRepository.setCaregiverDisplayName(caregiverName)
            settingsRepository.setEasyModeEnabled(false)
            settingsRepository.setRemindersEnabled(false)
            reminderScheduler.cancelAllMedicationReminders(DEFAULT_PATIENT_ID)
            finishSetup(DeviceRole.CAREGIVER)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun finishSetup(role: DeviceRole) {
        onboardingRepository.setDeviceRole(role)
        syncContextRepository.setMemberRole(role.toStorageValue())
        onboardingRepository.setSetupCompleted(true)
        _uiState.update { it.copy(savedDeviceRole = role) }
    }

    fun resolvedUserMode(): UserMode {
        val state = _uiState.value
        return (state.selectedRole ?: state.savedDeviceRole ?: DeviceRole.PATIENT).toUserMode()
    }
}
