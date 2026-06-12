package com.cuidavoz.mobile.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.components.hasMicrophonePermission
import com.cuidavoz.mobile.ui.viewmodel.SettingsField
import com.cuidavoz.mobile.ui.viewmodel.SettingsViewModel
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel,
    voiceAssistantViewModel: VoiceAssistantViewModel,
    onBack: () -> Unit,
    onOpenReports: () -> Unit,
    showSpeakScreenButton: Boolean,
    onSpeakScreen: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val voiceUiState by voiceAssistantViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermissionGranted = remember {
        mutableStateOf(context.hasNotificationPermission())
    }
    val microphonePermissionGranted = remember {
        mutableStateOf(context.hasMicrophonePermission())
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationPermissionGranted.value = granted
    }
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        microphonePermissionGranted.value = granted
        voiceAssistantViewModel.onMicrophonePermissionResult(granted)
    }

    ToastMessageEffect(
        message = uiState.message,
        onConsumed = viewModel::dismissMessage,
    )
    ToastMessageEffect(
        message = voiceUiState.message,
        onConsumed = voiceAssistantViewModel::dismissMessage,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalButton(onClick = onBack, modifier = Modifier.height(56.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                Text("Volver")
            }
            if (showSpeakScreenButton) {
                FilledTonalButton(onClick = onSpeakScreen, modifier = Modifier.height(56.dp)) {
                    Text("Escuchar")
                }
            }
        }
        Text(
            text = "Ajustes",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        AppCard {
            Text(
                text = "Ayuda para usar Contigo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Modo fácil",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Usa botones más grandes, menos texto y más ayuda por voz.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.easyModeEnabled,
                onCheckedChange = viewModel::setEasyModeEnabled,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Guía por voz",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Contigo puede leer las instrucciones en voz alta.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.voiceGuidanceEnabled,
                onCheckedChange = viewModel::setVoiceGuidanceEnabled,
            )
        }

        AppCard {
            Text(
                text = "Recordatorios de pastillas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Contigo puede sonar y vibrar cuando sea hora de tomar tus pastillas.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Contigo necesita permiso para avisarte cuando sea hora de tomar tus pastillas.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Activar recordatorios",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Switch(
                checked = uiState.remindersEnabled,
                onCheckedChange = viewModel::toggleReminders,
            )
            Text(
                text = "Repetición: cada ${uiState.repeatIntervalMinutes} minutos",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Text(
                text = "Máximo de avisos: ${uiState.maxRepeatCount}",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sonido", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = uiState.soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled,
            )
            Text("Vibración", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = uiState.vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled,
            )
            Text("Avisar al cuidador si no responde", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = "Si el paciente no confirma la toma, Contigo volverá a avisar y puede notificar al cuidador.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.notifyCaregiverOnMissed,
                onCheckedChange = viewModel::setNotifyCaregiverOnMissed,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Repetir aviso cada", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(5, 10, 15).forEach { minutes ->
                    AppButton(
                        label = "$minutes min",
                        onClick = { viewModel.setRepeatIntervalMinutes(minutes) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.repeatIntervalMinutes != minutes,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Máximo de avisos", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(2, 3, 5).forEach { count ->
                    AppButton(
                        label = count.toString(),
                        onClick = { viewModel.setMaxRepeatCount(count) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.maxRepeatCount != count,
                    )
                }
            }
            if (!notificationPermissionGranted.value) {
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Permitir recordatorios",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            if (!uiState.canScheduleExactAlarms) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Falta permitir alarmas exactas.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Text(
                    text = "Para avisos más puntuales, permite alarmas exactas en los ajustes del sistema.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppButton(
                    label = "Permitir alarmas exactas",
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            runCatching { context.startActivity(intent) }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "En algunos celulares, los avisos pueden retrasarse si el ahorro de batería está activo.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Reprogramar recordatorios",
                onClick = viewModel::reprogramReminders,
            )
        }

        AppCard {
            Text(
                text = "Importante en algunos celulares",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Contigo puede hablar cuando sea hora de tomar la pastilla, aunque la app no esté abierta.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Si el celular retrasa los avisos, permite que Contigo funcione en segundo plano y desactiva el ahorro de batería para esta app.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Abrir ajustes del celular",
                onClick = {
                    val batteryIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    runCatching { context.startActivity(batteryIntent) }
                },
            )
        }

        AppCard {
            Text(
                text = "Rangos indicados por el médico",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Usa los rangos que te indicó tu médico.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
        }

        AppCard {
            SettingsNumberField(
                label = "Sistólica mínima normal",
                value = uiState.systolicMinNormal,
            ) { viewModel.updateField(SettingsField.SYSTOLIC_MIN, it) }
            SettingsNumberField(
                label = "Sistólica máxima normal",
                value = uiState.systolicMaxNormal,
            ) { viewModel.updateField(SettingsField.SYSTOLIC_MAX, it) }
            SettingsNumberField(
                label = "Diastólica mínima normal",
                value = uiState.diastolicMinNormal,
            ) { viewModel.updateField(SettingsField.DIASTOLIC_MIN, it) }
            SettingsNumberField(
                label = "Diastólica máxima normal",
                value = uiState.diastolicMaxNormal,
            ) { viewModel.updateField(SettingsField.DIASTOLIC_MAX, it) }
            SettingsNumberField(
                label = "Pulso mínimo normal",
                value = uiState.pulseMinNormal,
            ) { viewModel.updateField(SettingsField.PULSE_MIN, it) }
            SettingsNumberField(
                label = "Pulso máximo normal",
                value = uiState.pulseMaxNormal,
            ) { viewModel.updateField(SettingsField.PULSE_MAX, it) }
            OutlinedTextField(
                value = uiState.doctorRecommendation,
                onValueChange = { viewModel.updateField(SettingsField.RECOMMENDATION, it) },
                label = { Text("Recomendación del médico") },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 3,
            )
        }

        AppButton(
            label = "Guardar rangos",
            onClick = viewModel::saveSettings,
        )

        AppCard {
            Text(
                text = "Asistente de voz",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Contigo puede escucharte y hablar para ayudarte a registrar tu presión y tus pastillas.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Activar asistente de voz",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Contigo puede escucharte para ayudarte a registrar datos.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.voiceAssistantEnabled,
                onCheckedChange = viewModel::setVoiceAssistantEnabled,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Leer recordatorios en voz alta",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Cuando suene la hora de la pastilla, Contigo dirá el nombre y la dosis en español. Recomendado si la persona no lee bien.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.voiceReminderEnabled,
                onCheckedChange = viewModel::setVoiceReminderEnabled,
            )
            Text(
                text = "Repetir voz del recordatorio",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VoiceRepeatOption(
                    label = "1 vez",
                    selected = uiState.voiceRepeatCount == 1,
                    onClick = { viewModel.setVoiceRepeatCount(1) },
                    modifier = Modifier.weight(1f),
                )
                VoiceRepeatOption(
                    label = "2 veces",
                    selected = uiState.voiceRepeatCount == 2,
                    onClick = { viewModel.setVoiceRepeatCount(2) },
                    modifier = Modifier.weight(1f),
                )
                VoiceRepeatOption(
                    label = "3 veces",
                    selected = uiState.voiceRepeatCount == 3,
                    onClick = { viewModel.setVoiceRepeatCount(3) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (!microphonePermissionGranted.value) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Contigo necesita usar el micrófono para escuchar tus indicaciones.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Permitir micrófono",
                    onClick = { microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Probar voz",
                onClick = voiceAssistantViewModel::testVoice,
            )
        }

        AppCard {
            Text(
                text = "Reporte para médico",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Prepara un reporte para tu médico.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Crear reporte",
                onClick = onOpenReports,
            )
        }

        AppCard {
            Text(
                text = "Importante en algunos celulares",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "En algunos celulares, el ahorro de batería puede retrasar recordatorios. Si notas retrasos, permite que Contigo funcione en segundo plano.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

}

@Composable
private fun SettingsNumberField(
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
private fun VoiceRepeatOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private fun Context.hasNotificationPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}
