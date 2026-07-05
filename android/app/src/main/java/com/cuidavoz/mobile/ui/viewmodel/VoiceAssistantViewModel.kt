package com.cuidavoz.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.cuidavoz.mobile.R
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.data.model.PatientEntity
import com.cuidavoz.mobile.data.repository.DailyStatusRepository
import com.cuidavoz.mobile.data.repository.FamilyContactRepository
import com.cuidavoz.mobile.data.repository.PatientRepository
import com.cuidavoz.mobile.data.repository.PressureRepository
import com.cuidavoz.mobile.data.repository.SettingsRepository
import com.cuidavoz.mobile.domain.DailyStatusSnapshot
import com.cuidavoz.mobile.domain.MedicationGroup
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.domain.MedicationDoseStatus
import com.cuidavoz.mobile.domain.MedicationSkipReason
import com.cuidavoz.mobile.domain.medicationOutcomeUserMessage
import com.cuidavoz.mobile.domain.voice.MedicationVoiceAction
import com.cuidavoz.mobile.domain.voice.MedicationVoiceMatch
import com.cuidavoz.mobile.domain.voice.MedicationVoiceMatcher
import com.cuidavoz.mobile.domain.voice.VoiceIntent
import com.cuidavoz.mobile.domain.voice.VoiceIntentParser
import com.cuidavoz.mobile.reminders.MedicationReminderScheduler
import com.cuidavoz.mobile.reminders.ReminderLaunchState
import com.cuidavoz.mobile.reminders.ReminderPrompt
import com.cuidavoz.mobile.util.ContigoLog
import com.cuidavoz.mobile.util.DEFAULT_PATIENT_ID
import com.cuidavoz.mobile.util.formatScheduleTime
import com.cuidavoz.mobile.util.formatTimeForVoice
import com.cuidavoz.mobile.voice.SpeechRecognitionError
import com.cuidavoz.mobile.voice.SpeechRecognitionManager
import com.cuidavoz.mobile.voice.TextToSpeechManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VoiceAssistantStatus {
    Idle,
    RequestingPermission,
    Preparing,
    Listening,
    Processing,
    Speaking,
    Success,
    ErrorRecoverable,
    PermissionDenied,
    RecognizerUnavailable,
    ConfirmationRequired,
}

sealed class VoiceConfirmation {
    data class Pressure(
        val systolic: Int,
        val diastolic: Int,
        val pulse: Int?,
    ) : VoiceConfirmation()

    data class Medication(
        val scheduleTime: String,
        val medicationNames: List<String>,
        val medication: MedicationEntity?,
    ) : VoiceConfirmation()

    data class MedicationSkip(
        val scheduleTime: String,
        val medication: MedicationEntity,
        val skipReason: MedicationSkipReason,
    ) : VoiceConfirmation()

    data class Help(
        val contact: FamilyContactEntity,
    ) : VoiceConfirmation()
}

data class VoiceAssistantUiState(
    val status: VoiceAssistantStatus = VoiceAssistantStatus.Idle,
    val recognizedText: String? = null,
    val message: String? = null,
    val confirmation: VoiceConfirmation? = null,
    val voiceAssistantEnabled: Boolean = true,
    val voiceReminderEnabled: Boolean = false,
    val voiceRepeatCount: Int = 2,
    val dialPhoneNumber: String? = null,
    val isSpeechRecognizerAvailable: Boolean = true,
    val assistantTitleResId: Int = R.string.voice_title_idle,
    val assistantHintResId: Int = R.string.voice_hint_idle,
    val showRetryActions: Boolean = false,
    val audioLevel: Float = 0f,
) {
    val statusLabelResId: Int
        get() = when (status) {
            VoiceAssistantStatus.Idle -> R.string.voice_btn_idle
            VoiceAssistantStatus.RequestingPermission -> R.string.voice_btn_perm
            VoiceAssistantStatus.Preparing -> R.string.voice_btn_preparing
            VoiceAssistantStatus.Listening -> R.string.voice_btn_listening
            VoiceAssistantStatus.Processing -> R.string.voice_btn_processing
            VoiceAssistantStatus.Speaking -> R.string.voice_btn_speaking
            VoiceAssistantStatus.Success -> R.string.voice_btn_idle
            VoiceAssistantStatus.ErrorRecoverable -> R.string.voice_btn_retry
            VoiceAssistantStatus.PermissionDenied -> R.string.voice_btn_perm
            VoiceAssistantStatus.RecognizerUnavailable -> R.string.voice_btn_fallback
            VoiceAssistantStatus.ConfirmationRequired -> R.string.voice_btn_confirmation
        }

    val showFallbackButtons: Boolean
        get() = status == VoiceAssistantStatus.ErrorRecoverable ||
            status == VoiceAssistantStatus.PermissionDenied ||
            status == VoiceAssistantStatus.RecognizerUnavailable
}

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    patientRepository: PatientRepository,
    familyContactRepository: FamilyContactRepository,
    private val dailyStatusRepository: DailyStatusRepository,
    private val pressureRepository: PressureRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: MedicationReminderScheduler,
    private val reminderLaunchState: ReminderLaunchState,
    private val textToSpeechManager: TextToSpeechManager,
    private val speechRecognitionManager: SpeechRecognitionManager,
) : ViewModel() {
    private var patient: PatientEntity? = null
    private var contact: FamilyContactEntity? = null
    private var dailyStatus: DailyStatusSnapshot? = null
    private var reminderPrompt: ReminderPrompt? = null
    private var lastSpokenPromptNonce: Long? = null
    private var pendingReminderPrompt: ReminderPrompt? = null

    private val _uiState = MutableStateFlow(
        VoiceAssistantUiState(
            isSpeechRecognizerAvailable = speechRecognitionManager.isAvailable(),
        ),
    )
    val uiState: StateFlow<VoiceAssistantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            patientRepository.observeCurrentPatient().collect { currentPatient ->
                patient = currentPatient
            }
        }
        viewModelScope.launch {
            familyContactRepository.observePrimaryContact(DEFAULT_PATIENT_ID).collect { currentContact ->
                contact = currentContact
            }
        }
        viewModelScope.launch {
            dailyStatusRepository.observeDailyStatus(DEFAULT_PATIENT_ID).collect { snapshot ->
                dailyStatus = snapshot
            }
        }
        viewModelScope.launch {
            speechRecognitionManager.rmsLevel.collect { level ->
                if (_uiState.value.status == VoiceAssistantStatus.Listening) {
                    _uiState.update { it.copy(audioLevel = level) }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeVoicePreferences().collect { preferences ->
                _uiState.update { state ->
                    state.copy(
                        voiceAssistantEnabled = preferences.voiceAssistantEnabled,
                        voiceReminderEnabled = preferences.voiceReminderEnabled,
                        voiceRepeatCount = preferences.voiceRepeatCount,
                        isSpeechRecognizerAvailable = speechRecognitionManager.isAvailable(),
                    )
                }
            }
        }
        viewModelScope.launch {
            reminderLaunchState.prompt.collect { prompt ->
                reminderPrompt = prompt
                if (
                    prompt != null &&
                    prompt.nonce != lastSpokenPromptNonce &&
                    uiState.value.voiceReminderEnabled
                ) {
                    if (canPlayReminderPrompt()) {
                        lastSpokenPromptNonce = prompt.nonce
                        speakReminderPrompt(prompt)
                    } else {
                        pendingReminderPrompt = prompt
                    }
                }
            }
        }
    }

    fun onVoiceButtonPressed() {
        ContigoLog.d(VOICE_TAG, "[Contigo][VoiceAssistant] buttonPressed")
    }

    fun onMicrophonePermissionAlreadyGranted() {
        ContigoLog.d(VOICE_TAG, "[Contigo][VoiceAssistant] permissionStatus=granted")
        startListeningFromUserAction()
    }

    fun onMicrophonePermissionRequested() {
        ContigoLog.d(VOICE_TAG, "[Contigo][VoiceAssistant] permissionStatus=denied")
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.RequestingPermission,
                message = context.getString(R.string.voice_msg_perm_req),
                assistantTitleResId = R.string.voice_title_perm_req,
                assistantHintResId = R.string.voice_hint_perm_req,
                showRetryActions = false,
            )
        }
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        ContigoLog.d(VOICE_TAG, "[Contigo][VoiceAssistant] permissionResult=${if (granted) "granted" else "denied"}")
        if (granted) {
            startListeningFromUserAction()
        } else {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.PermissionDenied,
                    message = context.getString(R.string.voice_msg_perm_denied),
                    assistantTitleResId = R.string.voice_title_perm_denied,
                    assistantHintResId = R.string.voice_hint_perm_denied,
                    showRetryActions = false,
                )
            }
        }
    }

    fun onMicrophonePermissionDenied() {
        onMicrophonePermissionResult(granted = false)
    }

    private fun startListeningFromUserAction() {
        if (!uiState.value.voiceAssistantEnabled) {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.Idle,
                    message = context.getString(R.string.voice_msg_disabled),
                    assistantTitleResId = R.string.voice_title_disabled,
                    assistantHintResId = R.string.voice_hint_disabled,
                    showRetryActions = false,
                )
            }
            return
        }
        if (
            uiState.value.status == VoiceAssistantStatus.Listening ||
            uiState.value.status == VoiceAssistantStatus.Processing ||
            uiState.value.status == VoiceAssistantStatus.Preparing ||
            uiState.value.status == VoiceAssistantStatus.Speaking ||
            speechRecognitionManager.isListening()
        ) {
            updateUiState {
                it.copy(
                    message = context.getString(R.string.voice_msg_busy),
                    assistantTitleResId = R.string.voice_title_busy,
                    assistantHintResId = R.string.voice_hint_busy,
                    showRetryActions = false,
                )
            }
            return
        }
        if (!speechRecognitionManager.isAvailable()) {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.RecognizerUnavailable,
                    message = context.getString(R.string.voice_msg_unavailable),
                    isSpeechRecognizerAvailable = false,
                    assistantTitleResId = R.string.voice_title_unavailable,
                    assistantHintResId = R.string.voice_hint_unavailable,
                    showRetryActions = false,
                )
            }
            return
        }

        startListeningFlow()
    }

    fun dismissMessage() {
        updateUiState { it.copy(message = null) }
    }

    fun consumeDialRequest() {
        updateUiState { it.copy(dialPhoneNumber = null) }
    }

    fun retryListening() {
        startListeningFromUserAction()
    }

    fun useButtonsInstead() {
        cancelListeningFlow(showMessage = false)
    }

    fun cancelListeningFlow(showMessage: Boolean = true) {
        textToSpeechManager.stop()
        speechRecognitionManager.cancelListening()
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Idle,
                recognizedText = null,
                confirmation = null,
                message = if (showMessage) context.getString(R.string.voice_msg_cancel) else null,
                assistantTitleResId = R.string.voice_title_idle,
                assistantHintResId = R.string.voice_hint_idle,
                showRetryActions = false,
            )
        }
    }

    fun confirmPendingAction() {
        val confirmation = uiState.value.confirmation ?: return
        viewModelScope.launch {
            when (confirmation) {
                is VoiceConfirmation.Pressure -> confirmPressure(confirmation)
                is VoiceConfirmation.Medication -> confirmMedication(confirmation)
                is VoiceConfirmation.MedicationSkip -> confirmMedicationSkip(confirmation)
                is VoiceConfirmation.Help -> confirmHelp(confirmation)
            }
        }
    }

    fun cancelPendingAction() {
        textToSpeechManager.stop()
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Idle,
                confirmation = null,
                message = context.getString(R.string.voice_msg_op_cancelled),
                assistantTitleResId = R.string.voice_title_op_cancelled,
                assistantHintResId = R.string.voice_hint_op_cancelled,
                showRetryActions = false,
            )
        }
    }

    fun testVoice() {
        if (!uiState.value.voiceAssistantEnabled) {
            updateUiState {
                it.copy(
                    message = context.getString(R.string.voice_msg_disabled),
                    assistantTitleResId = R.string.voice_title_disabled,
                    assistantHintResId = R.string.voice_hint_disabled,
                    showRetryActions = false,
                )
            }
            return
        }
        speakText("Hola. Soy Contigo. Estoy lista para ayudarte.")
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognitionManager.destroy()
        textToSpeechManager.stop()
    }

    private fun startListeningFlow() {
        if (speechRecognitionManager.isListening()) {
            return
        }

        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Preparing,
                recognizedText = null,
                confirmation = null,
                message = null,
                assistantTitleResId = R.string.voice_title_preparing,
                assistantHintResId = R.string.voice_hint_preparing,
                showRetryActions = false,
                isSpeechRecognizerAvailable = speechRecognitionManager.isAvailable(),
            )
        }

        val didSpeak = textToSpeechManager.speak(context.getString(R.string.voice_tts_listening)) { success ->
            if (success) {
                beginRecognizerListening()
            } else {
                handleRecognitionError(
                    SpeechRecognitionError(
                        code = android.speech.SpeechRecognizer.ERROR_CLIENT,
                        userMessage = context.getString(R.string.voice_tts_unknown_retry),
                    ),
                )
            }
        }
        if (!didSpeak) {
            beginRecognizerListening()
        }
    }

    private fun beginRecognizerListening() {
        beginRecognizerListening(expectConfirmation = false)
    }

    private fun beginConfirmationListening() {
        beginRecognizerListening(expectConfirmation = true)
    }

    private fun beginRecognizerListening(expectConfirmation: Boolean) {
        if (speechRecognitionManager.isListening()) {
            return
        }
        if (textToSpeechManager.isSpeaking()) {
            return
        }

        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Listening,
                recognizedText = null,
                message = null,
                assistantTitleResId = R.string.voice_title_busy,
                assistantHintResId = if (expectConfirmation) R.string.voice_hint_confirmation else R.string.voice_hint_listening,
                showRetryActions = false,
            )
        }
        speechRecognitionManager.startListening(
            onResult = ::handleRecognizedText,
            onPartialResult = ::handlePartialRecognizedText,
            onError = ::handleRecognitionError,
        )
    }

    private fun handlePartialRecognizedText(text: String) {
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Listening,
                recognizedText = context.getString(R.string.voice_listening_prefix, text),
                assistantTitleResId = R.string.voice_title_busy,
                assistantHintResId = if (it.confirmation != null) R.string.voice_hint_confirmation else R.string.voice_hint_listening,
                showRetryActions = false,
            )
        }
    }

    private fun handleRecognitionError(error: SpeechRecognitionError) {
        val retryActions = error.code == android.speech.SpeechRecognizer.ERROR_NO_MATCH ||
            error.code == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
        val status = when (error.code) {
            android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceAssistantStatus.PermissionDenied
            android.speech.SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            android.speech.SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            android.speech.SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT,
            android.speech.SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> VoiceAssistantStatus.RecognizerUnavailable
            else -> VoiceAssistantStatus.ErrorRecoverable
        }
        val assistantTitleResId = when (status) {
            VoiceAssistantStatus.PermissionDenied -> R.string.voice_title_perm_denied
            VoiceAssistantStatus.RecognizerUnavailable -> R.string.voice_title_unavailable
            else -> R.string.voice_title_unknown
        }
        val assistantHintResId = when (status) {
            VoiceAssistantStatus.PermissionDenied -> R.string.voice_hint_perm_denied
            VoiceAssistantStatus.RecognizerUnavailable -> R.string.voice_hint_unavailable
            else -> if (uiState.value.confirmation != null) {
                R.string.voice_hint_confirmation
            } else {
                R.string.voice_hint_unknown
            }
        }
        updateUiState {
            it.copy(
                status = status,
                recognizedText = null,
                message = error.userMessage,
                assistantTitleResId = assistantTitleResId,
                assistantHintResId = assistantHintResId,
                showRetryActions = retryActions,
            )
        }
    }

    private fun handleRecognizedText(text: String) {
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Processing,
                recognizedText = context.getString(R.string.voice_recognized_prefix, text),
                message = null,
                assistantTitleResId = R.string.voice_title_processing,
                assistantHintResId = R.string.voice_hint_processing,
                showRetryActions = false,
            )
        }

        viewModelScope.launch {
            val confirmation = uiState.value.confirmation
            when {
                confirmation != null && VoiceIntentParser.isPositiveConfirmation(text) -> {
                    confirmPendingAction()
                }
                confirmation != null && VoiceIntentParser.isNegativeConfirmation(text) -> {
                    cancelPendingAction()
                }
                else -> processVoiceIntent(text)
            }
        }
    }

    private suspend fun processVoiceIntent(text: String) {
        val voiceGroups = resolveMedicationGroupsForVoice()
        val voicePendingMedications = voiceGroups.flatMap { it.pendingMedications }.distinctBy { it.id }
        if (voicePendingMedications.isNotEmpty()) {
            MedicationVoiceMatcher.match(text, voicePendingMedications)?.let { match ->
                val targetGroup = resolveMedicationGroupForMatch(match, voiceGroups)
                if (targetGroup != null) {
                    handleMedicationVoiceMatch(match, targetGroup)
                    return
                }
            }
        }

        when (val intent = VoiceIntentParser.parse(text)) {
            is VoiceIntent.PressureValues -> {
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ConfirmationRequired,
                        confirmation = VoiceConfirmation.Pressure(
                            systolic = intent.systolic,
                            diastolic = intent.diastolic,
                            pulse = intent.pulse,
                        ),
                        assistantTitleResId = R.string.voice_title_confirm_pressure,
                        assistantHintResId = R.string.voice_hint_confirm_pressure,
                        showRetryActions = false,
                    )
                }
                val pulseText = intent.pulse?.let { pulse -> context.getString(R.string.voice_tts_pulse_suffix, pulse) }.orEmpty()
                speakText(
                    context.getString(R.string.voice_tts_pressure_confirm, intent.systolic, intent.diastolic, pulseText),
                    listenAfterSpeaking = true,
                )
            }

            VoiceIntent.RegisterPressure -> {
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ErrorRecoverable,
                        message = context.getString(R.string.voice_msg_pressure_incomplete),
                        assistantTitleResId = R.string.voice_title_pressure_incomplete,
                        assistantHintResId = R.string.voice_hint_pressure_incomplete,
                        showRetryActions = true,
                    )
                }
            }

            VoiceIntent.ConfirmMedicationTaken,
            VoiceIntent.ConfirmAllMedicationsTaken -> {
                val group = resolveMedicationGroup()
                if (group == null || group.pendingCount == 0) {
                    updateUiState {
                        it.copy(
                            status = VoiceAssistantStatus.ErrorRecoverable,
                            message = context.getString(R.string.voice_msg_med_none),
                            assistantTitleResId = R.string.voice_title_med_none,
                            assistantHintResId = R.string.voice_hint_med_none,
                            showRetryActions = false,
                        )
                    }
                    return
                }
                val confirmation = VoiceConfirmation.Medication(
                    scheduleTime = group.scheduleTime,
                    medicationNames = group.pendingMedications.map { it.name },
                    medication = group.pendingMedications.singleOrNull(),
                )
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ConfirmationRequired,
                        confirmation = confirmation,
                        assistantTitleResId = R.string.voice_title_confirm_med,
                        assistantHintResId = R.string.voice_hint_confirm_med,
                        showRetryActions = false,
                    )
                }
                val question = if (confirmation.medication != null) {
                    context.getString(R.string.voice_tts_med_confirm_single, confirmation.medication.name)
                } else {
                    context.getString(R.string.voice_tts_med_confirm_group, formatScheduleTime(group.scheduleTime))
                }
                speakText(question, listenAfterSpeaking = true)
            }

            VoiceIntent.AskForHelp -> {
                val currentContact = contact
                if (currentContact?.phone.isNullOrBlank()) {
                    updateUiState {
                        it.copy(
                            status = VoiceAssistantStatus.ErrorRecoverable,
                            message = context.getString(R.string.voice_msg_no_contact),
                            assistantTitleResId = R.string.voice_title_no_contact,
                            assistantHintResId = R.string.voice_hint_no_contact,
                            showRetryActions = false,
                        )
                    }
                    return
                }
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ConfirmationRequired,
                        confirmation = VoiceConfirmation.Help(currentContact!!),
                        assistantTitleResId = R.string.voice_title_confirm_help,
                        assistantHintResId = R.string.voice_hint_confirm_help,
                        showRetryActions = false,
                    )
                }
                speakText(context.getString(R.string.voice_tts_help_confirm, currentContact.fullName), listenAfterSpeaking = true)
            }

            VoiceIntent.RepeatReminder -> repeatCurrentReminder()
            VoiceIntent.Cancel -> cancelPendingAction()
            VoiceIntent.Unknown -> {
                val likelyMedicationConfirm = VoiceIntentParser.looksLikeMedicationConfirmation(text)
                val group = if (likelyMedicationConfirm) resolveMedicationGroup() else null

                if (group != null && group.pendingCount > 0) {
                    val confirmation = VoiceConfirmation.Medication(
                        scheduleTime = group.scheduleTime,
                        medicationNames = group.pendingMedications.map { it.name },
                        medication = group.pendingMedications.singleOrNull(),
                    )
                    updateUiState {
                        it.copy(
                            status = VoiceAssistantStatus.ConfirmationRequired,
                            confirmation = confirmation,
                            assistantTitleResId = R.string.voice_title_unknown,
                            assistantHintResId = R.string.voice_hint_unknown_med,
                            showRetryActions = false,
                        )
                    }
                    val question = if (confirmation.medication != null) {
                        context.getString(R.string.voice_tts_unknown_med_single, confirmation.medication.name)
                    } else {
                        context.getString(R.string.voice_tts_unknown_med_group, formatScheduleTime(group.scheduleTime))
                    }
                    speakText(question, listenAfterSpeaking = true)
                    return
                }

                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ErrorRecoverable,
                        message = context.getString(R.string.voice_tts_unknown_retry),
                        assistantTitleResId = R.string.voice_title_unknown,
                        assistantHintResId = R.string.voice_hint_unknown,
                        showRetryActions = true,
                    )
                }
                speakText(
                    text = context.getString(R.string.voice_tts_unknown_retry),
                    completionStatus = VoiceAssistantStatus.ErrorRecoverable,
                )
            }
        }
    }

    private suspend fun confirmPressure(confirmation: VoiceConfirmation.Pressure) {
        runCatching {
            pressureRepository.recordPressure(
                patientId = DEFAULT_PATIENT_ID,
                systolic = confirmation.systolic,
                diastolic = confirmation.diastolic,
                pulse = confirmation.pulse,
                notes = null,
            )
        }.onSuccess {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.Success,
                    confirmation = null,
                    message = context.getString(R.string.voice_tts_save_success),
                    assistantTitleResId = R.string.voice_title_idle,
                    assistantHintResId = R.string.voice_hint_idle,
                    showRetryActions = false,
                )
            }
            speakText(
                text = context.getString(R.string.voice_tts_save_success),
                completionStatus = VoiceAssistantStatus.Success,
            )
        }.onFailure {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.ErrorRecoverable,
                    message = context.getString(R.string.voice_msg_save_error),
                    assistantTitleResId = R.string.voice_title_save_error,
                    assistantHintResId = R.string.voice_hint_save_error,
                    showRetryActions = true,
                )
            }
        }
    }

    private suspend fun confirmMedication(confirmation: VoiceConfirmation.Medication) {
        val result = if (confirmation.medication != null) {
            dailyStatusRepository.recordMedicationOutcomes(
                patientId = DEFAULT_PATIENT_ID,
                scheduleTime = confirmation.medication.scheduleTime,
                outcomes = listOf(
                    MedicationDoseOutcome(
                        medicationId = confirmation.medication.id,
                        status = MedicationDoseStatus.TAKEN,
                    ),
                ),
            )
        } else {
            dailyStatusRepository.recordAllPendingAsTaken(
                patientId = DEFAULT_PATIENT_ID,
                scheduleTime = confirmation.scheduleTime,
            )
        }

        if (!result.anyRecorded) {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.ErrorRecoverable,
                    confirmation = null,
                    message = context.getString(R.string.voice_msg_med_already_taken),
                    assistantTitleResId = R.string.voice_title_idle,
                    assistantHintResId = R.string.voice_hint_med_already_taken,
                    showRetryActions = false,
                )
            }
            speakText(
                text = context.getString(R.string.voice_tts_med_already_taken),
                completionStatus = VoiceAssistantStatus.ErrorRecoverable,
            )
            return
        }

        if (result.groupResolved) {
            reminderScheduler.cancelMedicationGroupReminder(
                patientId = DEFAULT_PATIENT_ID,
                scheduleTime = confirmation.scheduleTime,
            )
            reminderLaunchState.clearPrompt()
        }
        val successMessage = medicationOutcomeUserMessage(result)
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Success,
                confirmation = null,
                message = successMessage,
                assistantTitleResId = R.string.voice_title_idle,
                assistantHintResId = R.string.voice_hint_idle,
                showRetryActions = false,
            )
        }
        speakText(
            text = successMessage,
            completionStatus = VoiceAssistantStatus.Success,
        )
    }

    private suspend fun confirmMedicationSkip(confirmation: VoiceConfirmation.MedicationSkip) {
        val result = dailyStatusRepository.recordMedicationOutcomes(
            patientId = DEFAULT_PATIENT_ID,
            scheduleTime = confirmation.scheduleTime,
            outcomes = listOf(
                MedicationDoseOutcome(
                    medicationId = confirmation.medication.id,
                    status = MedicationDoseStatus.SKIPPED,
                    skipReason = confirmation.skipReason,
                ),
            ),
        )

        if (!result.anyRecorded) {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.ErrorRecoverable,
                    confirmation = null,
                    message = context.getString(R.string.voice_msg_med_already_taken),
                    assistantTitleResId = R.string.voice_title_idle,
                    assistantHintResId = R.string.voice_hint_med_already_taken,
                    showRetryActions = false,
                )
            }
            speakText(
                text = context.getString(R.string.voice_tts_med_already_taken),
                completionStatus = VoiceAssistantStatus.ErrorRecoverable,
            )
            return
        }

        if (result.groupResolved) {
            reminderScheduler.cancelMedicationGroupReminder(
                patientId = DEFAULT_PATIENT_ID,
                scheduleTime = confirmation.scheduleTime,
            )
            reminderLaunchState.clearPrompt()
        }
        val successMessage = medicationOutcomeUserMessage(result)
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Success,
                confirmation = null,
                message = successMessage,
                assistantTitleResId = R.string.voice_title_idle,
                assistantHintResId = R.string.voice_hint_idle,
                showRetryActions = false,
            )
        }
        speakText(
            text = successMessage,
            completionStatus = VoiceAssistantStatus.Success,
        )
    }

    private suspend fun handleMedicationVoiceMatch(
        match: MedicationVoiceMatch,
        group: MedicationGroup,
    ) {
        when (match.action) {
            MedicationVoiceAction.ALL_TAKEN -> {
                val confirmation = VoiceConfirmation.Medication(
                    scheduleTime = group.scheduleTime,
                    medicationNames = group.pendingMedications.map { it.name },
                    medication = null,
                )
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ConfirmationRequired,
                        confirmation = confirmation,
                        assistantTitleResId = R.string.voice_title_confirm_med,
                        assistantHintResId = R.string.voice_hint_confirm_med,
                        showRetryActions = false,
                    )
                }
                speakText(
                    context.getString(R.string.voice_tts_med_confirm_group, formatScheduleTime(group.scheduleTime)),
                    listenAfterSpeaking = true,
                )
            }
            MedicationVoiceAction.TAKEN -> {
                val medication = match.medication ?: return
                val confirmation = VoiceConfirmation.Medication(
                    scheduleTime = group.scheduleTime,
                    medicationNames = listOf(medication.name),
                    medication = medication,
                )
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ConfirmationRequired,
                        confirmation = confirmation,
                        assistantTitleResId = R.string.voice_title_confirm_med,
                        assistantHintResId = R.string.voice_hint_confirm_med,
                        showRetryActions = false,
                    )
                }
                speakText(
                    context.getString(R.string.voice_tts_med_confirm_single, medication.name),
                    listenAfterSpeaking = true,
                )
            }
            MedicationVoiceAction.SKIPPED -> {
                val medication = match.medication ?: return
                val skipReason = match.skipReason ?: MedicationSkipReason.OTHER
                val confirmation = VoiceConfirmation.MedicationSkip(
                    scheduleTime = group.scheduleTime,
                    medication = medication,
                    skipReason = skipReason,
                )
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ConfirmationRequired,
                        confirmation = confirmation,
                        assistantTitleResId = R.string.voice_title_skip_med,
                        assistantHintResId = R.string.voice_hint_skip_med,
                        showRetryActions = false,
                    )
                }
                speakText(
                    context.getString(R.string.voice_tts_skip_confirm, medication.name, skipReason.displayLabel()),
                    listenAfterSpeaking = true,
                )
            }
        }
    }

    private suspend fun confirmHelp(confirmation: VoiceConfirmation.Help) {
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Idle,
                confirmation = null,
                dialPhoneNumber = confirmation.contact.phone,
                assistantTitleResId = R.string.voice_title_dialing,
                assistantHintResId = R.string.voice_hint_dialing,
                showRetryActions = false,
            )
        }
        speakText(context.getString(R.string.voice_tts_dialing))
    }

    private fun repeatCurrentReminder() {
        val prompt = reminderPrompt
        if (prompt != null) {
            speakReminderPrompt(prompt)
            return
        }

        val group = resolveMedicationGroup()
        if (group == null) {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.ErrorRecoverable,
                    message = context.getString(R.string.voice_msg_no_reminder),
                    assistantTitleResId = R.string.voice_title_no_reminder,
                    assistantHintResId = R.string.voice_hint_no_reminder,
                    showRetryActions = false,
                )
            }
            return
        }
        val names = group.pendingMedications.map { it.name }
        speakText(buildReminderSpeech(group.scheduleTime, names))
    }

    private fun resolveMedicationGroup(): MedicationGroup? {
        val prompt = reminderPrompt
        val groups = dailyStatus?.medicationGroups.orEmpty()
        val promptedGroup = prompt?.let { reminder ->
            groups.firstOrNull { it.scheduleTime == reminder.scheduleTime && it.pendingCount > 0 }
        }
        return promptedGroup ?: dailyStatus?.nextMedicationGroup
    }

    private fun resolveMedicationGroupsForVoice(): List<MedicationGroup> {
        return dailyStatus?.medicationGroups
            .orEmpty()
            .filter { it.pendingCount > 0 }
            .sortedBy { it.scheduleTime }
    }

    private fun resolveMedicationGroupForMatch(
        match: MedicationVoiceMatch,
        groups: List<MedicationGroup>,
    ): MedicationGroup? {
        val scheduleTime = match.scheduleTime ?: match.medication?.scheduleTime
        return scheduleTime?.let { targetTime ->
            groups.firstOrNull { group -> group.scheduleTime == targetTime }
        }
    }

    private fun speakReminderPrompt(prompt: ReminderPrompt) {
        speakText(
            text = buildReminderSpeech(prompt.scheduleTime, prompt.medicationNames),
            times = uiState.value.voiceRepeatCount,
        )
    }

    private fun buildReminderSpeech(
        scheduleTime: String,
        medicationNames: List<String>,
    ): String {
        val names = medicationNames.joinToString(", ")
        val spokenTime = formatTimeForVoice(scheduleTime)
        return if (medicationNames.size <= 1) {
            context.getString(R.string.voice_tts_reminder_single, medicationNames.firstOrNull().orEmpty(), spokenTime)
        } else {
            context.getString(R.string.voice_tts_reminder_multiple, medicationNames.size, names, spokenTime)
        }
    }

    private fun speakText(
        text: String,
        times: Int = 1,
        completionStatus: VoiceAssistantStatus? = null,
        listenAfterSpeaking: Boolean = false,
    ) {
        updateUiState { it.copy(status = VoiceAssistantStatus.Speaking) }
        val callback: (Boolean) -> Unit = { success ->
            if (!success) {
                updateUiState { state ->
                    state.copy(
                        status = VoiceAssistantStatus.ErrorRecoverable,
                        message = state.message ?: "No pude continuar con la voz. Intenta otra vez.",
                        showRetryActions = true,
                    )
                }
            } else {
                updateUiState { state ->
                    state.copy(
                        status = completionStatus ?: if (state.confirmation != null) {
                            VoiceAssistantStatus.ConfirmationRequired
                        } else {
                            VoiceAssistantStatus.Idle
                        },
                        message = state.message,
                        showRetryActions = state.showRetryActions,
                    )
                }

                if (listenAfterSpeaking && uiState.value.confirmation != null) {
                    beginConfirmationListening()
                }
            }
        }
        if (times > 1) {
            textToSpeechManager.speakRepeated(text, times, callback)
        } else {
            textToSpeechManager.speak(text, callback)
        }
    }

    private fun canPlayReminderPrompt(): Boolean {
        val state = uiState.value
        return state.status == VoiceAssistantStatus.Idle &&
            state.confirmation == null &&
            !speechRecognitionManager.isListening() &&
            !textToSpeechManager.isSpeaking()
    }

    private fun maybeSpeakPendingReminder() {
        val pendingPrompt = pendingReminderPrompt ?: return
        if (!uiState.value.voiceReminderEnabled || !canPlayReminderPrompt()) {
            return
        }
        pendingReminderPrompt = null
        lastSpokenPromptNonce = pendingPrompt.nonce
        speakReminderPrompt(pendingPrompt)
    }

    private fun updateUiState(transform: (VoiceAssistantUiState) -> VoiceAssistantUiState) {
        val updated = transform(_uiState.value)
        _uiState.value = updated
        ContigoLog.d(VOICE_TAG, "[Contigo][VoiceAssistant] state=${updated.status}")
        if (updated.status == VoiceAssistantStatus.Idle) {
            maybeSpeakPendingReminder()
        }
    }

    private companion object {
        const val VOICE_TAG = "ContigoVoiceAssistant"
    }
}
