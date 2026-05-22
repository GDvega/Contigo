package com.cuidavoz.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.MedicationImageGroupPreview
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.components.VoiceAssistantButton
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantStatus
import com.cuidavoz.mobile.ui.viewmodel.HomeViewModel
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel
import com.cuidavoz.mobile.ui.viewmodel.VoiceConfirmation
import com.cuidavoz.mobile.util.formatScheduleTime

@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    voiceAssistantViewModel: VoiceAssistantViewModel,
    easyModeEnabled: Boolean,
    showSpeakScreenButton: Boolean,
    onSpeakScreen: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceUiState by voiceAssistantViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPressureDialog by rememberSaveable { mutableStateOf(false) }
    var showMedicationConfirmation by rememberSaveable { mutableStateOf(false) }
    var systolic by rememberSaveable { mutableStateOf("") }
    var diastolic by rememberSaveable { mutableStateOf("") }
    var pulse by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    ToastMessageEffect(
        message = uiState.actionMessage,
        onConsumed = viewModel::dismissMessage,
    )

    LaunchedEffect(voiceUiState.dialPhoneNumber) {
        val phone = voiceUiState.dialPhoneNumber ?: return@LaunchedEffect
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}")))
        voiceAssistantViewModel.consumeDialRequest()
    }

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "CuidaVoz",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = uiState.greeting,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = uiState.generalStatusText,
            fontSize = 24.sp,
            lineHeight = 30.sp,
        )

        AppCard {
            Text(
                text = "Acciones principales",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            uiState.primaryActionLabel?.let { label ->
                AppButton(
                    label = label,
                    onClick = { showMedicationConfirmation = true },
                    icon = Icons.Outlined.CheckCircle,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            AppButton(
                label = "Registrar presión",
                onClick = { showPressureDialog = true },
                icon = Icons.Outlined.Edit,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Pedir ayuda",
                onClick = {
                    val phone = uiState.helpPhone
                    if (phone.isNullOrBlank()) {
                        viewModel.missingContactMessage()
                    } else {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}"))
                        )
                    }
                },
                icon = Icons.Outlined.Call,
            )
            Spacer(modifier = Modifier.height(12.dp))
            VoiceAssistantButton(
                state = voiceUiState,
                onButtonPressed = voiceAssistantViewModel::onVoiceButtonPressed,
                onPermissionAlreadyGranted = voiceAssistantViewModel::onMicrophonePermissionAlreadyGranted,
                onPermissionRequestStarted = voiceAssistantViewModel::onMicrophonePermissionRequested,
                onPermissionResult = voiceAssistantViewModel::onMicrophonePermissionResult,
                icon = Icons.Outlined.Mic,
            )
            if (showSpeakScreenButton) {
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Escuchar esta pantalla",
                    onClick = onSpeakScreen,
                    icon = Icons.Outlined.Hearing,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = voiceUiState.assistantTitle,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = voiceUiState.recognizedText ?: voiceUiState.assistantHint,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            voiceUiState.message?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = message,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
            }
            if (
                voiceUiState.status == VoiceAssistantStatus.Preparing ||
                voiceUiState.status == VoiceAssistantStatus.Listening ||
                voiceUiState.status == VoiceAssistantStatus.Speaking
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Cancelar",
                    onClick = { voiceAssistantViewModel.cancelListeningFlow() },
                )
            }
            if (voiceUiState.showRetryActions) {
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Intentar otra vez",
                    onClick = voiceAssistantViewModel::retryListening,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Usar botones",
                    onClick = voiceAssistantViewModel::useButtonsInstead,
                )
            }
        }

        uiState.reminderPromptText?.let { reminderText ->
            AppCard {
                Text(
                    text = "Recordatorio pendiente",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = reminderText,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
                if (uiState.nextGroupImageUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MedicationImageGroupPreview(
                        imageUris = uiState.nextGroupImageUris,
                        labels = uiState.nextGroupImageLabels,
                    )
                }
            }
        }

        AppCard {
            Text(
                text = "Próxima toma",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            val nextGroupSummary = uiState.nextGroupSummary
            if (nextGroupSummary == null) {
                Text(
                    text = "No hay medicamentos pendientes por ahora.",
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
            } else {
                Text(
                    text = nextGroupSummary,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                uiState.nextGroupNames?.let { names ->
                    Text(
                        text = names,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    )
                }
                if (uiState.nextGroupImageUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    MedicationImageGroupPreview(
                        imageUris = uiState.nextGroupImageUris,
                        labels = uiState.nextGroupImageLabels,
                    )
                }
            }
        }

        AppCard {
            if (!easyModeEnabled) {
                Text(
                    text = "Última presión de hoy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.latestPressureText,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Pastillas pendientes: ${uiState.medicationsPendingText}",
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
                Text(
                    text = "Adherencia de hoy: ${uiState.adherencePercentageText}",
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
                uiState.pressureHelperMessage?.let { helperMessage ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = helperMessage,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        if (uiState.reminderPromptText != null) {
            AppButton(
                label = "Cerrar recordatorio",
                onClick = viewModel::dismissReminderPrompt,
            )
        }
    }

    if (showPressureDialog) {
        AlertDialog(
            onDismissRequest = { showPressureDialog = false },
            title = {
                Text(
                    text = "Registrar presión",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PressureField("Sistólica", systolic) { systolic = it }
                    PressureField("Diastólica", diastolic) { diastolic = it }
                    PressureField("Pulso opcional", pulse) { pulse = it }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Nota opcional") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        minLines = 3,
                    )
                }
            },
            confirmButton = {
                AppButton(
                    label = "Guardar",
                    onClick = {
                        viewModel.registerPressure(
                            systolicText = systolic,
                            diastolicText = diastolic,
                            pulseText = pulse,
                            notes = notes,
                        ) { _ ->
                            systolic = ""
                            diastolic = ""
                            pulse = ""
                            notes = ""
                            showPressureDialog = false
                        }
                    },
                )
            },
            dismissButton = {
                AppButton(
                    label = "Cancelar",
                    onClick = { showPressureDialog = false },
                )
            },
        )
    }

    voiceUiState.confirmation?.let { confirmation ->
        VoiceConfirmationDialog(
            confirmation = confirmation,
            onConfirm = voiceAssistantViewModel::confirmPendingAction,
            onCancel = voiceAssistantViewModel::cancelPendingAction,
        )
    }

    if (showMedicationConfirmation) {
        val isMultiple = uiState.nextGroupMedicationNames.size > 1
        AlertDialog(
            onDismissRequest = { showMedicationConfirmation = false },
            title = {
                Text(
                    text = if (isMultiple) "¿Ya tomaste estas pastillas?" else "¿Ya tomaste tu pastilla?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.nextGroupMedicationNames.forEach { name ->
                        Text(
                            text = name,
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                        )
                    }
                    if (uiState.nextGroupImageUris.isNotEmpty()) {
                        MedicationImageGroupPreview(
                            imageUris = uiState.nextGroupImageUris,
                            labels = uiState.nextGroupImageLabels,
                        )
                    }
                }
            },
            confirmButton = {
                AppButton(
                    label = "Sí, ya la tomé",
                    onClick = {
                        showMedicationConfirmation = false
                        viewModel.markNextMedicationGroupTaken()
                    },
                )
            },
            dismissButton = {
                AppButton(
                    label = "No",
                    onClick = { showMedicationConfirmation = false },
                )
            },
        )
    }
}

@Composable
private fun PressureField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit)) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun VoiceConfirmationDialog(
    confirmation: VoiceConfirmation,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val (title, body, confirmLabel) = when (confirmation) {
        is VoiceConfirmation.Pressure -> Triple(
            "Confirmar presión",
            buildString {
                append("Te escuché: presión ${confirmation.systolic} sobre ${confirmation.diastolic}")
                confirmation.pulse?.let { pulse -> append(" con pulso $pulse") }
                append(". ¿Deseas guardarla?")
            },
            "Guardar",
        )
        is VoiceConfirmation.Medication -> {
            if (confirmation.medication != null) {
                Triple(
                    "Confirmar pastilla",
                    "¿Confirmas que ya tomaste ${confirmation.medication.name}?",
                    "Sí, registrar",
                )
            } else {
                Triple(
                    "Confirmar pastillas",
                    "¿Confirmas que ya tomaste tus pastillas de las ${formatScheduleTime(confirmation.scheduleTime)}?",
                    "Sí, registrar",
                )
            }
        }
        is VoiceConfirmation.Help -> Triple(
            "Pedir ayuda",
            "¿Deseas llamar a ${confirmation.contact.fullName}?",
            "Abrir llamada",
        )
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = body,
                fontSize = 20.sp,
                lineHeight = 26.sp,
            )
        },
        confirmButton = {
            AppButton(
                label = confirmLabel,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            AppButton(
                label = "Cancelar",
                onClick = onCancel,
            )
        },
    )
}
