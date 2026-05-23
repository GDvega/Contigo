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
                context.startActivity(Intent.createChooser(shareIntent, "Compartir reporte médico"))
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
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                    Text("Volver")
                }
                if (showSpeakScreenButton) {
                    FilledTonalButton(onClick = onSpeakScreen, modifier = Modifier.height(56.dp)) {
                        Text("Escuchar")
                    }
                }
            }
        }
        when (val state = uiState) {
            ReportsUiState.Loading -> {
                item {
                    Text(
                        text = "Reporte para el médico",
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    AppCard {
                        Text(
                            text = "Preparando reporte...",
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        )
                    }
                }
            }
            is ReportsUiState.Error -> {
                item {
                    Text(
                        text = "Reporte para el médico",
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
                            label = "Intentar otra vez",
                            onClick = { viewModel.loadReport(MedicalReportPeriod.LAST_7_DAYS) },
                        )
                    }
                }
            }
            is ReportsUiState.Ready -> {
                val report = state.reportData

                item {
                    Text(
                        text = "Reporte para el médico",
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                item {
                    Text(
                        text = "Prepara un reporte para tu médico.",
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    )
                }
                item {
                    Text(
                        text = "El reporte es para compartir con el médico. El respaldo es para recuperar tus datos.",
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
                            text = "Paciente",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = report.patient?.fullName ?: "Sin nombre registrado",
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                        )
                        Text(
                            text = "Edad: ${report.patient?.age?.toString() ?: "-"}",
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                        )
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = "Presión arterial",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Total de registros: ${report.pressureSummary.totalPressureReadings}", fontSize = 18.sp, lineHeight = 24.sp)
                        Text("Última presión: ${MedicalReportBuilder.latestPressureLabel(report.pressureSummary.latestPressure)}", fontSize = 18.sp, lineHeight = 24.sp)
                        Text("Fuera de rango: ${report.pressureSummary.outOfRangeCount}", fontSize = 18.sp, lineHeight = 24.sp)
                        Text("Altos o muy altos: ${report.pressureSummary.highOrCriticalCount}", fontSize = 18.sp, lineHeight = 24.sp)
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = "Medicamentos",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Medicamentos activos: ${report.medicationSummary.activeMedicationCount}", fontSize = 18.sp, lineHeight = 24.sp)
                        Text("Tomas registradas: ${report.medicationSummary.totalMedicationLogs}", fontSize = 18.sp, lineHeight = 24.sp)
                        Text("Pendientes u omitidas: ${report.medicationSummary.pendingOrSkippedCount}", fontSize = 18.sp, lineHeight = 24.sp)
                        Text("Adherencia: ${report.medicationSummary.adherencePercentage}%", fontSize = 18.sp, lineHeight = 24.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        report.activeMedications.forEach { medication ->
                            Text(
                                text = medication.name,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Dosis: ${medication.dose}", fontSize = 18.sp, lineHeight = 24.sp)
                            Text("Hora: ${com.cuidavoz.mobile.util.formatScheduleTime(medication.scheduleTime)}", fontSize = 18.sp, lineHeight = 24.sp)
                            Text("Duración: ${MedicalReportBuilder.medicationDurationLabel(medication)}", fontSize = 18.sp, lineHeight = 24.sp)
                            Text("Estado: ${MedicalReportBuilder.medicationActiveStatusLabel(medication)}", fontSize = 18.sp, lineHeight = 24.sp)
                            Text("Instrucciones: ${medication.instructions ?: "-"}", fontSize = 18.sp, lineHeight = 24.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = "Rangos del médico",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (report.healthSettings == null) {
                            Text("Sin rangos configurados.", fontSize = 18.sp, lineHeight = 24.sp)
                        } else {
                            Text(
                                "Sistólica: ${report.healthSettings.systolicMinNormal} - ${report.healthSettings.systolicMaxNormal}",
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            )
                            Text(
                                "Diastólica: ${report.healthSettings.diastolicMinNormal} - ${report.healthSettings.diastolicMaxNormal}",
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                            )
                            Text(
                                "Pulso: ${report.healthSettings.pulseMinNormal} - ${report.healthSettings.pulseMaxNormal}",
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
                                text = "No hay suficientes datos para generar un reporte completo. Puedes registrar presión o medicamentos primero.",
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                            )
                        }
                    }
                }
                item {
                    AppCard {
                        Text(
                            text = "Este reporte contiene información de salud. Compártelo solo con personas de confianza.",
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AppButton(
                                label = "Crear reporte",
                                onClick = viewModel::generatePdf,
                                enabled = state.busyMessage == null,
                            )
                            AppButton(
                                label = "Compartir reporte",
                                onClick = viewModel::sharePdf,
                                enabled = state.busyMessage == null,
                            )
                            AppButton(
                                label = "Guardar reporte",
                                onClick = { savePdfLauncher.launch(viewModel.suggestedFileName()) },
                                enabled = state.busyMessage == null,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Si todavía no está listo, lo preparamos antes de compartirlo.",
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                        )
                        state.cachedPdfFileName?.let { fileName ->
                            Text(
                                text = "Reporte listo: $fileName",
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
            MedicalReportPeriod.LAST_7_DAYS to "Últimos 7 días",
            MedicalReportPeriod.LAST_30_DAYS to "Últimos 30 días",
            MedicalReportPeriod.ALL to "Todo",
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
