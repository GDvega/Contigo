package com.cuidavoz.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.domain.PressureStatus
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.viewmodel.HistoryTab
import com.cuidavoz.mobile.ui.viewmodel.HistoryViewModel
import com.cuidavoz.mobile.ui.viewmodel.MedicationRangeFilter
import com.cuidavoz.mobile.ui.viewmodel.PressureRangeFilter
import com.cuidavoz.mobile.domain.medicationStatusDetail
import com.cuidavoz.mobile.util.formatDateTime
import com.cuidavoz.mobile.R

@Composable
fun HistoryScreen(
    innerPadding: PaddingValues,
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    showSpeakScreenButton: Boolean,
    onSpeakScreen: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
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
        }
        item {
            Text(
                text = stringResource(R.string.history_title),
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            SegmentRow(
                labels = listOf(stringResource(R.string.history_tab_pressure), stringResource(R.string.history_tab_medications)),
                selectedIndex = if (uiState.selectedTab == HistoryTab.PRESSURE) 0 else 1,
                onSelected = { index ->
                    viewModel.selectTab(if (index == 0) HistoryTab.PRESSURE else HistoryTab.MEDICATIONS)
                },
            )
        }

        if (uiState.selectedTab == HistoryTab.PRESSURE) {
            item {
                SegmentRow(
                    labels = listOf(stringResource(R.string.history_filter_7_days), stringResource(R.string.history_filter_30_days), stringResource(R.string.history_filter_all)),
                    selectedIndex = when (uiState.pressureFilter) {
                        PressureRangeFilter.SEVEN_DAYS -> 0
                        PressureRangeFilter.THIRTY_DAYS -> 1
                        PressureRangeFilter.ALL -> 2
                    },
                    onSelected = { index ->
                        viewModel.selectPressureFilter(
                            when (index) {
                                0 -> PressureRangeFilter.SEVEN_DAYS
                                1 -> PressureRangeFilter.THIRTY_DAYS
                                else -> PressureRangeFilter.ALL
                            }
                        )
                    },
                )
            }

            if (uiState.pressureReadings.isEmpty()) {
                item {
                    AppCard {
                        Text(stringResource(R.string.history_pressure_empty), fontSize = 22.sp, lineHeight = 28.sp)
                    }
                }
            } else {
                items(uiState.pressureReadings, key = { it.id }) { reading ->
                    AppCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${reading.systolic}/${reading.diastolic}", fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                                Text(formatDateTime(reading.measuredAt), fontSize = 18.sp, lineHeight = 24.sp)
                                reading.pulse?.let { Text(stringResource(R.string.history_label_pulse, it), fontSize = 18.sp, lineHeight = 24.sp) }
                                Text(stringResource(R.string.history_label_status, pressureStatusText(reading.status)), fontSize = 20.sp, lineHeight = 26.sp)
                                reading.notes?.let { Text(it, fontSize = 18.sp, lineHeight = 24.sp) }
                            }
                            AppButton(
                                label = "",
                                onClick = { viewModel.deletePressureReading(reading) },
                                icon = Icons.Outlined.Delete,
                                minHeight = 48.dp,
                                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(0.18f),
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        } else {
            item {
                SegmentRow(
                    labels = listOf(stringResource(R.string.history_filter_today), stringResource(R.string.history_filter_7_days), stringResource(R.string.history_filter_30_days), stringResource(R.string.history_filter_all)),
                    selectedIndex = when (uiState.medicationFilter) {
                        MedicationRangeFilter.TODAY -> 0
                        MedicationRangeFilter.SEVEN_DAYS -> 1
                        MedicationRangeFilter.THIRTY_DAYS -> 2
                        MedicationRangeFilter.ALL -> 3
                    },
                    onSelected = { index ->
                        viewModel.selectMedicationFilter(
                            when (index) {
                                0 -> MedicationRangeFilter.TODAY
                                1 -> MedicationRangeFilter.SEVEN_DAYS
                                2 -> MedicationRangeFilter.THIRTY_DAYS
                                else -> MedicationRangeFilter.ALL
                            }
                        )
                    },
                )
            }

            if (uiState.medicationHistory.isEmpty()) {
                item {
                    AppCard {
                        Text(stringResource(R.string.history_medications_empty), fontSize = 22.sp, lineHeight = 28.sp)
                    }
                }
            } else {
                items(uiState.medicationHistory, key = { "${it.medicationName}_${it.scheduledFor}" }) { item ->
                    AppCard {
                        Text(item.medicationName, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.history_label_scheduled, formatDateTime(item.scheduledFor)), fontSize = 18.sp, lineHeight = 24.sp)
                        Text(stringResource(R.string.history_label_status, medicationStatusDetail(item.status, item.skipReason)), fontSize = 20.sp, lineHeight = 26.sp)
                        item.takenAt?.let { Text(stringResource(R.string.history_label_taken, formatDateTime(it)), fontSize = 18.sp, lineHeight = 24.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        labels.forEachIndexed { index, label ->
            AppButton(
                label = label,
                onClick = { onSelected(index) },
                modifier = Modifier.weight(1f),
                enabled = index != selectedIndex,
                minHeight = 56.dp,
                textSize = 20.sp,
            )
        }
    }
}

@Composable
private fun pressureStatusText(status: String): String {
    return when (runCatching { PressureStatus.valueOf(status) }.getOrNull()) {
        PressureStatus.NORMAL -> stringResource(R.string.pressure_status_normal)
        PressureStatus.ELEVATED -> stringResource(R.string.pressure_status_elevated)
        PressureStatus.HIGH -> stringResource(R.string.pressure_status_high)
        PressureStatus.CRITICAL -> stringResource(R.string.pressure_status_critical)
        PressureStatus.OUT_OF_RANGE -> stringResource(R.string.pressure_status_out_of_range)
        null -> status
    }
}

private fun medicationStatusText(status: String): String {
    return when (status) {
        "TAKEN" -> "Tomado"
        "SKIPPED" -> "Omitido"
        else -> "Pendiente"
    }
}
