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
    val assistantTitle: String = "Toca el botón y habla con calma.",
    val assistantHint: String = "Puedes decir tu presión, confirmar una pastilla o pedir ayuda.",
    val showRetryActions: Boolean = false,
    val audioLevel: Float = 0f,
) {
    val statusLabel: String
        get() = when (status) {
            VoiceAssistantStatus.Idle -> "Hablar con Contigo"
            VoiceAssistantStatus.RequestingPermission -> "Permitir micrófono"
            VoiceAssistantStatus.Preparing -> "Preparando..."
            VoiceAssistantStatus.Listening -> "Escuchando..."
            VoiceAssistantStatus.Processing -> "Procesando..."
            VoiceAssistantStatus.Speaking -> "Hablando..."
            VoiceAssistantStatus.Success -> "Hablar con Contigo"
            VoiceAssistantStatus.ErrorRecoverable -> "Intentar voz otra vez"
            VoiceAssistantStatus.PermissionDenied -> "Permitir micrófono"
            VoiceAssistantStatus.RecognizerUnavailable -> "Usar botones"
            VoiceAssistantStatus.ConfirmationRequired -> "Responder a Contigo"
        }

    val showFallbackButtons: Boolean
        get() = status == VoiceAssistantStatus.ErrorRecoverable ||
            status == VoiceAssistantStatus.PermissionDenied ||
            status == VoiceAssistantStatus.RecognizerUnavailable
}

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
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
                message = "Contigo necesita usar el micrófono para escucharte.",
                assistantTitle = "Necesito permiso para escucharte.",
                assistantHint = "Permite el micrófono para hablar con Contigo.",
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
                    message = "Necesito permiso de micrófono para escucharte.",
                    assistantTitle = "No pude usar el micrófono.",
                    assistantHint = "Puedes permitir el micrófono o usar los botones.",
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
                    message = "El asistente de voz está desactivado en Ajustes.",
                    assistantTitle = "Asistente de voz desactivado.",
                    assistantHint = "Puedes activarlo desde Ajustes si quieres volver a usarlo.",
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
                    message = "Todavía te estoy escuchando. Puedes usar los botones si prefieres.",
                    assistantTitle = "Te escucho...",
                    assistantHint = "Habla ahora o usa el botón Cancelar.",
                    showRetryActions = false,
                )
            }
            return
        }
        if (!speechRecognitionManager.isAvailable()) {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.RecognizerUnavailable,
                    message = "Este celular no tiene reconocimiento de voz disponible.",
                    isSpeechRecognizerAvailable = false,
                    assistantTitle = "La voz no está disponible en este celular.",
                    assistantHint = "Puedes seguir usando Contigo con los botones grandes.",
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
                message = if (showMessage) "Puedes seguir usando los botones." else null,
                assistantTitle = "Toca el botón y habla con calma.",
                assistantHint = "Puedes decir tu presión, confirmar una pastilla o pedir ayuda.",
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
                message = "Operación cancelada.",
                assistantTitle = "Operación cancelada.",
                assistantHint = "Puedes volver a intentar o usar los botones.",
                showRetryActions = false,
            )
        }
    }

    fun testVoice() {
        if (!uiState.value.voiceAssistantEnabled) {
            updateUiState {
                it.copy(
                    message = "El asistente de voz está desactivado en Ajustes.",
                    assistantTitle = "Asistente de voz desactivado.",
                    assistantHint = "Actívalo desde Ajustes para probar la voz.",
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
                assistantTitle = "Preparando el micrófono...",
                assistantHint = "En un momento te voy a escuchar.",
                showRetryActions = false,
                isSpeechRecognizerAvailable = speechRecognitionManager.isAvailable(),
            )
        }

        val didSpeak = textToSpeechManager.speak("Te escucho. Habla ahora.") { success ->
            if (success) {
                beginRecognizerListening()
            } else {
                handleRecognitionError(
                    SpeechRecognitionError(
                        code = android.speech.SpeechRecognizer.ERROR_CLIENT,
                        userMessage = "No pude iniciar la guía por voz. Intenta otra vez.",
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
                assistantTitle = "Te escucho...",
                assistantHint = if (expectConfirmation) "Responde sí o no." else "Habla ahora.",
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
                recognizedText = "Te estoy escuchando: $text",
                assistantTitle = "Te escucho...",
                assistantHint = if (it.confirmation != null) "Responde sí o no." else "Habla ahora.",
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
        val assistantTitle = when (status) {
            VoiceAssistantStatus.PermissionDenied -> "Necesito permiso para escucharte."
            VoiceAssistantStatus.RecognizerUnavailable -> "La voz no está disponible en este celular."
            else -> "No pude escucharte bien."
        }
        val assistantHint = when (status) {
            VoiceAssistantStatus.PermissionDenied -> "Puedes permitir el micrófono o usar los botones."
            VoiceAssistantStatus.RecognizerUnavailable -> "Puedes seguir usando los botones grandes."
            else -> if (uiState.value.confirmation != null) {
                "Responde sí o no, o usa los botones."
            } else {
                "Puedes intentar otra vez o usar los botones."
            }
        }
        updateUiState {
            it.copy(
                status = status,
                recognizedText = null,
                message = error.userMessage,
                assistantTitle = assistantTitle,
                assistantHint = assistantHint,
                showRetryActions = retryActions,
            )
        }
    }

    private fun handleRecognizedText(text: String) {
        updateUiState {
            it.copy(
                status = VoiceAssistantStatus.Processing,
                recognizedText = "Te escuché: $text",
                message = null,
                assistantTitle = "Estoy revisando lo que dijiste.",
                assistantHint = "Espera un momento.",
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
        val group = resolveMedicationGroup()
        if (group != null && group.pendingCount > 0) {
            MedicationVoiceMatcher.match(text, group.pendingMedications)?.let { match ->
                handleMedicationVoiceMatch(match, group)
                return
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
                        assistantTitle = "Quiero confirmar tu presión.",
                        assistantHint = "Revisa el dato antes de guardarlo.",
                        showRetryActions = false,
                    )
                }
                val pulseText = intent.pulse?.let { pulse -> " y pulso $pulse" }.orEmpty()
                speakText(
                    "Te escuché: presión ${intent.systolic} sobre ${intent.diastolic}$pulseText. ¿Deseas guardarla?",
                    listenAfterSpeaking = true,
                )
            }

            VoiceIntent.RegisterPressure -> {
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ErrorRecoverable,
                        message = "Dime tu presión, por ejemplo: presión 120 sobre 80.",
                        assistantTitle = "Necesito escuchar tu presión completa.",
                        assistantHint = "Por ejemplo: mi presión es 120 sobre 80.",
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
                            message = "No encontré una pastilla pendiente para registrar.",
                            assistantTitle = "No encontré una pastilla pendiente.",
                            assistantHint = "Puedes usar los botones para revisar tu próxima toma.",
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
                        assistantTitle = "Quiero confirmar tu toma.",
                        assistantHint = "Revisa la pastilla antes de registrarla.",
                        showRetryActions = false,
                    )
                }
                val question = if (confirmation.medication != null) {
                    "¿Confirmas que ya tomaste ${confirmation.medication.name}?"
                } else {
                    "¿Confirmas que ya tomaste tus pastillas de las ${formatScheduleTime(group.scheduleTime)}?"
                }
                speakText(question, listenAfterSpeaking = true)
            }

            VoiceIntent.AskForHelp -> {
                val currentContact = contact
                if (currentContact?.phone.isNullOrBlank()) {
                    updateUiState {
                        it.copy(
                            status = VoiceAssistantStatus.ErrorRecoverable,
                            message = "Configura un contacto familiar en Ajustes.",
                            assistantTitle = "Todavía no hay un contacto familiar.",
                            assistantHint = "Configúralo en Ajustes para pedir ayuda por voz.",
                            showRetryActions = false,
                        )
                    }
                    return
                }
                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ConfirmationRequired,
                        confirmation = VoiceConfirmation.Help(currentContact!!),
                        assistantTitle = "Quiero confirmar la llamada.",
                        assistantHint = "Revisa el contacto antes de llamar.",
                        showRetryActions = false,
                    )
                }
                speakText("¿Deseas llamar a ${currentContact.fullName}?", listenAfterSpeaking = true)
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
                            assistantTitle = "No te escuché bien.",
                            assistantHint = "Pero ¿quieres registrar tu medicina ahora?",
                            showRetryActions = false,
                        )
                    }
                    val question = if (confirmation.medication != null) {
                        "No te entendí bien, pero ¿ya tomaste ${confirmation.medication.name}?"
                    } else {
                        "No te entendí bien, ¿te refieres a que ya tomaste las pastillas de las ${formatScheduleTime(group.scheduleTime)}?"
                    }
                    speakText(question, listenAfterSpeaking = true)
                    return
                }

                updateUiState {
                    it.copy(
                        status = VoiceAssistantStatus.ErrorRecoverable,
                        message = "No pude escucharte bien.",
                        assistantTitle = "No pude escucharte bien.",
                        assistantHint = "Intenta otra vez o usa los botones.",
                        showRetryActions = true,
                    )
                }
                speakText(
                    text = "No pude escucharte bien. Intenta otra vez.",
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
                    message = "Presión registrada correctamente.",
                    assistantTitle = "Presión registrada correctamente.",
                    assistantHint = "Puedes volver a hablar o seguir con los botones.",
                    showRetryActions = false,
                )
            }
            speakText(
                text = "Presión registrada correctamente.",
                completionStatus = VoiceAssistantStatus.Success,
            )
        }.onFailure {
            updateUiState {
                it.copy(
                    status = VoiceAssistantStatus.ErrorRecoverable,
                    message = "No pude guardar la información. Intenta otra vez.",
                    assistantTitle = "No pude guardar la información.",
                    assistantHint = "Intenta otra vez o usa los botones.",
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
                    message = "Esa toma ya fue registrada.",
                    assistantTitle = "Esa toma ya fue registrada.",
                    assistantHint = "Puedes revisar tus botones principales.",
                    showRetryActions = false,
                )
            }
            speakText(
                text = "Esa toma ya fue registrada.",
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
                assistantTitle = successMessage,
                assistantHint = "Puedes volver a hablar si necesitas algo más.",
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
                    message = "Esa toma ya fue registrada.",
                    assistantTitle = "Esa toma ya fue registrada.",
                    assistantHint = "Puedes revisar tus botones principales.",
                    showRetryActions = false,
                )
            }
            speakText(
                text = "Esa toma ya fue registrada.",
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
                assistantTitle = successMessage,
                assistantHint = "Puedes volver a hablar si necesitas algo más.",
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
                        assistantTitle = "Quiero confirmar tu toma.",
                        assistantHint = "Revisa las pastillas antes de registrarlas.",
                        showRetryActions = false,
                    )
                }
                speakText(
                    "¿Confirmas que ya tomaste tus pastillas de las ${formatScheduleTime(group.scheduleTime)}?",
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
                        assistantTitle = "Quiero confirmar tu toma.",
                        assistantHint = "Revisa la pastilla antes de registrarla.",
                        showRetryActions = false,
                    )
                }
                speakText(
                    "¿Confirmas que ya tomaste ${medication.name}?",
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
                        assistantTitle = "Quiero registrar que no pudiste tomarla.",
                        assistantHint = "Revisa el motivo antes de guardarlo.",
                        showRetryActions = false,
                    )
                }
                speakText(
                    "¿Registro que no pudiste tomar ${medication.name} (${skipReason.displayLabel()})?",
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
                assistantTitle = "Abriendo la llamada.",
                assistantHint = "Estoy preparando el teléfono.",
                showRetryActions = false,
            )
        }
        speakText("Abriendo la llamada para pedir ayuda.")
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
                    message = "No hay un recordatorio para repetir ahora.",
                    assistantTitle = "No hay un recordatorio para repetir.",
                    assistantHint = "Puedes revisar tu próxima toma en pantalla.",
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
            "Es hora de tomar tu pastilla. Debes tomar ${medicationNames.firstOrNull().orEmpty()}, una pastilla. Son las $spokenTime."
        } else {
            "Es hora de tomar tus pastillas. Debes tomar ${medicationNames.size} pastillas: $names. Son las $spokenTime."
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
