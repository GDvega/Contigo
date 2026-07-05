package com.cuidavoz.mobile.ui.screens

import android.content.Intent
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
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.cuidavoz.mobile.R
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.viewmodel.FamilyViewModel

import com.cuidavoz.mobile.util.formatDateTime

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
                text = stringResource(R.string.family_title),
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (showSpeakScreenButton) {
            item {
                AppButton(
                    label = stringResource(R.string.family_btn_speak),
                    onClick = onSpeakScreen,
                )
            }
        }

        item {
            AppCard {
                Text(
                    text = uiState.patient?.fullName ?: stringResource(R.string.family_patient_default),
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.family_patient_age, uiState.patient?.age ?: 0),
                    fontSize = 20.sp,
                    lineHeight = 26.sp
                )
                Text(
                    text = stringResource(
                        R.string.family_patient_status,
                        uiState.dailyStatus?.statusTitle ?: stringResource(R.string.family_status_ok)
                    ),
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                )
            }
        }

        item {
            AppCard {
                Text(stringResource(R.string.family_pressure_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                
                val pressureSummary = uiState.latestPressure?.let {
                    "${it.systolic}/${it.diastolic}${it.pulse?.let { p -> " · " + stringResource(R.string.family_pressure_pulse, p) } ?: ""}"
                } ?: stringResource(R.string.family_pressure_empty)
                
                Text(pressureSummary, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                
                val pressureDetail = uiState.latestPressure?.let { formatDateTime(it.measuredAt) }
                    ?: stringResource(R.string.family_pressure_empty_detail)
                    
                Text(pressureDetail, fontSize = 18.sp, lineHeight = 24.sp)
                Text(stringResource(uiState.pressureSafetyResId), fontSize = 20.sp, lineHeight = 26.sp)
            }
        }

        item {
            AppCard {
                Text(stringResource(R.string.family_meds_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.family_meds_active, uiState.dailyStatus?.activeMedicationCount ?: 0), fontSize = 20.sp, lineHeight = 26.sp)
                Text(stringResource(R.string.family_meds_taken, uiState.dailyStatus?.takenMedicationCount ?: 0), fontSize = 20.sp, lineHeight = 26.sp)
                Text(stringResource(R.string.family_meds_pending, uiState.dailyStatus?.pendingMedicationCount ?: 0), fontSize = 20.sp, lineHeight = 26.sp)
                
                val adherence = if (uiState.hasAdherence) uiState.adherenceText else stringResource(R.string.family_adherence_none)
                Text(
                    text = stringResource(R.string.family_meds_adherence, adherence),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            AppCard {
                Text(stringResource(R.string.family_contact_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(uiState.contact?.fullName ?: stringResource(R.string.family_contact_none), fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold)
                Text(uiState.contact?.relationship ?: "-", fontSize = 20.sp, lineHeight = 26.sp)
                Text(uiState.contact?.phone ?: "-", fontSize = 22.sp, lineHeight = 28.sp)
                AppButton(
                    label = stringResource(R.string.family_btn_call),
                    onClick = {
                        val phone = uiState.contact?.phone ?: return@AppButton
                        val intent = Intent(Intent.ACTION_DIAL, "tel:${phone.replace(" ", "")}".toUri())
                        context.startActivity(intent)
                    },
                    enabled = uiState.hasContact,
                )
            }
        }

        if (uiState.alertsResIds.isNotEmpty()) {
            items(uiState.alertsResIds) { alertResId ->
                AppCard {
                    Text(stringResource(R.string.family_alert_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(stringResource(alertResId), fontSize = 18.sp, lineHeight = 24.sp)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(
                    label = stringResource(R.string.family_btn_create_report),
                    onClick = onOpenReports,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
