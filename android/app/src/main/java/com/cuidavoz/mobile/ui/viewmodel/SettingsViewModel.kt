package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.HealthSettingsEntity
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.createLocalId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsScreenState(
    val familyName: String = "",
    val familyPhone: String = "",
    val familyRelationship: String = "",
    val systolicMinNormal: String = "",
    val systolicMaxNormal: String = "",
    val diastolicMinNormal: String = "",
    val diastolicMaxNormal: String = "",
    val pulseMinNormal: String = "",
    val pulseMaxNormal: String = "",
    val doctorRecommendation: String = "",
    val remindersEnabled: Boolean = false,
    val repeatIntervalMinutes: Int = 10,
    val maxRepeatCount: Int = 3,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notifyCaregiverOnMissed: Boolean = true,
    val canScheduleExactAlarms: Boolean = true,
    val voiceAssistantEnabled: Boolean = true,
    val voiceReminderEnabled: Boolean = false,
    val voiceRepeatCount: Int = 2,
    val easyModeEnabled: Boolean = false,
    val voiceGuidanceEnabled: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val familyContactRepository: FamilyContactRepository,
    private val reminderScheduler: MedicationReminderScheduler,
) : ViewModel() {
    private var currentSettingsId: String? = null
    private var currentContactId: String? = null
    private var currentContactCreatedAt: Long? = null
    private val _uiState = MutableStateFlow(SettingsScreenState())
    val uiState: StateFlow<SettingsScreenState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeHealthSettings(DEFAULT_PATIENT_ID).collect { settings ->
                settings ?: return@collect
                currentSettingsId = settings.id
                _uiState.update { state ->
                    state.copy(
                        systolicMinNormal = settings.systolicMinNormal.toString(),
                        systolicMaxNormal = settings.systolicMaxNormal.toString(),
                        diastolicMinNormal = settings.diastolicMinNormal.toString(),
                        diastolicMaxNormal = settings.diastolicMaxNormal.toString(),
                        pulseMinNormal = settings.pulseMinNormal.toString(),
                        pulseMaxNormal = settings.pulseMaxNormal.toString(),
                        doctorRecommendation = settings.doctorRecommendation.orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            familyContactRepository.observePrimaryContact(DEFAULT_PATIENT_ID).collect { contact ->
                contact ?: return@collect
                currentContactId = contact.id
                currentContactCreatedAt = contact.createdAt
                _uiState.update { state ->
                    state.copy(
                        familyName = contact.fullName,
                        familyPhone = contact.phone,
                        familyRelationship = contact.relationship.orEmpty(),
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeReminderPreferences().collect { preferences ->
                _uiState.update { state ->
                    state.copy(
                        remindersEnabled = preferences.remindersEnabled,
                        repeatIntervalMinutes = preferences.repeatIntervalMinutes,
                        maxRepeatCount = preferences.maxRepeatCount,
                        soundEnabled = preferences.soundEnabled,
                        vibrationEnabled = preferences.vibrationEnabled,
                        notifyCaregiverOnMissed = preferences.notifyCaregiverOnMissed,
                        canScheduleExactAlarms = reminderScheduler.canScheduleExactAlarms(),
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeVoicePreferences().collect { preferences ->
                _uiState.update { state ->
                    state.copy(
                        voiceAssistantEnabled = true,
                        voiceReminderEnabled = preferences.voiceReminderEnabled,
                        voiceRepeatCount = preferences.voiceRepeatCount,
                        easyModeEnabled = preferences.easyModeEnabled,
                        voiceGuidanceEnabled = preferences.voiceGuidanceEnabled,
                    )
                }
            }
        }
    }

    fun updateField(field: SettingsField, value: String) {
        _uiState.update { state ->
            when (field) {
                SettingsField.SYSTOLIC_MIN -> state.copy(systolicMinNormal = value.filter(Char::isDigit))
                SettingsField.SYSTOLIC_MAX -> state.copy(systolicMaxNormal = value.filter(Char::isDigit))
                SettingsField.DIASTOLIC_MIN -> state.copy(diastolicMinNormal = value.filter(Char::isDigit))
                SettingsField.DIASTOLIC_MAX -> state.copy(diastolicMaxNormal = value.filter(Char::isDigit))
                SettingsField.PULSE_MIN -> state.copy(pulseMinNormal = value.filter(Char::isDigit))
                SettingsField.PULSE_MAX -> state.copy(pulseMaxNormal = value.filter(Char::isDigit))
                SettingsField.RECOMMENDATION -> state.copy(doctorRecommendation = value)
            }
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun updateContactField(field: ContactField, value: String) {
        _uiState.update { state ->
            when (field) {
                ContactField.NAME -> state.copy(familyName = value)
                ContactField.PHONE -> state.copy(familyPhone = value)
                ContactField.RELATIONSHIP -> state.copy(familyRelationship = value)
            }
        }
    }

    fun saveContact() {
        val state = _uiState.value
        if (state.familyName.isBlank() || state.familyPhone.trim().length < 6) {
            _uiState.update { it.copy(message = "Revisa los datos del contacto.") }
            return
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            familyContactRepository.upsertContact(
                FamilyContactEntity(
                    id = currentContactId ?: createLocalId("family_contact"),
                    patientId = DEFAULT_PATIENT_ID,
                    fullName = state.familyName.trim(),
                    phone = state.familyPhone.trim(),
                    relationship = state.familyRelationship.trim().ifBlank { null },
                    createdAt = currentContactCreatedAt ?: now,
                    updatedAt = now,
                ),
            )
            _uiState.update { it.copy(message = "Contacto guardado correctamente.") }
        }
    }

    fun saveSettings() {
        val state = _uiState.value
        val systolicMin = state.systolicMinNormal.toIntOrNull()
        val systolicMax = state.systolicMaxNormal.toIntOrNull()
        val diastolicMin = state.diastolicMinNormal.toIntOrNull()
        val diastolicMax = state.diastolicMaxNormal.toIntOrNull()
        val pulseMin = state.pulseMinNormal.toIntOrNull()
        val pulseMax = state.pulseMaxNormal.toIntOrNull()

        val invalid =
            listOf(systolicMin, systolicMax, diastolicMin, diastolicMax, pulseMin, pulseMax).any {
                it == null || it <= 0
            } ||
                systolicMin!! >= systolicMax!! ||
                diastolicMin!! >= diastolicMax!! ||
                pulseMin!! >= pulseMax!!

        if (invalid) {
            _uiState.update {
                it.copy(
                    message = "Revisa los rangos ingresados. El mínimo debe ser menor que el máximo.",
                )
            }
            return
        }

        viewModelScope.launch {
            settingsRepository.upsertHealthSettings(
                HealthSettingsEntity(
                    id = currentSettingsId ?: createLocalId("health_settings"),
                    patientId = DEFAULT_PATIENT_ID,
                    systolicMinNormal = systolicMin,
                    systolicMaxNormal = systolicMax,
                    diastolicMinNormal = diastolicMin,
                    diastolicMaxNormal = diastolicMax,
                    pulseMinNormal = pulseMin,
                    pulseMaxNormal = pulseMax,
                    doctorRecommendation = state.doctorRecommendation.trim().ifBlank { null },
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            _uiState.update { it.copy(message = "Rangos guardados correctamente.") }
        }
    }

    fun toggleReminders(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setRemindersEnabled(enabled)
            if (enabled) {
                reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            } else {
                reminderScheduler.cancelAllMedicationReminders(DEFAULT_PATIENT_ID)
            }
            _uiState.update {
                it.copy(
                    canScheduleExactAlarms = reminderScheduler.canScheduleExactAlarms(),
                    message = if (enabled) {
                        "Recordatorios activados."
                    } else {
                        "Recordatorios desactivados."
                    },
                )
            }
        }
    }

    fun reprogramReminders() {
        viewModelScope.launch {
            reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            _uiState.update {
                it.copy(
                    canScheduleExactAlarms = reminderScheduler.canScheduleExactAlarms(),
                    message = "Recordatorios reprogramados.",
                )
            }
        }
    }

    fun setVoiceAssistantEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVoiceAssistantEnabled(true)
            _uiState.update {
                it.copy(message = "El asistente de voz queda activo siempre.")
            }
        }
    }

    fun setVoiceReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVoiceReminderEnabled(enabled)
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Lectura de recordatorios activada."
                    } else {
                        "Lectura de recordatorios desactivada."
                    },
                )
            }
        }
    }

    fun setVoiceRepeatCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setVoiceRepeatCount(count)
            _uiState.update { it.copy(message = "Repetición de voz guardada.") }
        }
    }

    fun setEasyModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEasyModeEnabled(enabled)
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Modo fácil activado."
                    } else {
                        "Modo fácil desactivado."
                    },
                )
            }
        }
    }

    fun setVoiceGuidanceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVoiceGuidanceEnabled(enabled)
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Guía por voz activada."
                    } else {
                        "Guía por voz desactivada."
                    },
                )
            }
        }
    }

    fun setRepeatIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.setRepeatIntervalMinutes(minutes)
            reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            _uiState.update { it.copy(message = "Nuevo tiempo de repetición guardado.") }
        }
    }

    fun setMaxRepeatCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setMaxRepeatCount(count)
            reminderScheduler.scheduleAllMedicationReminders(DEFAULT_PATIENT_ID)
            _uiState.update { it.copy(message = "Nuevo máximo de avisos guardado.") }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
            _uiState.update { it.copy(message = if (enabled) "Sonido activado." else "Sonido desactivado.") }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVibrationEnabled(enabled)
            _uiState.update { it.copy(message = if (enabled) "Vibración activada." else "Vibración desactivada.") }
        }
    }

    fun setNotifyCaregiverOnMissed(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotifyCaregiverOnMissed(enabled)
            _uiState.update {
                it.copy(
                    message = if (enabled) {
                        "Avisaremos al cuidador si no responde."
                    } else {
                        "Ya no avisaremos al cuidador por falta de respuesta."
                    },
                )
            }
        }
    }
}

enum class SettingsField {
    SYSTOLIC_MIN,
    SYSTOLIC_MAX,
    DIASTOLIC_MIN,
    DIASTOLIC_MAX,
    PULSE_MIN,
    PULSE_MAX,
    RECOMMENDATION,
}

enum class ContactField {
    NAME,
    PHONE,
    RELATIONSHIP,
}
