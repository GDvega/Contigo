package com.cuidavoz.mobile.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.viewmodel.FamilyViewModel

@Composable
fun FamilyScreen(
    innerPadding: PaddingValues,
    viewModel: FamilyViewModel,
    onOpenReports: () -> Unit,
    showSpeakScreenButton: Boolean,
    onSpeakScreen: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .padding(innerPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = "Familia",
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (showSpeakScreenButton) {
            item {
                AppButton(
                    label = "Escuchar esta pantalla",
                    onClick = onSpeakScreen,
                )
            }
        }

        item {
            AppCard {
                Text(uiState.patient?.fullName ?: "Paciente", fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Text("Edad: ${uiState.patient?.age ?: "-"} años", fontSize = 20.sp, lineHeight = 26.sp)
                Text("Estado general: ${uiState.dailyStatus?.statusTitle ?: "Todo en orden"}", fontSize = 22.sp, lineHeight = 28.sp)
            }
        }

        item {
            AppCard {
                Text("Última presión", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(uiState.latestPressureSummary, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                Text(uiState.latestPressureDetail, fontSize = 18.sp, lineHeight = 24.sp)
                Text(uiState.pressureSafetyText, fontSize = 20.sp, lineHeight = 26.sp)
            }
        }

        item {
            AppCard {
                Text("Medicamentos de hoy", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Activos: ${uiState.dailyStatus?.activeMedicationCount ?: 0}", fontSize = 20.sp, lineHeight = 26.sp)
                Text("Tomados: ${uiState.dailyStatus?.takenMedicationCount ?: 0}", fontSize = 20.sp, lineHeight = 26.sp)
                Text("Pendientes: ${uiState.dailyStatus?.pendingMedicationCount ?: 0}", fontSize = 20.sp, lineHeight = 26.sp)
                Text("Adherencia: ${uiState.adherenceText}", fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            AppCard {
                Text("Contacto familiar", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(uiState.contact?.fullName ?: "Sin contacto", fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                Text(uiState.contact?.relationship ?: "-", fontSize = 20.sp, lineHeight = 26.sp)
                Text(uiState.contact?.phone ?: "-", fontSize = 22.sp, lineHeight = 28.sp)
                AppButton(
                    label = "Llamar",
                    onClick = {
                        val phone = uiState.contact?.phone ?: return@AppButton
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}"))
                        context.startActivity(intent)
                    },
                    enabled = uiState.hasContact,
                )
            }
        }

        if (uiState.alerts.isNotEmpty()) {
            items(uiState.alerts) { alert ->
                AppCard {
                    Text("Alerta", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(alert, fontSize = 18.sp, lineHeight = 24.sp)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(
                    label = "Crear reporte",
                    onClick = onOpenReports,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
