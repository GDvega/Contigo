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
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ConfirmMedicationDialog
import com.cuidavoz.mobile.ui.components.MedicationImagePreview
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.components.VoiceAssistantSection
import com.cuidavoz.mobile.ui.components.VoiceConfirmationDialog
import com.cuidavoz.mobile.ui.viewmodel.HomeScreenState
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantUiState
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel
import com.cuidavoz.mobile.util.formatScheduleTime

private val PatientBackground = Color(0xFFFBF7EC)
private val StatusBackground = Color(0xFFE9DDF8)
private val StatusText = Color(0xFF0B1F3A)
private val VoiceButtonBackground = Color(0xFFE9DDF8)

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
    onConfirmMedicationTaken: () -> Unit,
    onRemindLater: () -> Unit,
    onDismissMessage: () -> Unit,
) {
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
            .background(PatientBackground)
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
                color = StatusText,
            )

            StatusPill(text = uiState.generalStatusText)

            MainMedicationCard(
                uiState = uiState,
                isMultiple = isMultiple,
            )

            if (nextMedications.isNotEmpty()) {
                AppButton(
                    label = if (isMultiple) "Ya tomé todas" else "Ya tomé",
                    onClick = { showMedicationConfirmation = true },
                    contentDescription = "Botón ya tomé",
                    minHeight = 72.dp,
                    textSize = 27.sp,
                )
            }

            PatientActionButton(
                label = "Medir presión",
                icon = Icons.Outlined.Favorite,
                backgroundColor = Color(0xFFE3F5F2),
                onClick = onOpenMeasurePressure,
                contentDescription = "Botón medir presión",
            )
            PatientActionButton(
                label = "Pedir ayuda",
                icon = Icons.Outlined.Call,
                backgroundColor = Color(0xFFFDE8EA),
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
                onMedicationTaken = if (nextMedications.isNotEmpty()) {
                    { showMedicationConfirmation = true }
                } else {
                    null
                },
                onMeasurePressure = onOpenMeasurePressure,
                onAskHelp = onOpenHelp,
                onCancel = { voiceAssistantViewModel.cancelListeningFlow() },
            )

            SmallSecondaryButton(
                label = "Escuchar",
                icon = Icons.Outlined.Hearing,
                onClick = onSpeakHome,
                contentDescription = "Botón escuchar instrucciones",
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

    if (showMedicationConfirmation) {
        ConfirmMedicationDialog(
            medications = nextMedications,
            scheduleTime = uiState.nextGroupScheduleTime,
            onConfirm = {
                showMedicationConfirmation = false
                onConfirmMedicationTaken()
            },
            onDismiss = { showMedicationConfirmation = false },
            onRequestHelp = onOpenHelp,
        )
    }

    voiceUiState.confirmation?.let { confirmation ->
        VoiceConfirmationDialog(
            confirmation = confirmation,
            onConfirm = voiceAssistantViewModel::confirmPendingAction,
            onCancel = voiceAssistantViewModel::cancelPendingAction,
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
private fun Header(onSpeakHome: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "CuidaVoz",
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F7C78),
        )
        FilledTonalButton(
            onClick = onSpeakHome,
            modifier = Modifier.height(56.dp),
            colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                containerColor = VoiceButtonBackground,
                contentColor = StatusText,
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
    val nextMedications = uiState.nextGroupMedications
    AppCard {
        Text(
            text = if (isMultiple) "Toma estas pastillas" else "Próxima pastilla",
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = StatusText,
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (nextMedications.isEmpty()) {
            Text(
                text = "No hay pastillas pendientes hoy.",
                fontSize = 20.sp,
                lineHeight = 26.sp,
            )
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
                    color = Color(0xFF0F7C78),
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
                        color = StatusText,
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
                            color = Color(0xFF0F7C78),
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
private fun StatusPill(text: String) {
    Text(
        text = text,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        color = StatusText,
        modifier = Modifier
            .background(StatusBackground, MaterialTheme.shapes.large)
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
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = StatusText,
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
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            containerColor = VoiceButtonBackground,
            contentColor = StatusText,
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
