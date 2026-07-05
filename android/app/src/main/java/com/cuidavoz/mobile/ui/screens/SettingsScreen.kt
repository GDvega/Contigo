package com.cuidavoz.mobile.ui.screens

import android.Manifest
import android.os.PowerManager
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
import androidx.compose.ui.res.stringResource
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
import com.cuidavoz.mobile.R

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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.caregiver_btn_back))
                Text(stringResource(R.string.caregiver_btn_back))
            }
            if (showSpeakScreenButton) {
                FilledTonalButton(onClick = onSpeakScreen, modifier = Modifier.height(56.dp)) {
                    Text(stringResource(R.string.home_btn_listen))
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        AppCard {
            Text(
                text = stringResource(R.string.settings_help_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_easy_mode_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_easy_mode_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.easyModeEnabled,
                onCheckedChange = viewModel::setEasyModeEnabled,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_voice_guide_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_voice_guide_desc),
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
                text = stringResource(R.string.settings_reminders_section_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.settings_reminders_intro),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_reminders_perm_help),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_reminders_enable_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Switch(
                checked = uiState.remindersEnabled,
                onCheckedChange = viewModel::toggleReminders,
            )
            Text(
                text = stringResource(R.string.settings_reminders_repeat_label, uiState.repeatIntervalMinutes),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Text(
                text = stringResource(R.string.settings_reminders_max_alerts_label, uiState.maxRepeatCount),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.settings_reminders_sound), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = uiState.soundEnabled,
                onCheckedChange = viewModel::setSoundEnabled,
            )
            Text(stringResource(R.string.settings_reminders_vibration), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Switch(
                checked = uiState.vibrationEnabled,
                onCheckedChange = viewModel::setVibrationEnabled,
            )
            Text(stringResource(R.string.settings_reminders_notify_caregiver), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.settings_reminders_notify_caregiver_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.notifyCaregiverOnMissed,
                onCheckedChange = viewModel::setNotifyCaregiverOnMissed,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.settings_reminders_repeat_every), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(5, 10, 15).forEach { minutes ->
                    AppButton(
                        label = stringResource(R.string.settings_reminders_min_unit, minutes),
                        onClick = { viewModel.setRepeatIntervalMinutes(minutes) },
                        modifier = Modifier.weight(1f),
                        enabled = uiState.repeatIntervalMinutes != minutes,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.settings_reminders_max_alerts_label, 0).substringBefore(":"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
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
                    label = stringResource(R.string.settings_reminders_btn_perm_notif),
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
                    text = stringResource(R.string.settings_reminders_perm_exact_missing),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Text(
                    text = stringResource(R.string.settings_reminders_perm_exact_desc),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppButton(
                    label = stringResource(R.string.settings_reminders_btn_perm_exact),
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
                    text = stringResource(R.string.settings_reminders_battery_warning),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = stringResource(R.string.settings_reminders_btn_reprogram),
                onClick = viewModel::reprogramReminders,
            )
        }

        AppCard {
            Text(
                text = stringResource(R.string.settings_important_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.settings_important_bg_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_important_battery_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            val isIgnoringBatteryOptimizations = remember(context) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
            if (!isIgnoringBatteryOptimizations) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Para que los recordatorios suenen a la hora exacta, permite que Contigo ignore el ahorro de batería.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppButton(
                    label = "Permitir que Contigo funcione sin restricciones de batería",
                    onClick = {
                        val batteryRequestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        runCatching { context.startActivity(batteryRequestIntent) }
                    },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = stringResource(R.string.settings_btn_open_system_settings),
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
                text = stringResource(R.string.settings_medical_ranges_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_medical_ranges_intro),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
        }

        AppCard {
            SettingsNumberField(
                label = stringResource(R.string.settings_label_systolic_min),
                value = uiState.systolicMinNormal,
            ) { viewModel.updateField(SettingsField.SYSTOLIC_MIN, it) }
            SettingsNumberField(
                label = stringResource(R.string.settings_label_systolic_max),
                value = uiState.systolicMaxNormal,
            ) { viewModel.updateField(SettingsField.SYSTOLIC_MAX, it) }
            SettingsNumberField(
                label = stringResource(R.string.settings_label_diastolic_min),
                value = uiState.diastolicMinNormal,
            ) { viewModel.updateField(SettingsField.DIASTOLIC_MIN, it) }
            SettingsNumberField(
                label = stringResource(R.string.settings_label_diastolic_max),
                value = uiState.diastolicMaxNormal,
            ) { viewModel.updateField(SettingsField.DIASTOLIC_MAX, it) }
            SettingsNumberField(
                label = stringResource(R.string.settings_label_pulse_min),
                value = uiState.pulseMinNormal,
            ) { viewModel.updateField(SettingsField.PULSE_MIN, it) }
            SettingsNumberField(
                label = stringResource(R.string.settings_label_pulse_max),
                value = uiState.pulseMaxNormal,
            ) { viewModel.updateField(SettingsField.PULSE_MAX, it) }
            OutlinedTextField(
                value = uiState.doctorRecommendation,
                onValueChange = { viewModel.updateField(SettingsField.RECOMMENDATION, it) },
                label = { Text(stringResource(R.string.settings_label_doctor_rec)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 3,
            )
        }

        AppButton(
            label = stringResource(R.string.settings_btn_save_ranges),
            onClick = viewModel::saveSettings,
        )

        AppCard {
            Text(
                text = stringResource(R.string.settings_voice_assistant_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.settings_voice_assistant_intro),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_voice_assistant_enable_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_voice_assistant_enable_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.voiceAssistantEnabled,
                onCheckedChange = viewModel::setVoiceAssistantEnabled,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.settings_voice_reminder_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.settings_voice_reminder_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Switch(
                checked = uiState.voiceReminderEnabled,
                onCheckedChange = viewModel::setVoiceReminderEnabled,
            )
            Text(
                text = stringResource(R.string.settings_voice_repeat_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VoiceRepeatOption(
                    label = stringResource(R.string.settings_voice_repeat_unit, 1),
                    selected = uiState.voiceRepeatCount == 1,
                    onClick = { viewModel.setVoiceRepeatCount(1) },
                    modifier = Modifier.weight(1f),
                )
                VoiceRepeatOption(
                    label = stringResource(R.string.settings_voice_repeat_unit_plural, 2),
                    selected = uiState.voiceRepeatCount == 2,
                    onClick = { viewModel.setVoiceRepeatCount(2) },
                    modifier = Modifier.weight(1f),
                )
                VoiceRepeatOption(
                    label = stringResource(R.string.settings_voice_repeat_unit_plural, 3),
                    selected = uiState.voiceRepeatCount == 3,
                    onClick = { viewModel.setVoiceRepeatCount(3) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (!microphonePermissionGranted.value) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.settings_voice_mic_perm_desc),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = stringResource(R.string.settings_voice_btn_perm_mic),
                    onClick = { microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = stringResource(R.string.settings_voice_btn_test),
                onClick = voiceAssistantViewModel::testVoice,
            )
        }

        AppCard {
            Text(
                text = stringResource(R.string.reports_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.reports_intro),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = stringResource(R.string.settings_btn_create_report),
                onClick = onOpenReports,
            )
        }

        AppCard {
            Text(
                text = stringResource(R.string.settings_important_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.settings_important_battery_desc),
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
