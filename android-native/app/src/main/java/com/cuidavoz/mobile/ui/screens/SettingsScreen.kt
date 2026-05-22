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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.cuidavoz.mobile.ui.components.BackupSummaryDialog
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.components.hasMicrophonePermission
import com.cuidavoz.mobile.ui.viewmodel.BackupUiState
import com.cuidavoz.mobile.ui.viewmodel.BackupViewModel
import com.cuidavoz.mobile.ui.viewmodel.ContactField
import com.cuidavoz.mobile.ui.viewmodel.SettingsField
import com.cuidavoz.mobile.ui.viewmodel.SettingsViewModel
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantViewModel
import com.cuidavoz.mobile.data.backup.ImportStrategy

@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel,
    backupViewModel: BackupViewModel,
    voiceAssistantViewModel: VoiceAssistantViewModel,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenReports: () -> Unit,
    showSpeakScreenButton: Boolean,
    onSpeakScreen: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupUiState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val voiceUiState by voiceAssistantViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showReplaceConfirmation by remember { mutableStateOf(false) }
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
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        backupViewModel.exportBackup(uri)
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        backupViewModel.readBackupSummary(uri)
    }

    LaunchedEffect(backupUiState) {
        if (backupUiState !is BackupUiState.ImportPreview) {
            showReplaceConfirmation = false
        }
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
                text = "Ayuda para usar CuidaVoz",
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
                text = "CuidaVoz puede leer las instrucciones en voz alta.",
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
                text = "Contacto familiar",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            SettingsTextField(
                label = "Nombre del familiar",
                value = uiState.familyName,
            ) { viewModel.updateContactField(ContactField.NAME, it) }
            SettingsTextField(
                label = "Teléfono",
                value = uiState.familyPhone,
                keyboardType = KeyboardType.Phone,
            ) { viewModel.updateContactField(ContactField.PHONE, it) }
            SettingsTextField(
                label = "Relación",
                value = uiState.familyRelationship,
            ) { viewModel.updateContactField(ContactField.RELATIONSHIP, it) }
            AppButton(
                label = "Guardar contacto",
                onClick = viewModel::saveContact,
            )
        }

        AppCard {
            Text(
                text = "Recordatorios de pastillas",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "CuidaVoz puede sonar y vibrar cuando sea hora de tomar tus pastillas.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CuidaVoz necesita permiso para avisarte cuando sea hora de tomar tus pastillas.",
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
                text = "Si María no confirma la toma, CuidaVoz volverá a avisar y puede notificar al cuidador.",
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
                text = "CuidaVoz puede hablar cuando sea hora de tomar la pastilla, aunque la app no esté abierta.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Si el celular retrasa los avisos, permite que CuidaVoz funcione en segundo plano y desactiva el ahorro de batería para esta app.",
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
                text = "CuidaVoz puede escucharte y hablar para ayudarte a registrar tu presión y tus pastillas.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "El asistente de voz está activo siempre.",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Solo necesitas tocar Hablar cuando quieras usarlo.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Leer recordatorios en voz alta",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
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
                    text = "CuidaVoz necesita usar el micrófono para escuchar tus indicaciones.",
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
                text = "Copia de seguridad",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Guarda una copia de tus datos para no perderlos si cambias de celular.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Este archivo tiene datos de salud. Guárdalo con cuidado.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Guardar copia",
                onClick = { createBackupLauncher.launch(backupViewModel.suggestedFileName()) },
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Importa respaldos solo si confías en su origen.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Recuperar copia",
                onClick = {
                    importBackupLauncher.launch(
                        arrayOf("application/zip", "application/octet-stream"),
                    )
                },
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
                text = "En algunos celulares, el ahorro de batería puede retrasar recordatorios. Si notas retrasos, permite que CuidaVoz funcione en segundo plano.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
    }

    when (val state = backupUiState) {
        BackupUiState.Idle -> Unit
        BackupUiState.Exporting -> {
            BackupProgressDialog(text = "Creando respaldo...")
        }
        is BackupUiState.ExportSuccess -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text("Respaldo creado") },
                text = {
                    Text(
                        "Respaldo creado correctamente.\n\n" +
                            "Medicamentos: ${state.result.exportedMedications}\n" +
                            "Presiones: ${state.result.exportedPressureReadings}\n" +
                            "Registros de pastillas: ${state.result.exportedMedicationLogs}\n" +
                            "Imagenes: ${state.result.exportedImages}" +
                            if (state.result.warnings.isNotEmpty()) {
                                "\n\nAvisos:\n${state.result.warnings.joinToString("\n")}"
                            } else {
                                ""
                            },
                    )
                },
                confirmButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text("Aceptar")
                    }
                },
            )
        }
        BackupUiState.ImportReading -> {
            BackupProgressDialog(text = "Leyendo respaldo...")
        }
        is BackupUiState.ImportPreview -> {
            BackupSummaryDialog(
                summary = state.summary,
                onCancel = backupViewModel::dismissState,
                onMerge = { backupViewModel.importBackup(ImportStrategy.MERGE) },
                onReplace = { showReplaceConfirmation = true },
            )
        }
        is BackupUiState.Importing -> {
            BackupProgressDialog(
                text = if (state.strategy == ImportStrategy.REPLACE_ALL) {
                    "Restaurando datos..."
                } else {
                    "Importando datos..."
                },
            )
        }
        is BackupUiState.ImportSuccess -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text("Importación completada") },
                text = {
                    Text(
                        "Se importaron ${state.result.importedMedications} medicamentos, " +
                            "${state.result.importedPressureReadings} presiones, " +
                            "${state.result.importedMedicationLogs} registros de pastillas " +
                            "y ${state.result.importedImages} imágenes.\n\n" +
                            if (state.result.skippedDuplicates > 0) {
                                "Duplicados omitidos: ${state.result.skippedDuplicates}\n\n"
                            } else {
                                ""
                            } +
                            if (state.result.errors.isNotEmpty()) {
                                state.result.errors.joinToString("\n")
                            } else {
                                "Datos restaurados correctamente."
                            },
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupViewModel.dismissState()
                            onOpenHome()
                        },
                    ) {
                        Text("Ver mis datos")
                    }
                },
                dismissButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text("Cerrar")
                    }
                },
            )
        }
        is BackupUiState.Error -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text("No pudimos continuar") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text("Aceptar")
                    }
                },
            )
        }
    }

    if (showReplaceConfirmation && backupUiState is BackupUiState.ImportPreview) {
            AlertDialog(
                onDismissRequest = { showReplaceConfirmation = false },
                title = { Text("Confirmar reemplazo") },
                text = {
                    Text(
                        "Esto cambiará tus datos. Borrará lo actual de este celular y pondrá la copia guardada.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                        showReplaceConfirmation = false
                        backupViewModel.importBackup(ImportStrategy.REPLACE_ALL)
                    },
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmation = false }) {
                    Text("Cancelar")
                }
            },
        )
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

@Composable
private fun BackupProgressDialog(text: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun SettingsTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
