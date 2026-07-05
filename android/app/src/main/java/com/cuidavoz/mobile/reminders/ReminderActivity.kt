package com.cuidavoz.mobile.reminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cuidavoz.mobile.ContigoApp
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.domain.MedicationDoseStatus
import com.cuidavoz.mobile.domain.MedicationOutcomeResult
import com.cuidavoz.mobile.domain.medicationOutcomeUserMessage
import com.cuidavoz.mobile.domain.voice.MedicationVoiceAction
import com.cuidavoz.mobile.domain.voice.MedicationVoiceMatch
import com.cuidavoz.mobile.domain.voice.MedicationVoiceMatcher
import com.cuidavoz.mobile.domain.voice.ReminderVoiceDecision
import com.cuidavoz.mobile.domain.voice.VoiceIntentParser
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ConfirmMedicationDialog
import com.cuidavoz.mobile.ui.components.MedicationImagePreview
import com.cuidavoz.mobile.ui.theme.ContigoTheme
import com.cuidavoz.mobile.util.formatScheduleTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val ReminderButtonMinHeight = 76.dp
private val ReminderButtonTextSize = 28.sp

data class ReminderUiState(
    val payload: ReminderPayload? = null,
    val medications: List<MedicationEntity> = emptyList(),
    val pendingMedications: List<MedicationEntity> = emptyList(),
    val voicePendingMedications: List<MedicationEntity> = emptyList(),
    val voicePayloadsByScheduleTime: Map<String, ReminderPayload> = emptyMap(),
    val message: String? = null,
    val listening: Boolean = false,
    val heardText: String? = null,
    val showConfirmDialog: Boolean = false,
)

private data class ReminderVoiceContext(
    val pendingMedications: List<MedicationEntity>,
    val payloadsByScheduleTime: Map<String, ReminderPayload>,
)

class ReminderActivity : ComponentActivity() {
    private val appContainer: com.cuidavoz.mobile.ContigoAppContainer
        get() = (application as ContigoApp).appContainer

    private val state = MutableStateFlow(ReminderUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge()
        loadReminder(intent)

        setContent {
            ContigoTheme {
                ReminderRoute(
                    stateFlow = state.asStateFlow(),
                    onRegisterTomas = ::openConfirmDialog,
                    onSaveOutcomes = ::saveOutcomes,
                    onDismissDialog = ::dismissConfirmDialog,
                    onSnooze = ::snooze,
                    onVoice = ::startVoiceFlow,
                    onRequestHelp = ::requestHelp,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        loadReminder(intent)
    }

    override fun onStop() {
        super.onStop()
        appContainer.speechRecognitionManager.cancelListening()
    }

    override fun onDestroy() {
        runCatching { appContainer.textToSpeechManager.stop() }
        super.onDestroy()
    }

    private fun loadReminder(intent: Intent?) {
        val payload = intent?.toReminderPayload() ?: return
        lifecycleScope.launch {
            reloadPendingMedications(payload)
            val openVoice = intent.getBooleanExtra(EXTRA_OPEN_VOICE, false)
            updateState {
                it.copy(
                    message = when {
                        openVoice -> "Pulsa «Hablar» y di el nombre, color, forma, orden u hora."
                        else -> null
                    },
                )
            }
            val speechByService = intent.getBooleanExtra(EXTRA_SPEECH_BY_SERVICE, false)
            if (
                appContainer.settingsRepository.getVoicePreferences().voiceReminderEnabled &&
                !speechByService
            ) {
                val patientName = appContainer.patientRepository.getCurrentPatient()?.fullName?.substringBefore(" ") ?: "paciente"
                val reminderMessage = MedicationReminderMessageFactory.build(
                    patientName,
                    payload,
                    state.value.pendingMedications.ifEmpty { state.value.medications },
                )
                appContainer.textToSpeechManager.configureForMedicationReminders()
                appContainer.textToSpeechManager.speakRepeated(
                    reminderMessage.speech,
                    appContainer.settingsRepository.getVoicePreferences().voiceRepeatCount,
                )
            }
            if (openVoice) {
                startVoiceFlow()
            }
        }
    }

    private suspend fun reloadPendingMedications(payload: ReminderPayload) {
        val pendingIds = appContainer.dailyStatusRepository
            .getPendingMedicationIdsForScheduleTime(payload.patientId, payload.scheduleTime)
            .toSet()
        val meds = appContainer.medicationRepository
            .getMedicationsByIds(payload.medicationIds)
            .orderedForReminderPayload(payload)
        val pendingMeds = meds.filter { it.id in pendingIds }
        val voiceContext = loadVoiceReminderContext(payload)
        updateState {
            it.copy(
                payload = payload,
                medications = meds,
                pendingMedications = pendingMeds,
                voicePendingMedications = voiceContext.pendingMedications,
                voicePayloadsByScheduleTime = voiceContext.payloadsByScheduleTime,
            )
        }
        if (pendingMeds.isEmpty() && meds.isNotEmpty()) {
            appContainer.reminderScheduler.cancelReminderGroup(payload.reminderGroupId, payload.scheduleTime)
            finish()
        }
    }

    private suspend fun loadVoiceReminderContext(payload: ReminderPayload): ReminderVoiceContext {
        val now = System.currentTimeMillis()
        val duePayloads = appContainer.medicationReminderRepository
            .getScheduledReminders(payload.patientId)
            .filter { reminder ->
                reminder.scheduledAt <= now &&
                    (payload.targetDate.isBlank() || reminder.targetDate == payload.targetDate)
            }
            .map { it.toReminderPayload() }
        val payloadsByScheduleTime = (duePayloads + payload)
            .groupBy { it.scheduleTime }
            .mapValues { (_, values) -> values.maxByOrNull { it.scheduledAt } ?: values.first() }
        val orderedPayloads = payloadsByScheduleTime.values
            .sortedWith(compareBy<ReminderPayload> { it.scheduledAt }.thenBy { it.scheduleTime })
        val medicationIds = orderedPayloads.flatMap { it.medicationIds }.distinct()
        val medications = appContainer.medicationRepository.getMedicationsByIds(medicationIds)
        val pendingMedications = orderedPayloads.flatMap { reminderPayload ->
            val pendingIds = appContainer.dailyStatusRepository
                .getPendingMedicationIdsForScheduleTime(reminderPayload.patientId, reminderPayload.scheduleTime)
                .toSet()
            medications
                .orderedForReminderPayload(reminderPayload)
                .filter { medication -> medication.id in pendingIds }
        }.distinctBy { it.id }
        return ReminderVoiceContext(
            pendingMedications = pendingMedications,
            payloadsByScheduleTime = payloadsByScheduleTime,
        )
    }

    private fun openConfirmDialog() {
        val pending = state.value.pendingMedications
        if (pending.isEmpty()) {
            updateState { it.copy(message = "No hay pastillas pendientes para registrar.") }
            return
        }
        updateState { it.copy(showConfirmDialog = true, message = null) }
    }

    private fun dismissConfirmDialog() {
        updateState { it.copy(showConfirmDialog = false) }
    }

    private fun saveOutcomes(outcomes: List<MedicationDoseOutcome>) {
        val payload = state.value.payload ?: return
        saveOutcomes(payload, outcomes)
    }

    private fun saveOutcomes(
        payload: ReminderPayload,
        outcomes: List<MedicationDoseOutcome>,
    ) {
        lifecycleScope.launch {
            val result = appContainer.reminderScheduler.recordReminderOutcomes(payload, outcomes)
            handleOutcomeResult(result, payload)
        }
    }

    private fun recordAllPendingTaken(scheduleTime: String? = null) {
        val payload = scheduleTime?.let { state.value.voicePayloadsByScheduleTime[it] }
            ?: state.value.payload
            ?: return
        val pending = if (scheduleTime == null) {
            state.value.pendingMedications
        } else {
            state.value.voicePendingMedications.filter { it.scheduleTime == scheduleTime }
        }
        if (pending.isEmpty()) return
        saveOutcomes(
            payload,
            pending.map {
                MedicationDoseOutcome(
                    medicationId = it.id,
                    status = MedicationDoseStatus.TAKEN,
                )
            },
        )
    }

    private suspend fun handleOutcomeResult(result: MedicationOutcomeResult, payload: ReminderPayload) {
        val currentPayload = state.value.payload
        val message = medicationOutcomeUserMessage(result)
        if (!result.anyRecorded) {
            updateState {
                it.copy(
                    message = message,
                    showConfirmDialog = false,
                )
            }
            return
        }
        reloadPendingMedications(currentPayload ?: payload)
        if (result.groupResolved) {
            appContainer.reminderScheduler.cancelReminderGroup(payload.reminderGroupId, payload.scheduleTime)
            appContainer.notificationHelper.showConfirmationNotification(
                message,
                payload,
            )
            if (currentPayload?.scheduleTime == payload.scheduleTime) {
                appContainer.textToSpeechManager.stop()
                MedicationReminderVoiceService.stop(this)
                finish()
            } else {
                updateState {
                    it.copy(
                        message = message,
                        showConfirmDialog = false,
                    )
                }
            }
        } else {
            updateState {
                it.copy(
                    message = message,
                    showConfirmDialog = false,
                )
            }
        }
    }

    private fun snooze() {
        val payload = state.value.payload ?: return
        lifecycleScope.launch {
            appContainer.reminderScheduler.markReminderSnoozed(payload)
            appContainer.notificationHelper.showConfirmationNotification("Te lo recordaré en un rato.", payload)
            appContainer.textToSpeechManager.stop()
            MedicationReminderVoiceService.stop(this@ReminderActivity)
            finish()
        }
    }

    private fun requestHelp() {
        val payload = state.value.payload ?: return
        lifecycleScope.launch {
            val phone = appContainer.reminderScheduler.requestHelp(payload)
            if (!phone.isNullOrBlank()) {
                startActivity(
                    Intent(Intent.ACTION_DIAL, "tel:$phone".toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
            appContainer.notificationHelper.showConfirmationNotification("Se avisó al cuidador.", payload)
        }
    }

    private fun startVoiceFlow() {
        val payload = state.value.payload ?: return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            updateState { it.copy(message = "Permite el micrófono para poder hablar con Contigo.") }
            return
        }
        appContainer.speechRecognitionManager.startListening(
            onResult = { text -> handleVoiceResult(text, payload) },
            onPartialResult = { partial ->
                updateState { it.copy(listening = true, heardText = partial) }
            },
            onError = {
                updateState { state ->
                    state.copy(listening = false, message = it.userMessage)
                }
            },
        )
        updateState {
            it.copy(
                listening = true,
                message = "Te escucho. Di «ya tomé la primera», «la roja» o «la de las 3:30».",
            )
        }
    }

    private fun handleVoiceResult(text: String, payload: ReminderPayload) {
        updateState { it.copy(listening = false, heardText = text) }
        val pending = state.value.voicePendingMedications.ifEmpty { state.value.pendingMedications }

        MedicationVoiceMatcher.match(text, pending)?.let { match ->
            when (match.action) {
                MedicationVoiceAction.ALL_TAKEN -> recordAllPendingTaken(match.scheduleTime)
                MedicationVoiceAction.TAKEN -> {
                    val medication = match.medication ?: return
                    saveOutcomes(
                        payloadForMatch(match) ?: return,
                        listOf(
                            MedicationDoseOutcome(
                                medicationId = medication.id,
                                status = MedicationDoseStatus.TAKEN,
                            ),
                        ),
                    )
                }
                MedicationVoiceAction.SKIPPED -> {
                    val medication = match.medication ?: return
                    saveOutcomes(
                        payloadForMatch(match) ?: return,
                        listOf(
                            MedicationDoseOutcome(
                                medicationId = medication.id,
                                status = MedicationDoseStatus.SKIPPED,
                                skipReason = match.skipReason,
                            ),
                        ),
                    )
                }
            }
            return
        }

        when (
            VoiceIntentParser.parseReminderResponse(
                input = text,
                reminderActive = payload.reminderId != null,
                pendingMedications = pending,
            )
        ) {
            ReminderVoiceDecision.ConfirmTaken -> recordAllPendingTaken()
            ReminderVoiceDecision.ConfirmMedicationTaken,
            ReminderVoiceDecision.ConfirmMedicationSkipped,
            -> {
                updateState {
                    it.copy(message = "Di el nombre, color, forma, orden u hora de la pastilla.")
                }
            }
            ReminderVoiceDecision.Snooze -> snooze()
            ReminderVoiceDecision.NeedHelp -> requestHelp()
            ReminderVoiceDecision.Uncertain -> {
                updateState {
                    it.copy(message = "No entendí bien. Di «ya tomé la primera», «la roja» o «la de las 3:30».")
                }
            }
        }
    }

    private fun payloadForMatch(match: MedicationVoiceMatch): ReminderPayload? {
        val scheduleTime = match.scheduleTime ?: match.medication?.scheduleTime
        return scheduleTime?.let { state.value.voicePayloadsByScheduleTime[it] } ?: state.value.payload
    }

    private fun updateState(transform: (ReminderUiState) -> ReminderUiState) {
        state.value = transform(state.value)
    }

    companion object {
        fun createIntent(
            context: Context,
            payload: ReminderPayload,
            openVoice: Boolean = false,
        ): Intent {
            return Intent(context, ReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtras(payload.toReceiverIntent().extras ?: Bundle())
                putExtra(EXTRA_OPEN_VOICE, openVoice)
            }
        }
    }
}

@Composable
private fun ReminderRoute(
    stateFlow: kotlinx.coroutines.flow.StateFlow<ReminderUiState>,
    onRegisterTomas: () -> Unit,
    onSaveOutcomes: (List<MedicationDoseOutcome>) -> Unit,
    onDismissDialog: () -> Unit,
    onSnooze: () -> Unit,
    onVoice: () -> Unit,
    onRequestHelp: () -> Unit,
) {
    val state by stateFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onVoice()
    }

    ReminderScreen(
        state = state,
        onRegisterTomas = onRegisterTomas,
        onSnooze = onSnooze,
        onVoice = {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                onVoice()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    )

    if (state.showConfirmDialog && state.pendingMedications.isNotEmpty()) {
        ConfirmMedicationDialog(
            medications = state.pendingMedications,
            scheduleTime = state.payload?.scheduleTime,
            onSave = onSaveOutcomes,
            onDismiss = onDismissDialog,
            onRequestHelp = {
                onDismissDialog()
                onRequestHelp()
            },
        )
    }
}

@Composable
private fun ReminderScreen(
    state: ReminderUiState,
    onRegisterTomas: () -> Unit,
    onSnooze: () -> Unit,
    onVoice: () -> Unit,
) {
    val payload = state.payload
    val pending = state.pendingMedications
    val extraColors = ContigoTheme.extraColors
    val registerLabel = if (pending.size > 1) "Registrar tomas" else "Ya tomé"
    val voiceLabel = when {
        state.listening -> "Escuchando…"
        else -> "Hablar"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            AppCard {
                Text(
                    text = "Es hora de tu pastilla",
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (pending.size > 1) {
                        "Toma estas pastillas ahora"
                    } else {
                        "Toma esta pastilla ahora"
                    },
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
                payload?.let {
                    Text(
                        text = "Hora: ${formatScheduleTime(it.scheduleTime)}",
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    )
                }
                state.message?.let {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        items(pending, key = { it.id }) { medication ->
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MedicationImagePreview(
                        imageUri = medication.imageUri,
                        label = medication.name,
                        size = 108.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = medication.name,
                            fontSize = 26.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = medication.dose,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        )
                        medication.instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
                            Text(
                                text = instructions,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AppButton(
                    label = registerLabel,
                    onClick = onRegisterTomas,
                    enabled = pending.isNotEmpty(),
                    minHeight = ReminderButtonMinHeight,
                    textSize = ReminderButtonTextSize,
                    contentDescription = registerLabel,
                )
                AppButton(
                    label = "Posponerlo",
                    onClick = onSnooze,
                    minHeight = ReminderButtonMinHeight,
                    textSize = ReminderButtonTextSize,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    contentDescription = "Posponer recordatorio",
                )
                AppButton(
                    label = voiceLabel,
                    onClick = onVoice,
                    enabled = !state.listening && pending.isNotEmpty(),
                    icon = Icons.Outlined.Mic,
                    minHeight = ReminderButtonMinHeight,
                    textSize = ReminderButtonTextSize,
                    containerColor = extraColors.voiceButtonBackground,
                    contentColor = extraColors.statusText,
                    contentDescription = "Hablar con Contigo",
                )
                Text(
                    text = "Puedes decir «ya tomé la primera», «la roja», «la de las 3:30» o «no pude tomar la aspirina».",
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
