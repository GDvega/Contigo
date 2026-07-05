package com.cuidavoz.mobile.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.data.backup.ImportStrategy
import com.cuidavoz.mobile.data.model.DeviceRole
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.BackupPasswordDialog
import com.cuidavoz.mobile.ui.components.BackupSummaryDialog
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.navigation.UserMode
import com.cuidavoz.mobile.ui.viewmodel.BackupUiState
import com.cuidavoz.mobile.ui.viewmodel.BackupViewModel
import com.cuidavoz.mobile.ui.viewmodel.OnboardingField
import com.cuidavoz.mobile.ui.viewmodel.OnboardingScreenState
import com.cuidavoz.mobile.ui.viewmodel.OnboardingStep
import com.cuidavoz.mobile.ui.viewmodel.OnboardingViewModel
import com.cuidavoz.mobile.util.PhoneVisualTransformation
import com.cuidavoz.mobile.R

@Composable
fun OnboardingScreen(
    innerPadding: PaddingValues,
    viewModel: OnboardingViewModel,
    backupViewModel: BackupViewModel,
    onSetupFinished: (UserMode) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backupUiState by backupViewModel.uiState.collectAsStateWithLifecycle()
    var showReplaceConfirmation by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImportPassword by remember { mutableStateOf("") }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        pendingImportPassword = ""
        showImportPasswordDialog = true
    }

    LaunchedEffect(uiState.setupCompleted, uiState.savedDeviceRole, uiState.selectedRole) {
        if (uiState.setupCompleted == true) {
            onSetupFinished(viewModel.resolvedUserMode())
        }
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

    if (uiState.setupCompleted == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_title),
            modifier = Modifier.testTag(OnboardingTestTags.TITLE),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )

        AnimatedContent(
            targetState = uiState.step,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "OnboardingTransition"
        ) { step ->
            when (step) {
                OnboardingStep.ROLE_SELECTION -> RoleSelectionContent(
                    selectedRole = uiState.selectedRole,
                    onSelectRole = viewModel::selectRole,
                    onContinue = viewModel::continueFromRoleSelection,
                )
                OnboardingStep.DETAILS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        TextButton(onClick = viewModel::backToRoleSelection) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.onboarding_change_profile))
                        }
                        
                        when (uiState.selectedRole) {
                            DeviceRole.CAREGIVER -> CaregiverDetailsContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                importBackupLauncher = importBackupLauncher,
                            )
                            DeviceRole.PATIENT, null -> PatientDetailsContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                importBackupLauncher = importBackupLauncher,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showImportPasswordDialog) {
        BackupPasswordDialog(
            title = stringResource(R.string.backup_import_pwd_title),
            description = stringResource(R.string.backup_import_pwd_desc),
            confirmLabel = stringResource(R.string.btn_continue),
            passwordRequired = false,
            password = pendingImportPassword,
            onPasswordChange = { pendingImportPassword = it },
            onConfirm = {
                val uri = pendingImportUri
                val password = pendingImportPassword.toCharArray().takeIf { it.isNotEmpty() }
                pendingImportPassword = ""
                showImportPasswordDialog = false
                pendingImportUri = null
                if (uri != null) {
                    backupViewModel.readBackupSummary(uri, password)
                }
            },
            onDismiss = {
                pendingImportPassword = ""
                pendingImportUri = null
                showImportPasswordDialog = false
            },
        )
    }

    when (val state = backupUiState) {
        BackupUiState.Idle -> Unit
        BackupUiState.Exporting -> Unit
        is BackupUiState.ExportSuccess -> Unit
        BackupUiState.ImportReading -> OnboardingProgressDialog(stringResource(R.string.dialog_import_reading))
        is BackupUiState.ImportPreview -> BackupSummaryDialog(
            summary = state.summary,
            onCancel = backupViewModel::dismissState,
            onMerge = { backupViewModel.importBackup(ImportStrategy.MERGE) },
            onReplace = { showReplaceConfirmation = true },
        )
        is BackupUiState.Importing -> OnboardingProgressDialog(
            if (state.strategy == ImportStrategy.REPLACE_ALL) {
                stringResource(R.string.dialog_importing_restore)
            } else {
                stringResource(R.string.dialog_importing_import)
            },
        )
        is BackupUiState.ImportSuccess -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.dialog_import_success_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_import_success_message,
                        state.result.importedMedications,
                        state.result.importedPressureReadings,
                        state.result.importedMedicationLogs,
                        state.result.importedImages,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        backupViewModel.dismissState()
                        viewModel.completeAfterImport()
                    },
                ) {
                    Text(stringResource(R.string.btn_enter))
                }
            },
        )
        is BackupUiState.Error -> AlertDialog(
            onDismissRequest = backupViewModel::dismissState,
            title = { Text(stringResource(R.string.dialog_import_error_title)) },
            text = { Text(state.message) },
            confirmButton = {
                TextButton(onClick = backupViewModel::dismissState) {
                    Text(stringResource(R.string.btn_accept))
                }
            },
        )
    }

    if (showReplaceConfirmation && backupUiState is BackupUiState.ImportPreview) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirmation = false },
            title = { Text(stringResource(R.string.dialog_replace_confirm_title)) },
            text = {
                Text(stringResource(R.string.dialog_replace_confirm_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirmation = false
                        backupViewModel.importBackup(ImportStrategy.REPLACE_ALL)
                    },
                ) {
                    Text(stringResource(R.string.btn_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmation = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
        )
    }
}

@Composable
private fun RoleSelectionContent(
    selectedRole: DeviceRole?,
    onSelectRole: (DeviceRole) -> Unit,
    onContinue: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_role_selection_title),
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.onboarding_role_selection_description),
            fontSize = 19.sp,
            lineHeight = 26.sp,
        )
        RoleOptionCard(
            title = stringResource(R.string.onboarding_role_patient_title),
            description = stringResource(R.string.onboarding_role_patient_description),
            selected = selectedRole == DeviceRole.PATIENT,
            testTag = OnboardingTestTags.ROLE_PATIENT,
            onClick = { onSelectRole(DeviceRole.PATIENT) },
        )
        RoleOptionCard(
            title = stringResource(R.string.onboarding_role_caregiver_title),
            description = stringResource(R.string.onboarding_role_caregiver_description),
            selected = selectedRole == DeviceRole.CAREGIVER,
            testTag = OnboardingTestTags.ROLE_CAREGIVER,
            onClick = { onSelectRole(DeviceRole.CAREGIVER) },
        )
        AppButton(
            label = stringResource(R.string.onboarding_continue),
            onClick = onContinue,
            enabled = selectedRole != null,
            testTag = OnboardingTestTags.CONTINUE,
        )
    }
}

@Composable
private fun RoleOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 6.dp else 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, fontSize = 18.sp, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun PatientDetailsContent(
    uiState: OnboardingScreenState,
    viewModel: OnboardingViewModel,
    importBackupLauncher: ActivityResultLauncher<Array<String>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_patient_details_intro),
            fontSize = 19.sp,
            lineHeight = 26.sp,
        )
        BackupRestoreCard(importBackupLauncher = importBackupLauncher)
        AppCard {
            Text(
                text = stringResource(R.string.onboarding_patient_section_title),
                modifier = Modifier.testTag(OnboardingTestTags.PATIENT_DETAILS),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            OnboardingTextField(
                label = stringResource(R.string.onboarding_patient_name_label),
                value = uiState.patientName,
                onValueChange = { viewModel.updateField(OnboardingField.PATIENT_NAME, it) },
            )
            OnboardingTextField(
                label = stringResource(R.string.onboarding_patient_age_label),
                value = uiState.patientAge,
                keyboardType = KeyboardType.Number,
                onValueChange = { viewModel.updateField(OnboardingField.PATIENT_AGE, it) },
            )
            OnboardingTextField(
                label = stringResource(R.string.onboarding_patient_notes_label),
                value = uiState.patientNotes,
                singleLine = false,
                onValueChange = { viewModel.updateField(OnboardingField.PATIENT_NOTES, it) },
            )
        }
        AppCard {
            Text(
                text = stringResource(R.string.onboarding_caregiver_section_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            OnboardingTextField(
                label = stringResource(R.string.onboarding_caregiver_name_label),
                value = uiState.caregiverName,
                onValueChange = { viewModel.updateField(OnboardingField.CAREGIVER_NAME, it) },
            )
            OnboardingTextField(
                label = stringResource(R.string.onboarding_caregiver_phone_label),
                value = uiState.caregiverPhone,
                keyboardType = KeyboardType.Phone,
                visualTransformation = PhoneVisualTransformation(),
                onValueChange = { viewModel.updateField(OnboardingField.CAREGIVER_PHONE, it) },
            )
            OnboardingTextField(
                label = stringResource(R.string.onboarding_caregiver_relationship_label),
                value = uiState.caregiverRelationship,
                onValueChange = { viewModel.updateField(OnboardingField.CAREGIVER_RELATIONSHIP, it) },
            )
        }
        RemindersCard(
            remindersEnabled = uiState.remindersEnabled,
            onRemindersChanged = viewModel::setRemindersEnabled,
        )
        FinishSetupButton(
            isSaving = uiState.isSaving,
            onClick = viewModel::saveInitialData,
            label = stringResource(R.string.onboarding_finish_patient),
        )
        SetupFooterNote(
            text = stringResource(R.string.onboarding_footer_patient),
        )
    }
}

@Composable
private fun CaregiverDetailsContent(
    uiState: OnboardingScreenState,
    viewModel: OnboardingViewModel,
    importBackupLauncher: ActivityResultLauncher<Array<String>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.onboarding_caregiver_details_intro),
            fontSize = 19.sp,
            lineHeight = 26.sp,
        )
        BackupRestoreCard(importBackupLauncher = importBackupLauncher)
        AppCard {
            Text(
                text = stringResource(R.string.onboarding_caregiver_your_profile_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            OnboardingTextField(
                label = stringResource(R.string.onboarding_caregiver_your_name_label),
                value = uiState.caregiverName,
                onValueChange = { viewModel.updateField(OnboardingField.CAREGIVER_NAME, it) },
            )
        }
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = stringResource(R.string.onboarding_linking_title),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_linking_description),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.padding(start = 4.dp))
                Text(
                    text = stringResource(R.string.onboarding_linking_help),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        FinishSetupButton(
            isSaving = uiState.isSaving,
            label = stringResource(R.string.onboarding_finish_caregiver),
            onClick = viewModel::saveInitialData,
        )
        SetupFooterNote(
            text = stringResource(R.string.onboarding_footer_caregiver),
        )
    }
}

@Composable
private fun BackupRestoreCard(
    importBackupLauncher: ActivityResultLauncher<Array<String>>,
) {
    AppCard {
        Text(text = stringResource(R.string.onboarding_backup_title), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_backup_description),
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = {
                importBackupLauncher.launch(
                    arrayOf("application/zip", "application/octet-stream"),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_backup_button), fontSize = 18.sp)
        }
    }
}

@Composable
private fun RemindersCard(
    remindersEnabled: Boolean,
    onRemindersChanged: (Boolean) -> Unit,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.onboarding_reminders_title), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = stringResource(R.string.onboarding_reminders_description),
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                )
            }
            Switch(
                checked = remindersEnabled,
                onCheckedChange = onRemindersChanged,
            )
        }
    }
}

@Composable
private fun FinishSetupButton(
    isSaving: Boolean,
    onClick: () -> Unit,
    label: String = stringResource(R.string.onboarding_finish_patient),
) {
    AppButton(
        label = if (isSaving) stringResource(R.string.onboarding_saving) else label,
        onClick = onClick,
        enabled = !isSaving,
    )
}

@Composable
private fun SetupFooterNote(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun OnboardingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
    )
}

@Composable
private fun OnboardingProgressDialog(text: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(text) },
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
