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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.cuidavoz.mobile.R
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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.caregiver_btn_back))
                Text(stringResource(R.string.caregiver_btn_back))
            }
            FilledTonalButton(onClick = onSpeak) {
                Icon(Icons.Outlined.Hearing, contentDescription = stringResource(R.string.home_btn_listen))
                Text(stringResource(R.string.home_btn_listen))
            }
        }

        Text(
            text = stringResource(R.string.pressure_title),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.pressure_intro),
            fontSize = 20.sp,
            lineHeight = 26.sp,
        )

        PressureInputCard(
            title = stringResource(R.string.pressure_label_systolic),
            subtitle = stringResource(R.string.pressure_label_systolic_sub),
            value = systolic,
            onValueChange = { systolic = it.filter(Char::isDigit) },
            icon = Icons.Outlined.Favorite,
            iconTint = extraColors.errorRed,
            unit = stringResource(R.string.pressure_unit_mmHg),
        )
        PressureInputCard(
            title = stringResource(R.string.pressure_label_diastolic),
            subtitle = stringResource(R.string.pressure_label_diastolic_sub),
            value = diastolic,
            onValueChange = { diastolic = it.filter(Char::isDigit) },
            icon = Icons.Outlined.Speed,
            iconTint = extraColors.infoBlue,
            unit = stringResource(R.string.pressure_unit_mmHg),
        )
        PressureInputCard(
            title = stringResource(R.string.pressure_label_pulse),
            subtitle = stringResource(R.string.pressure_label_pulse_sub),
            value = pulse,
            onValueChange = { pulse = it.filter(Char::isDigit) },
            icon = Icons.Outlined.MonitorHeart,
            iconTint = extraColors.reportIcon,
            unit = stringResource(R.string.pressure_unit_pulse),
        )

        val statusGood = stringResource(R.string.pressure_status_good)
        val statusCheck = stringResource(R.string.pressure_status_check)

        AppButton(
            label = stringResource(R.string.pressure_btn_save),
            onClick = {
                viewModel.registerPressure(
                    systolicText = systolic,
                    diastolicText = diastolic,
                    pulseText = pulse,
                    notes = "",
                ) { result ->
                    onSaved(result.toSavedScreenData(statusGood, statusCheck))
                    systolic = ""
                    diastolic = ""
                    pulse = ""
                }
            },
            contentDescription = "Botón Guardar",
        )

        AppCard {
            Text(
                text = stringResource(R.string.pressure_voice_examples_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pressure_voice_example_1),
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
                text = stringResource(voiceUiState.assistantTitleResId),
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = voiceUiState.recognizedText ?: stringResource(voiceUiState.assistantHintResId),
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
                    label = stringResource(R.string.btn_cancel),
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
    val numericValue = value.toIntOrNull() ?: 0

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
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
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedIconButton(
                onClick = { if (numericValue > 0) onValueChange((numericValue - 1).toString()) },
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = "Restar 1", modifier = Modifier.size(32.dp))
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.headlineLarge.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                label = { Text(unit, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
            )

            OutlinedIconButton(
                onClick = { onValueChange((numericValue + 1).toString()) },
                modifier = Modifier.size(64.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Sumar 1", modifier = Modifier.size(32.dp))
            }
        }
    }
}

private fun SavedPressureResult.toSavedScreenData(
    statusGood: String,
    statusCheck: String,
): PressureSavedData {
    val statusText = when (status) {
        "NORMAL", "ELEVATED" -> statusGood
        else -> statusCheck
    }
    return PressureSavedData(
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        measuredAt = measuredAt,
        statusText = statusText,
    )
}
