package com.cuidavoz.mobile.reminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.cuidavoz.mobile.CuidaVozApp
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.voice.ReminderVoiceDecision
import com.cuidavoz.mobile.domain.voice.VoiceIntentParser
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.MedicationImagePreview
import com.cuidavoz.mobile.ui.theme.CuidaVozTheme
import com.cuidavoz.mobile.util.formatScheduleTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReminderUiState(
    val payload: ReminderPayload? = null,
    val medications: List<MedicationEntity> = emptyList(),
    val message: String? = null,
    val listening: Boolean = false,
    val heardText: String? = null,
    val confirmVoiceTaken: Boolean = false,
)

class ReminderActivity : ComponentActivity() {
    private val appContainer: com.cuidavoz.mobile.CuidaVozAppContainer
        get() = (application as CuidaVozApp).appContainer

    private val state = MutableStateFlow(ReminderUiState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        loadReminder(intent)

        setContent {
            CuidaVozTheme {
                ReminderRoute(
                    stateFlow = state.asStateFlow(),
                    onMarkTaken = ::markTaken,
                    onSnooze = ::snooze,
                    onHelp = ::requestHelp,
                    onReplay = ::replayReminder,
                    onVoice = ::startVoiceFlow,
                    onConfirmVoiceTaken = ::confirmVoiceTaken,
                    onDismissVoiceTaken = { updateState { it.copy(confirmVoiceTaken = false) } },
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

    private fun loadReminder(intent: Intent?) {
        val payload = intent?.toReminderPayload() ?: return
        lifecycleScope.launch {
            val meds = appContainer.medicationRepository.getMedicationsByIds(payload.medicationIds)
            updateState {
                it.copy(
                    payload = payload,
                    medications = meds,
                    message = if (intent.getBooleanExtra(EXTRA_OPEN_VOICE, false)) {
                        "Te escucho. Di: Ya tomé."
                    } else {
                        null
                    },
                )
            }
            if (appContainer.settingsRepository.getVoicePreferences().voiceReminderEnabled) {
                val patientName = appContainer.patientRepository.getCurrentPatient()?.fullName?.substringBefore(" ") ?: "María"
                val reminderMessage = MedicationReminderMessageFactory.build(patientName, payload, meds)
                appContainer.textToSpeechManager.speakRepeated(
                    reminderMessage.speech,
                    appContainer.settingsRepository.getVoicePreferences().voiceRepeatCount,
                )
            }
        }
    }

    private fun markTaken() {
        val payload = state.value.payload ?: return
        lifecycleScope.launch {
            val saved = appContainer.reminderScheduler.markReminderTaken(payload)
            if (saved) {
                appContainer.reminderScheduler.cancelReminderGroup(payload.reminderGroupId, payload.scheduleTime)
                appContainer.notificationHelper.showConfirmationNotification("Listo. Toma registrada.", payload)
                finish()
            } else {
                updateState { it.copy(message = "No había una toma pendiente para registrar.") }
            }
        }
    }

    private fun snooze() {
        val payload = state.value.payload ?: return
        lifecycleScope.launch {
            appContainer.reminderScheduler.markReminderSnoozed(payload)
            updateState { it.copy(message = "Te lo recordaré después.") }
            finish()
        }
    }

    private fun requestHelp() {
        val payload = state.value.payload ?: return
        lifecycleScope.launch {
            val phone = appContainer.reminderScheduler.requestHelp(payload)
            if (!phone.isNullOrBlank()) {
                startActivity(
                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
            updateState { it.copy(message = "Ya avisamos al cuidador.") }
        }
    }

    private fun startVoiceFlow() {
        val payload = state.value.payload ?: return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            updateState { it.copy(message = "CuidaVoz necesita avisarte cuando sea hora de tomar tus pastillas.") }
            return
        }
        appContainer.speechRecognitionManager.startListening(
            onResult = { text ->
                val decision = VoiceIntentParser.parseReminderResponse(text, reminderActive = payload.reminderId != null)
                when (decision) {
                    ReminderVoiceDecision.ConfirmTaken -> updateState {
                        it.copy(
                            listening = false,
                            heardText = text,
                            confirmVoiceTaken = true,
                        )
                    }
                    ReminderVoiceDecision.Snooze -> {
                        updateState { it.copy(listening = false, heardText = text) }
                        snooze()
                    }
                    ReminderVoiceDecision.NeedHelp -> {
                        updateState { it.copy(listening = false, heardText = text) }
                        requestHelp()
                    }
                    ReminderVoiceDecision.Uncertain -> updateState {
                        it.copy(
                            listening = false,
                            heardText = text,
                            message = "No estoy seguro. Usa el botón Ya tomé.",
                        )
                    }
                }
            },
            onPartialResult = { partial ->
                updateState { it.copy(listening = true, heardText = partial) }
            },
            onError = {
                updateState { state ->
                    state.copy(listening = false, message = it.userMessage)
                }
            },
        )
        updateState { it.copy(listening = true, message = "Te escucho. Di: Ya tomé.") }
    }

    private fun confirmVoiceTaken() {
        markTaken()
    }

    private fun replayReminder() {
        val payload = state.value.payload ?: return
        val medications = state.value.medications
        lifecycleScope.launch {
            val patientName = appContainer.patientRepository.getCurrentPatient()?.fullName?.substringBefore(" ") ?: "María"
            val message = MedicationReminderMessageFactory.build(patientName, payload, medications)
            val repeatCount = appContainer.settingsRepository.getVoicePreferences().voiceRepeatCount
            appContainer.textToSpeechManager.speakRepeated(message.speech, repeatCount)
        }
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
    onMarkTaken: () -> Unit,
    onSnooze: () -> Unit,
    onHelp: () -> Unit,
    onReplay: () -> Unit,
    onVoice: () -> Unit,
    onConfirmVoiceTaken: () -> Unit,
    onDismissVoiceTaken: () -> Unit,
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
        onMarkTaken = onMarkTaken,
        onSnooze = onSnooze,
        onHelp = onHelp,
        onReplay = onReplay,
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

    if (state.confirmVoiceTaken) {
        val medicationName = state.medications.firstOrNull()?.name ?: state.payload?.medicationNames?.joinToString(", ").orEmpty()
        AlertDialog(
            onDismissRequest = onDismissVoiceTaken,
            title = { Text("¿Confirmas que tomaste $medicationName?") },
            text = { Text("Solo registraremos la toma si confirmas ahora.") },
            confirmButton = {
                AppButton(label = "Sí, ya tomé", onClick = onConfirmVoiceTaken)
            },
            dismissButton = {
                AppButton(label = "No", onClick = onDismissVoiceTaken)
            },
        )
    }
}

@Composable
private fun ReminderScreen(
    state: ReminderUiState,
    onMarkTaken: () -> Unit,
    onSnooze: () -> Unit,
    onHelp: () -> Unit,
    onReplay: () -> Unit,
    onVoice: () -> Unit,
) {
    val payload = state.payload
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            AppCard {
                Text("Es hora de tu pastilla", fontSize = 30.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.medications.size > 1) "Toma estas pastillas" else "Toma esta pastilla",
                    fontSize = 22.sp,
                )
                payload?.let {
                    Text("Hora: ${formatScheduleTime(it.scheduleTime)}", fontSize = 20.sp)
                }
                state.message?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                }
                state.heardText?.let {
                    Text("Escuché: $it", fontSize = 18.sp)
                }
            }
        }
        items(state.medications.ifEmpty { emptyList() }, key = { it.id }) { medication ->
            AppCard {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MedicationImagePreview(
                        imageUri = medication.imageUri,
                        label = medication.name,
                        size = 100.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(medication.name, fontSize = 24.sp)
                        Text(medication.dose, fontSize = 20.sp)
                        Text("Hora: ${formatScheduleTime(medication.scheduleTime)}", fontSize = 18.sp)
                        medication.color?.let { Text("Color: $it", fontSize = 18.sp) }
                        medication.shape?.let { Text("Forma: $it", fontSize = 18.sp) }
                        medication.instructions?.let { Text(it, fontSize = 18.sp) }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(
                    label = if (state.medications.size > 1) "Ya tomé todas" else "Ya tomé",
                    onClick = onMarkTaken,
                )
                AppButton(label = "Recordar después", onClick = onSnooze)
                AppButton(label = "Pedir ayuda", onClick = onHelp)
                AppButton(label = "Escuchar otra vez", onClick = onReplay)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onVoice,
                ) {
                    Text("Responder hablando")
                }
            }
        }
    }
}
