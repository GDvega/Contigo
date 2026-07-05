package com.cuidavoz.mobile.ui.screens.caregiver

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.theme.ContigoTheme
import com.cuidavoz.mobile.ui.viewmodel.HistoricalPressureViewModel
import com.cuidavoz.mobile.util.formatDate
import com.cuidavoz.mobile.R
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalPressureScreen(
    innerPadding: PaddingValues,
    viewModel: HistoricalPressureViewModel,
    onBack: () -> Unit,
) {
    val extraColors = ContigoTheme.extraColors
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.selectedDateMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val now = System.currentTimeMillis()
                val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
                // Ajustar a medianoche para comparación justa si fuera necesario, 
                // pero Material 3 maneja UTC.
                return utcTimeMillis in (thirtyDaysAgo..now)
            }
        }
    )

    val timePickerState = rememberTimePickerState(
        initialHour = uiState.selectedHour,
        initialMinute = uiState.selectedMinute,
        is24Hour = true
    )

    ToastMessageEffect(
        message = uiState.message,
        onConsumed = viewModel::dismissMessage,
    )

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.resetSuccess()
            onBack()
        }
    }

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
        FilledTonalButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.caregiver_btn_back))
            Text(stringResource(R.string.caregiver_btn_back))
        }

        Text(
            text = stringResource(R.string.historical_title),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.historical_intro),
            fontSize = 20.sp,
            lineHeight = 26.sp,
        )

        AppCard {
            Text(stringResource(R.string.historical_section_datetime), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            AppButton(
                label = stringResource(R.string.historical_label_date, formatDate(uiState.selectedDateMillis)),
                onClick = { showDatePicker = true },
                icon = Icons.Outlined.CalendarMonth,
                minHeight = 56.dp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val hourStr = uiState.selectedHour.toString().padStart(2, '0')
            val minStr = uiState.selectedMinute.toString().padStart(2, '0')
            AppButton(
                label = stringResource(R.string.historical_label_time, hourStr, minStr),
                onClick = { showTimePicker = true },
                icon = Icons.Outlined.Schedule,
                minHeight = 56.dp
            )
        }

        HistoricalInputCard(
            title = stringResource(R.string.historical_label_systolic),
            value = uiState.systolic,
            onValueChange = viewModel::onSystolicChange,
            icon = Icons.Outlined.Favorite,
            iconTint = extraColors.errorRed,
            unit = stringResource(R.string.pressure_unit_mmHg),
        )
        
        HistoricalInputCard(
            title = stringResource(R.string.historical_label_diastolic),
            value = uiState.diastolic,
            onValueChange = viewModel::onDiastolicChange,
            icon = Icons.Outlined.Speed,
            iconTint = extraColors.infoBlue,
            unit = stringResource(R.string.pressure_unit_mmHg),
        )
        
        HistoricalInputCard(
            title = stringResource(R.string.historical_label_pulse),
            value = uiState.pulse,
            onValueChange = viewModel::onPulseChange,
            icon = Icons.Outlined.MonitorHeart,
            iconTint = extraColors.reportIcon,
            unit = stringResource(R.string.pressure_unit_pulse).replace("por min", "lpm"),
        )

        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Notes,
                    contentDescription = stringResource(R.string.historical_label_notes),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.historical_label_notes),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text(stringResource(R.string.historical_notes_placeholder)) },
                minLines = 2
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AppButton(
            label = stringResource(R.string.historical_btn_save_another),
            onClick = { viewModel.saveReading(keepDate = true) },
            enabled = !uiState.isSaving,
            minHeight = 64.dp,
            textSize = 20.sp
        )

        AppButton(
            label = stringResource(R.string.historical_btn_save_exit),
            onClick = { viewModel.saveReading(keepDate = false) },
            enabled = !uiState.isSaving,
            minHeight = 64.dp,
            textSize = 20.sp
        )

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showDatePicker) {
// ... resto del archivo
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            AppCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(R.string.historical_time_picker_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                        TextButton(onClick = {
                            viewModel.onTimeChange(timePickerState.hour, timePickerState.minute)
                            showTimePicker = false
                        }) {
                            Text(stringResource(R.string.btn_accept))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricalInputCard(
    title: String,
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
            )
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.headlineMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = { Text(unit) },
        )
    }
}
