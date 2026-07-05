package com.cuidavoz.mobile.ui.screens.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.cuidavoz.mobile.ui.theme.ContigoTheme
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.MedicationImagePreview
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.viewmodel.ActivityPriority
import com.cuidavoz.mobile.ui.viewmodel.CaregiverActivityType
import com.cuidavoz.mobile.ui.viewmodel.CaregiverActivityUi
import com.cuidavoz.mobile.ui.viewmodel.CaregiverDashboardUiState
import com.cuidavoz.mobile.ui.viewmodel.CaregiverMedicationUi
import com.cuidavoz.mobile.ui.viewmodel.CaregiverMedicationStatus
import com.cuidavoz.mobile.ui.viewmodel.CaregiverPressureUi
import com.cuidavoz.mobile.R

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
fun CaregiverDashboardScreenPreview() {
    ContigoTheme(isCaregiverMode = true) {
        CaregiverDashboardScreen(
            innerPadding = PaddingValues(0.dp),
            uiState = CaregiverDashboardUiState(
                patientName = "Juan Pérez",
                todayStatusText = "Todo controlado"
            ),
            onDismissMessage = {},
            onBack = {},
            onOpenLinking = {},
            onOpenMedications = {},
            onOpenRecords = {},
            onOpenHistoricalPressure = {},
            onOpenReports = {},
            onOpenFamilyContact = {},
            onOpenSettings = {},
            onOpenBackup = {},
            onRetrySync = {},
            onToggleSync = {},
            onCallPatient = {},
            onReturnPatient = {}
        )
    }
}

@Composable
fun CaregiverDashboardScreen(
    innerPadding: PaddingValues,
    uiState: CaregiverDashboardUiState,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit,
    onOpenLinking: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenHistoricalPressure: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenFamilyContact: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onRetrySync: () -> Unit,
    onToggleSync: (Boolean) -> Unit,
    onCallPatient: () -> Unit,
    onReturnPatient: () -> Unit,
) {
    ToastMessageEffect(
        message = uiState.message,
        onConsumed = onDismissMessage,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DashboardHeader(
                patientName = uiState.patientName,
                onBack = onBack,
                onCallPatient = onCallPatient,
            )
        }

        if (uiState.isLoading) {
            item {
                AppCard {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text(stringResource(R.string.caregiver_loading_patient), fontSize = 20.sp, lineHeight = 26.sp)
                    }
                }
            }
        } else {
            item {
                TodayStatusCard(uiState = uiState)
            }
            item {
                MainMedicationCard(medication = uiState.nextMedication)
            }
            item {
                MedicationSectionCard(
                    title = stringResource(R.string.caregiver_section_pending),
                    medications = uiState.pendingMedications,
                    emptyMessage = stringResource(R.string.caregiver_meds_pending_empty),
                    remainingCount = uiState.remainingPendingCount,
                    visuallySecondary = false,
                )
            }
            item {
                MedicationSectionCard(
                    title = stringResource(R.string.caregiver_section_taken),
                    medications = uiState.takenMedications,
                    emptyMessage = stringResource(R.string.caregiver_meds_taken_empty),
                    remainingCount = uiState.remainingTakenCount,
                    visuallySecondary = true,
                )
            }
            item {
                LatestPressureCard(pressure = uiState.latestPressure)
            }
            item {
                RecentActivityCard(recentActivity = uiState.recentActivity)
            }
        }

        item {
            SyncStatusCard(
                uiState = uiState,
                onOpenLinking = onOpenLinking,
                onRetrySync = onRetrySync,
                onToggleSync = onToggleSync,
            )
        }
        item {
            Text(
                text = stringResource(R.string.caregiver_actions_title),
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            CaregiverActionRow(
                firstTitle = stringResource(R.string.caregiver_action_medications),
                firstIcon = Icons.Outlined.LocalHospital,
                onFirstClick = onOpenMedications,
                secondTitle = stringResource(R.string.caregiver_action_history),
                secondIcon = Icons.Outlined.History,
                onSecondClick = onOpenRecords,
            )
        }
        item {
            CaregiverActionRow(
                firstTitle = stringResource(R.string.caregiver_action_load_history),
                firstIcon = Icons.Outlined.FileUpload,
                onFirstClick = onOpenHistoricalPressure,
                secondTitle = stringResource(R.string.caregiver_action_reports),
                secondIcon = Icons.Outlined.Description,
                onSecondClick = onOpenReports,
            )
        }
        item {
            CaregiverActionRow(
                firstTitle = stringResource(R.string.caregiver_action_contact),
                firstIcon = Icons.Outlined.Favorite,
                onFirstClick = onOpenFamilyContact,
                secondTitle = stringResource(R.string.caregiver_action_settings),
                secondIcon = Icons.Outlined.Settings,
                onSecondClick = onOpenSettings,
            )
        }
        item {
            CaregiverActionButton(
                title = stringResource(R.string.caregiver_action_backup),
                icon = Icons.Outlined.Lock,
                onClick = onOpenBackup,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            AppButton(
                label = stringResource(R.string.caregiver_btn_return_patient),
                onClick = onReturnPatient,
                minHeight = 60.dp,
                textSize = 22.sp,
                contentDescription = "Botón Volver al modo paciente",
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    patientName: String,
    onBack: () -> Unit,
    onCallPatient: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(onClick = onBack, modifier = Modifier.height(56.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            Text(stringResource(R.string.caregiver_btn_back))
        }
        FilledTonalButton(onClick = onCallPatient, modifier = Modifier.height(56.dp)) {
            Icon(Icons.Outlined.Phone, contentDescription = "Llamar paciente")
            Text(stringResource(R.string.caregiver_btn_call))
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.caregiver_label_patient_state, patientName),
        fontSize = 30.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun TodayStatusCard(uiState: CaregiverDashboardUiState) {
    val containerColor = when {
        uiState.hasOverdueMedications -> MaterialTheme.colorScheme.errorContainer
        uiState.pendingToday > 0 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    AppCard(containerColor = containerColor) {
        Text(stringResource(R.string.caregiver_status_title), fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = uiState.todayStatusText,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
        )
        if (uiState.totalToday > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.caregiver_status_taken_count, uiState.takenToday, uiState.totalToday),
                fontSize = 21.sp,
                lineHeight = 27.sp,
            )
            if (uiState.pendingToday > 0) {
                Text(
                    text = stringResource(R.string.caregiver_status_pending_count, uiState.pendingToday),
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun MainMedicationCard(medication: CaregiverMedicationUi?) {
    AppCard(
        containerColor = when (medication?.status) {
            CaregiverMedicationStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
            CaregiverMedicationStatus.DUE_NOW -> MaterialTheme.colorScheme.secondaryContainer
            CaregiverMedicationStatus.TAKEN -> MaterialTheme.colorScheme.surfaceVariant
            CaregiverMedicationStatus.UPCOMING,
            null -> MaterialTheme.colorScheme.surface
        },
    ) {
        Text(
            text = if (medication?.isOverdue == true) stringResource(R.string.caregiver_medication_overdue_title) else stringResource(R.string.caregiver_medication_upcoming),
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (medication == null) {
            Text(stringResource(R.string.caregiver_medication_none), fontSize = 20.sp, lineHeight = 26.sp)
            return@AppCard
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MedicationImagePreview(
                imageUri = medication.imageUri,
                label = medication.name,
                size = 88.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusLabel(
                    text = medication.statusText,
                    priority = medication.status.toActivityPriority(),
                )
                Text(
                    text = medication.name,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(medication.dosage, fontSize = 19.sp, lineHeight = 25.sp)
                Text(
                    text = medication.scheduledTimeText,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MedicationSectionCard(
    title: String,
    medications: List<CaregiverMedicationUi>,
    emptyMessage: String,
    remainingCount: Int,
    visuallySecondary: Boolean,
) {
    AppCard(
        containerColor = if (visuallySecondary) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Text(title, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        if (medications.isEmpty()) {
            Text(emptyMessage, fontSize = 19.sp, lineHeight = 25.sp)
        } else {
            medications.forEachIndexed { index, medication ->
                MedicationStatusRow(
                    medication = medication,
                    visuallySecondary = visuallySecondary,
                )
                if (index != medications.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            if (remainingCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.caregiver_meds_remaining, remainingCount),
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MedicationStatusRow(
    medication: CaregiverMedicationUi,
    visuallySecondary: Boolean,
) {
    val rowBackground = when (medication.status) {
        CaregiverMedicationStatus.OVERDUE -> MaterialTheme.colorScheme.errorContainer
        CaregiverMedicationStatus.DUE_NOW -> MaterialTheme.colorScheme.secondaryContainer
        CaregiverMedicationStatus.UPCOMING -> MaterialTheme.colorScheme.surface
        CaregiverMedicationStatus.TAKEN -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground, MaterialTheme.shapes.medium)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MedicationImagePreview(
            imageUri = medication.imageUri,
            label = medication.name,
            size = 64.dp,
            alpha = if (visuallySecondary) 0.72f else 1f,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = medication.name,
                fontSize = 21.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${medication.dosage} · ${medication.scheduledTimeText}",
                fontSize = 17.sp,
                lineHeight = 23.sp,
            )
            Text(
                text = medication.takenTimeText?.let { stringResource(R.string.caregiver_label_taken_at, it) } ?: medication.statusText,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = if (medication.status == CaregiverMedicationStatus.TAKEN) {
                    FontWeight.Normal
                } else {
                    FontWeight.Bold
                },
                color = when {
                    medication.isOverdue -> MaterialTheme.colorScheme.error
                    medication.status == CaregiverMedicationStatus.DUE_NOW -> MaterialTheme.colorScheme.primary
                    visuallySecondary -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.primary
                },
            )
        }
    }
}

@Composable
private fun LatestPressureCard(pressure: CaregiverPressureUi?) {
    val containerColor = when {
        pressure?.priority == ActivityPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        pressure?.priority == ActivityPriority.WARNING || pressure?.isOld == true ->
            MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    AppCard(containerColor = containerColor) {
        Text(stringResource(R.string.caregiver_pressure_title), fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (pressure == null) {
            Text(stringResource(R.string.caregiver_pressure_empty), fontSize = 20.sp, lineHeight = 26.sp)
            return@AppCard
        }
        Text(
            text = "${pressure.systolic}/${pressure.diastolic}",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )
        pressure.pulse?.let {
            Text(stringResource(R.string.caregiver_pressure_pulse, it), fontSize = 19.sp, lineHeight = 25.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        StatusLabel(
            text = pressure.classificationText,
            priority = pressure.priority,
        )
        pressure.attentionText?.let { attentionText ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = attentionText,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (pressure.isOld) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.caregiver_pressure_old_warning),
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = pressure.measuredAtText,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecentActivityCard(recentActivity: List<CaregiverActivityUi>) {
    AppCard {
        Text(stringResource(R.string.caregiver_activity_title), fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (recentActivity.isEmpty()) {
            Text(
                stringResource(R.string.caregiver_activity_empty),
                fontSize = 19.sp,
                lineHeight = 25.sp,
            )
        } else {
            recentActivity.forEachIndexed { index, activity ->
                ActivityRow(activity = activity)
                if (index != recentActivity.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: CaregiverActivityUi) {
    val icon = when (activity.type) {
        CaregiverActivityType.MEDICATION_TAKEN -> Icons.Outlined.CheckCircle
        CaregiverActivityType.MEDICATION_SKIPPED -> Icons.Outlined.Warning
        CaregiverActivityType.PRESSURE_RECORDED -> Icons.Outlined.Favorite
    }
    val iconTint = when (activity.priority) {
        ActivityPriority.NORMAL -> MaterialTheme.colorScheme.primary
        ActivityPriority.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        ActivityPriority.CRITICAL -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = activity.title,
                fontSize = 19.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(activity.subtitle, fontSize = 17.sp, lineHeight = 23.sp)
            Text(
                text = activity.timeText,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SyncStatusCard(
    uiState: CaregiverDashboardUiState,
    onOpenLinking: () -> Unit,
    onRetrySync: () -> Unit,
    onToggleSync: (Boolean) -> Unit,
) {
    AppCard(
        containerColor = if (uiState.syncNeedsAttention) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
    ) {
        Text(stringResource(R.string.caregiver_sync_title), fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        StatusLabel(
            text = uiState.syncStatusText,
            priority = if (uiState.syncNeedsAttention) ActivityPriority.WARNING else ActivityPriority.NORMAL,
        )
        uiState.syncDetailText?.let { detail ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(detail, fontSize = 16.sp, lineHeight = 22.sp)
        }
        Text(
            text = if (uiState.familyLinked) stringResource(R.string.caregiver_sync_active) else stringResource(R.string.caregiver_sync_inactive),
            fontSize = 16.sp,
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.caregiver_sync_enable_label), fontSize = 17.sp, lineHeight = 23.sp)
            Switch(checked = uiState.syncEnabled, onCheckedChange = onToggleSync)
        }
        FilledTonalButton(
            onClick = onOpenLinking,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Icon(Icons.Outlined.Link, contentDescription = null)
            Text(stringResource(R.string.caregiver_sync_btn_link), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        FilledTonalButton(
            onClick = onRetrySync,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Text(stringResource(R.string.caregiver_sync_btn_now), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatusLabel(
    text: String,
    priority: ActivityPriority,
) {
    val containerColor = when (priority) {
        ActivityPriority.NORMAL -> MaterialTheme.colorScheme.primaryContainer
        ActivityPriority.WARNING -> MaterialTheme.colorScheme.secondaryContainer
        ActivityPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = when (priority) {
        ActivityPriority.NORMAL -> MaterialTheme.colorScheme.onPrimaryContainer
        ActivityPriority.WARNING -> MaterialTheme.colorScheme.onSecondaryContainer
        ActivityPriority.CRITICAL -> MaterialTheme.colorScheme.onErrorContainer
    }
    Text(
        text = text,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        modifier = Modifier
            .background(containerColor, MaterialTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

private fun CaregiverMedicationStatus.toActivityPriority(): ActivityPriority = when (this) {
    CaregiverMedicationStatus.OVERDUE -> ActivityPriority.CRITICAL
    CaregiverMedicationStatus.DUE_NOW -> ActivityPriority.WARNING
    CaregiverMedicationStatus.UPCOMING,
    CaregiverMedicationStatus.TAKEN -> ActivityPriority.NORMAL
}

@Composable
private fun CaregiverActionRow(
    firstTitle: String,
    firstIcon: ImageVector,
    onFirstClick: () -> Unit,
    secondTitle: String,
    secondIcon: ImageVector,
    onSecondClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CaregiverActionButton(
            title = firstTitle,
            icon = firstIcon,
            onClick = onFirstClick,
            modifier = Modifier.weight(1f),
        )
        CaregiverActionButton(
            title = secondTitle,
            icon = secondIcon,
            onClick = onSecondClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CaregiverActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = title,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
