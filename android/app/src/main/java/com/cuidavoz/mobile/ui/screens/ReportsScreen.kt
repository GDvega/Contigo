package com.cuidavoz.mobile.ui.screens

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.domain.report.MedicalReportBuilder
import com.cuidavoz.mobile.domain.report.MedicalReportPeriod
import com.cuidavoz.mobile.ui.viewmodel.ReportsUiState
import com.cuidavoz.mobile.ui.viewmodel.ReportsViewModel
import com.cuidavoz.mobile.R

@Composable
fun ReportsScreen(
    innerPadding: PaddingValues,
    viewModel: ReportsViewModel,
    onBack: () -> Unit,
    showSpeakScreenButton: Boolean,
    onSpeakScreen: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        viewModel.savePdf(uri)
    }

    if (uiState is ReportsUiState.Ready) {
        val readyState = uiState as ReportsUiState.Ready
        ToastMessageEffect(
            message = readyState.message,
            onConsumed = viewModel::consumeMessage,
        )

        LaunchedEffect(readyState.pendingShareUri) {
            val pendingUri = readyState.pendingShareUri ?: return@LaunchedEffect
            runCatching {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, pendingUri)
                    clipData = ClipData.newRawUri("reporte_medico", pendingUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.reports_share_title)))
                viewModel.markShareHandled()
            }.onFailure {
                viewModel.markShareFailed()
            }
        }
    }

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
        when (val state = uiState) {
            ReportsUiState.Loading -> {
                item {
                    Text(
                        text = stringResource(R.string.reports_title),
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.reports_loading),
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        )
                    }
                }
            }
            is ReportsUiState.Error -> {
                item {
                    Text(
                        text = stringResource(R.string.reports_title),
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    AppCard {
                        Text(
                            text = state.message,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AppButton(
                            label = stringResource(R.string.reports_btn_retry),
                            onClick = { viewModel.loadReport(MedicalReportPeriod.LAST_7_DAYS) },
                        )
                    }
                }
            }
            is ReportsUiState.Ready -> {
                val report = state.reportData

                item {
                    Text(
                        text = stringResource(R.string.reports_title),
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.reports_intro),
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.reports_disclaimer),
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                    )
                }
                item {
                    PeriodSelector(
                        selectedPeriod = state.period,
                        onSelected = viewModel::changePeriod,
                    )
                }
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.reports_section_patient),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = report.patient?.fullName ?: stringResource(R.string.reports_patient_no_name),
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        )
                        Text(
                            text = stringResource(R.string.reports_patient_age, report.patient?.age?.toString() ?: "-"),
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                        )
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.reports_section_pressure),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.reports_pressure_total, report.pressureSummary.totalPressureReadings), fontSize = 18.sp, lineHeight = 24.sp)
                        Text(stringResource(R.string.reports_pressure_latest, MedicalReportBuilder.latestPressureLabel(report.pressureSummary.latestPressure)), fontSize = 18.sp, lineHeight = 24.sp)
                        Text(stringResource(R.string.reports_pressure_out_of_range, report.pressureSummary.outOfRangeCount), fontSize = 18.sp, lineHeight = 24.sp)
                        Text(stringResource(R.string.reports_pressure_high, report.pressureSummary.highOrCriticalCount), fontSize = 18.sp, lineHeight = 24.sp)
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.reports_section_medications),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.reports_meds_active, report.medicationSummary.activeMedicationCount), fontSize = 18.sp, lineHeight = 24.sp)
                        Text(stringResource(R.string.reports_meds_taken, report.medicationSummary.totalMedicationLogs), fontSize = 18.sp, lineHeight = 24.sp)
                        Text(stringResource(R.string.reports_meds_pending, report.medicationSummary.pendingOrSkippedCount), fontSize = 18.sp, lineHeight = 24.sp)
                        Text(stringResource(R.string.reports_meds_adherence, report.medicationSummary.adherencePercentage), fontSize = 18.sp, lineHeight = 24.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        report.activeMedications.forEach { medication ->
                            Text(
                                text = medication.name,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(stringResource(R.string.reports_meds_dose, medication.dose), fontSize = 18.sp, lineHeight = 24.sp)
                            Text(stringResource(R.string.reports_meds_time, com.cuidavoz.mobile.util.formatScheduleTime(medication.scheduleTime)), fontSize = 18.sp, lineHeight = 24.sp)
                            Text(stringResource(R.string.reports_meds_duration, MedicalReportBuilder.medicationDurationLabel(medication)), fontSize = 18.sp, lineHeight = 24.sp)
                            Text(stringResource(R.string.reports_meds_status, MedicalReportBuilder.medicationActiveStatusLabel(medication)), fontSize = 18.sp, lineHeight = 24.sp)
                            Text(stringResource(R.string.reports_meds_instructions, medication.instructions ?: "-"), fontSize = 18.sp, lineHeight = 24.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.reports_section_ranges),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (report.healthSettings == null) {
                            Text(stringResource(R.string.reports_ranges_empty), fontSize = 18.sp, lineHeight = 24.sp)
                        } else {
                            Text(
                                stringResource(R.string.reports_ranges_systolic, report.healthSettings.systolicMinNormal, report.healthSettings.systolicMaxNormal),
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            )
                            Text(
                                stringResource(R.string.reports_ranges_diastolic, report.healthSettings.diastolicMinNormal, report.healthSettings.diastolicMaxNormal),
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            )
                            Text(
                                stringResource(R.string.reports_ranges_pulse, report.healthSettings.pulseMinNormal, report.healthSettings.pulseMaxNormal),
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            )
                        }
                    }
                }
                if (!state.hasEnoughData) {
                    item {
                        AppCard {
                            Text(
                                text = stringResource(R.string.reports_insufficient_data),
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                            )
                        }
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = stringResource(R.string.reports_footer),
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppButton(
                                label = stringResource(R.string.reports_btn_create),
                                onClick = viewModel::generatePdf,
                                enabled = state.busyMessage == null,
                            )
                            AppButton(
                                label = stringResource(R.string.reports_btn_share),
                                onClick = viewModel::sharePdf,
                                enabled = state.busyMessage == null,
                            )
                            AppButton(
                                label = stringResource(R.string.reports_btn_save),
                                onClick = { savePdfLauncher.launch(viewModel.suggestedFileName()) },
                                enabled = state.busyMessage == null,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.reports_pending_help),
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                        )
                        state.cachedPdfFileName?.let { fileName ->
                            Text(
                                text = stringResource(R.string.reports_ready_label, fileName),
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState is ReportsUiState.Ready) {
        val readyState = uiState as ReportsUiState.Ready
        if (!readyState.busyMessage.isNullOrBlank()) {
            ProgressDialog(readyState.busyMessage)
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: MedicalReportPeriod,
    onSelected: (MedicalReportPeriod) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOf(
            MedicalReportPeriod.LAST_7_DAYS to stringResource(R.string.history_filter_7_days),
            MedicalReportPeriod.LAST_30_DAYS to stringResource(R.string.history_filter_30_days),
            MedicalReportPeriod.ALL to stringResource(R.string.history_filter_all),
        ).forEach { (period, label) ->
            AppButton(
                label = label,
                onClick = { onSelected(period) },
                modifier = Modifier.weight(1f),
                enabled = selectedPeriod != period,
                minHeight = 56.dp,
                textSize = 18.sp,
            )
        }
    }
}

@Composable
private fun ProgressDialog(message: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(message) },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
    )
}
