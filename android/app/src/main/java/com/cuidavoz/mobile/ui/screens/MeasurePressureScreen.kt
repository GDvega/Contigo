package com.cuidavoz.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.components.VoiceAssistantButton
import com.cuidavoz.mobile.ui.theme.ContigoTheme
import com.cuidavoz.mobile.ui.viewmodel.HomeViewModel
import com.cuidavoz.mobile.ui.viewmodel.SavedPressureResult
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantStatus
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel

@Composable
fun MeasurePressureScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    voiceAssistantViewModel: VoiceAssistantViewModel,
    onBack: () -> Unit,
    onSpeak: () -> Unit,
    onSaved: (PressureSavedData) -> Unit,
) {
    val extraColors = ContigoTheme.extraColors
    val homeUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceUiState by voiceAssistantViewModel.uiState.collectAsStateWithLifecycle()
    var systolic by rememberSaveable { mutableStateOf("") }
    var diastolic by rememberSaveable { mutableStateOf("") }
    var pulse by rememberSaveable { mutableStateOf("") }

    ToastMessageEffect(
        message = homeUiState.actionMessage,
        onConsumed = viewModel::dismissMessage,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                Text("Volver")
            }
            FilledTonalButton(onClick = onSpeak) {
                Icon(Icons.Outlined.Hearing, contentDescription = "Escuchar")
                Text("Escuchar")
            }
        }

        Text(
            text = "Medir presión",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Mide tu presión cuando puedas.",
            fontSize = 20.sp,
            lineHeight = 26.sp,
        )

        PressureInputCard(
            title = "Alta",
            subtitle = "(sistólica)",
            value = systolic,
            onValueChange = { systolic = it.filter(Char::isDigit) },
            icon = Icons.Outlined.Favorite,
            iconTint = extraColors.errorRed,
            unit = "mmHg",
        )
        PressureInputCard(
            title = "Baja",
            subtitle = "(diastólica)",
            value = diastolic,
            onValueChange = { diastolic = it.filter(Char::isDigit) },
            icon = Icons.Outlined.Speed,
            iconTint = extraColors.infoBlue,
            unit = "mmHg",
        )
        PressureInputCard(
            title = "Pulso",
            subtitle = "(opcional)",
            value = pulse,
            onValueChange = { pulse = it.filter(Char::isDigit) },
            icon = Icons.Outlined.MonitorHeart,
            iconTint = extraColors.reportIcon,
            unit = "por min",
        )

        AppButton(
            label = "Guardar",
            onClick = {
                viewModel.registerPressure(
                    systolicText = systolic,
                    diastolicText = diastolic,
                    pulseText = pulse,
                    notes = "",
                ) { result ->
                    onSaved(result.toSavedScreenData())
                    systolic = ""
                    diastolic = ""
                    pulse = ""
                }
            },
            contentDescription = "Botón Guardar",
        )

        AppCard {
            Text(
                text = "Puedes decir:",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ciento veinte sobre ochenta",
                fontSize = 20.sp,
                lineHeight = 26.sp,
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = voiceUiState.assistantTitle,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = voiceUiState.recognizedText ?: voiceUiState.assistantHint,
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            voiceUiState.message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
            }
            if (
                voiceUiState.status == VoiceAssistantStatus.Preparing ||
                voiceUiState.status == VoiceAssistantStatus.Listening ||
                voiceUiState.status == VoiceAssistantStatus.Speaking ||
                voiceUiState.status == VoiceAssistantStatus.Processing
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Cancelar",
                    onClick = { voiceAssistantViewModel.cancelListeningFlow() },
                    contentDescription = "Botón Cancelar voz",
                )
            }
        }

        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun PressureInputCard(
    title: String,
    subtitle: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    unit: String,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = { Text(unit) },
        )
    }
}

private fun SavedPressureResult.toSavedScreenData(): PressureSavedData {
    val statusText = when (status) {
        "NORMAL", "ELEVATED" -> "Todo bien"
        else -> "Revisar con familiar o médico"
    }
    return PressureSavedData(
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        measuredAt = measuredAt,
        statusText = statusText,
    )
}
