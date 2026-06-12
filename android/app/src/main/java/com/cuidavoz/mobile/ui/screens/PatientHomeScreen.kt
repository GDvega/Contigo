package com.cuidavoz.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ConfirmMedicationDialog
import com.cuidavoz.mobile.ui.components.MedicationImagePreview
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.components.VoiceAssistantSection
import com.cuidavoz.mobile.ui.theme.ContigoTheme
import com.cuidavoz.mobile.ui.viewmodel.HomeScreenState
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantUiState
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel
import com.cuidavoz.mobile.util.formatScheduleTime

// Removal of private constants that are now in the theme
@Composable
fun PatientHomeScreen(
    innerPadding: PaddingValues,
    uiState: HomeScreenState,
    voiceUiState: VoiceAssistantUiState,
    easyModeEnabled: Boolean,
    voiceAssistantViewModel: VoiceAssistantViewModel,
    onOpenMeasurePressure: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenCaregiver: () -> Unit,
    onSpeakHome: () -> Unit,
    onRecordMedicationOutcomes: (List<MedicationDoseOutcome>) -> Unit,
    onRemindLater: () -> Unit,
    onDismissMessage: () -> Unit,
) {
    val extraColors = ContigoTheme.extraColors
    var showMedicationConfirmation by rememberSaveable { mutableStateOf(false) }
    var showCaregiverConfirmation by rememberSaveable { mutableStateOf(false) }
    val nextMedications = uiState.nextGroupMedications
    val isMultiple = nextMedications.size > 1
    val voiceTranscript = voiceUiState.recognizedText
        ?.substringAfter(": ", voiceUiState.recognizedText.orEmpty())
        ?.takeIf { it.isNotBlank() }

    ToastMessageEffect(
        message = uiState.actionMessage,
        onConsumed = onDismissMessage,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(extraColors.patientBackground)
            .padding(innerPadding),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header(onSpeakHome = onSpeakHome)

            Text(
                text = uiState.greeting,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Bold,
                color = extraColors.statusText,
            )

            StatusPill(text = uiState.generalStatusText)

            PressureStatusCard(uiState = uiState)

            uiState.reminderPromptText?.let { promptText ->
                ReminderPromptCard(
                    promptText = promptText,
                    hasPendingMedications = nextMedications.isNotEmpty(),
                    isMultiple = isMultiple,
                    onConfirmMedicationTaken = {
                        if (nextMedications.isNotEmpty()) {
                            showMedicationConfirmation = true
                        }
                    },
                    onRemindLater = onRemindLater,
                )
            }

            MainMedicationCard(
                uiState = uiState,
                isMultiple = isMultiple,
            )

            if (nextMedications.isNotEmpty() && uiState.reminderPrompt == null) {
                AppButton(
                    label = if (isMultiple) "Registrar tomas" else "Ya tomé",
                    onClick = { showMedicationConfirmation = true },
                    contentDescription = if (isMultiple) {
                        "Botón registrar tomas"
                    } else {
                        "Botón ya tomé"
                    },
                    minHeight = 72.dp,
                    textSize = 27.sp,
                )
            }

            PatientActionButton(
                label = "Medir presión",
                icon = Icons.Outlined.Favorite,
                backgroundColor = extraColors.measurePressureButton,
                onClick = onOpenMeasurePressure,
                contentDescription = "Botón medir presión",
            )
            PatientActionButton(
                label = "Pedir ayuda",
                icon = Icons.Outlined.Call,
                backgroundColor = extraColors.helpButton,
                onClick = onOpenHelp,
                contentDescription = "Botón pedir ayuda",
            )

            VoiceAssistantSection(
                state = voiceUiState,
                transcript = voiceTranscript,
                onButtonPressed = voiceAssistantViewModel::onVoiceButtonPressed,
                onPermissionAlreadyGranted = voiceAssistantViewModel::onMicrophonePermissionAlreadyGranted,
                onPermissionRequestStarted = voiceAssistantViewModel::onMicrophonePermissionRequested,
                onPermissionResult = voiceAssistantViewModel::onMicrophonePermissionResult,
                onRetry = voiceAssistantViewModel::retryListening,
                onUseButtons = voiceAssistantViewModel::useButtonsInstead,
                onCancel = { voiceAssistantViewModel.cancelListeningFlow() },
            )

            SmallSecondaryButton(
                label = "Familiar / Ajustes",
                icon = Icons.Outlined.Settings,
                onClick = { showCaregiverConfirmation = true },
                contentDescription = "Botón familiar y ajustes",
            )

            Spacer(modifier = Modifier.height(if (easyModeEnabled) 40.dp else 32.dp))
        }
    }

    if (showMedicationConfirmation && nextMedications.isNotEmpty()) {
        ConfirmMedicationDialog(
            medications = nextMedications,
            scheduleTime = uiState.nextGroupScheduleTime,
            onSave = { outcomes ->
                showMedicationConfirmation = false
                onRecordMedicationOutcomes(outcomes)
            },
            onDismiss = { showMedicationConfirmation = false },
            onRequestHelp = {
                showMedicationConfirmation = false
                onOpenHelp()
            },
        )
    }

    if (showCaregiverConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCaregiverConfirmation = false },
            title = {
                Text(
                    text = "Esta zona es para el familiar o cuidador.",
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Aquí se cambian pastillas, reportes, registros y ajustes.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
            },
            confirmButton = {
                AppButton(
                    label = "Entrar",
                    onClick = {
                        showCaregiverConfirmation = false
                        onOpenCaregiver()
                    },
                    contentDescription = "Botón entrar",
                )
            },
            dismissButton = {
                AppButton(
                    label = "Cancelar",
                    onClick = { showCaregiverConfirmation = false },
                    contentDescription = "Botón cancelar",
                )
            },
        )
    }
}

@Composable
private fun ReminderPromptCard(
    promptText: String,
    hasPendingMedications: Boolean,
    isMultiple: Boolean,
    onConfirmMedicationTaken: () -> Unit,
    onRemindLater: () -> Unit,
) {
    val extraColors = ContigoTheme.extraColors
    AppCard {
        Text(
            text = "Recordatorio de medicamento",
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = extraColors.statusText,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = promptText,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (hasPendingMedications) {
            AppButton(
                label = if (isMultiple) "Registrar tomas" else "Ya tomé",
                onClick = onConfirmMedicationTaken,
                contentDescription = if (isMultiple) {
                    "Botón registrar tomas desde recordatorio"
                } else {
                    "Botón confirmar toma desde recordatorio"
                },
                minHeight = 64.dp,
                textSize = 24.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        AppButton(
            label = "Posponerlo",
            onClick = onRemindLater,
            contentDescription = "Botón posponer recordatorio",
            minHeight = 64.dp,
            textSize = 24.sp,
        )
    }
}

@Composable
private fun Header(onSpeakHome: () -> Unit) {
    val extraColors = ContigoTheme.extraColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Contigo",
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = extraColors.brandText,
        )
        FilledTonalButton(
            onClick = onSpeakHome,
            modifier = Modifier.height(56.dp),
            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                containerColor = extraColors.voiceButtonBackground,
                contentColor = extraColors.statusText,
            ),
        ) {
            Icon(Icons.Outlined.Hearing, contentDescription = "Escuchar")
            Text(
                text = "Escuchar",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MainMedicationCard(
    uiState: HomeScreenState,
    isMultiple: Boolean,
) {
    val extraColors = ContigoTheme.extraColors
    val nextMedications = uiState.nextGroupMedications
    AppCard {
        Text(
            text = when {
                nextMedications.isEmpty() -> "Medicamentos de hoy"
                isMultiple -> "Toma estas pastillas"
                else -> "Próxima pastilla"
            },
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = extraColors.statusText,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (nextMedications.isEmpty()) {
            Text(
                text = if ((uiState.dailyStatus?.activeMedicationCount ?: 0) == 0) {
                    "No hay medicamentos programados para hoy."
                } else {
                    "No hay pastillas pendientes hoy."
                },
                fontSize = 20.sp,
                lineHeight = 26.sp,
            )
            if ((uiState.dailyStatus?.activeMedicationCount ?: 0) == 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Para agregar medicamentos, entra a Familiar / Ajustes.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@AppCard
        }

        if (isMultiple) {
            Text(
                text = "${nextMedications.size} pastillas",
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Bold,
            )
            uiState.nextGroupScheduleTime?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatScheduleTime(it),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    color = extraColors.brandText,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            nextMedications.forEach { medication ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MedicationImagePreview(
                        imageUri = medication.imageUri,
                        label = medication.name,
                        size = 72.dp,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = medication.name,
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = medication.dose,
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        } else {
            val medication = nextMedications.first()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MedicationImagePreview(
                    imageUri = medication.imageUri,
                    label = medication.name,
                    size = 132.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = medication.name,
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = extraColors.statusText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = medication.dose,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                    )
                    uiState.nextGroupScheduleTime?.let {
                        Text(
                            text = formatScheduleTime(it),
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            color = extraColors.brandText,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    medication.instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
                        Text(
                            text = instructions,
                            fontSize = 20.sp,
                            lineHeight = 24.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PressureStatusCard(uiState: HomeScreenState) {
    val extraColors = ContigoTheme.extraColors
    AppCard {
        Text(
            text = "Presión de hoy",
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = extraColors.statusText,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiState.latestPressureText,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = uiState.pressureHelperMessage ?: "Puedes medir tu presión cuando lo necesites.",
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusPill(text: String) {
    val extraColors = ContigoTheme.extraColors
    Text(
        text = text,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        color = extraColors.statusText,
        modifier = Modifier
            .background(extraColors.statusBackground, MaterialTheme.shapes.large)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun PatientActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val extraColors = ContigoTheme.extraColors
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = extraColors.statusText,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
        Text(
            text = label,
            fontSize = 24.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun SmallSecondaryButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
) {
    val extraColors = ContigoTheme.extraColors
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = extraColors.voiceButtonBackground,
            contentColor = extraColors.statusText,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
