package com.cuidavoz.mobile.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.data.backup.BackupCrypto
import com.cuidavoz.mobile.data.backup.ImportStrategy
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.BackupPasswordDialog
import com.cuidavoz.mobile.ui.components.BackupSummaryDialog
import com.cuidavoz.mobile.ui.viewmodel.BackupUiState
import com.cuidavoz.mobile.ui.viewmodel.BackupViewModel
import com.cuidavoz.mobile.R

@Composable
fun BackupScreen(
    innerPadding: PaddingValues,
    backupViewModel: BackupViewModel,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
) {
    val backupUiState by backupViewModel.uiState.collectAsStateWithLifecycle()
    var showReplaceConfirmation by remember { mutableStateOf(false) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf("") }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pendingImportPassword by remember { mutableStateOf("") }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val password = pendingExportPassword.toCharArray()
        pendingExportPassword = ""
        backupViewModel.exportBackup(uri, password)
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingImportUri = uri
        pendingImportPassword = ""
        showImportPasswordDialog = true
    }

    LaunchedEffect(backupUiState) {
        if (backupUiState !is BackupUiState.ImportPreview) {
            showReplaceConfirmation = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            FilledTonalButton(onClick = onBack, modifier = Modifier.height(56.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.caregiver_btn_back))
                Text(stringResource(R.string.caregiver_btn_back))
            }
        }

        Text(
            text = stringResource(R.string.backup_title),
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        AppCard {
            Text(
                text = stringResource(R.string.backup_save_section),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_save_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_save_pwd_help, BackupCrypto.MIN_PASSWORD_LENGTH),
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = stringResource(R.string.backup_btn_save),
                onClick = {
                    pendingExportPassword = ""
                    showExportPasswordDialog = true
                },
            )
        }

        AppCard {
            Text(
                text = stringResource(R.string.backup_restore_section),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_restore_desc),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_restore_pwd_help),
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = stringResource(R.string.backup_btn_restore),
                onClick = {
                    importBackupLauncher.launch(
                        arrayOf("application/zip", "application/octet-stream"),
                    )
                },
            )
        }
    }

    when (val state = backupUiState) {
        BackupUiState.Idle -> Unit
        BackupUiState.Exporting -> BackupProgressDialog(text = stringResource(R.string.backup_progress_creating))
        is BackupUiState.ExportSuccess -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text(stringResource(R.string.backup_success_title)) },
                text = {
                    val baseMsg = stringResource(
                        R.string.backup_success_msg,
                        state.result.exportedMedications,
                        state.result.exportedPressureReadings,
                        state.result.exportedMedicationLogs,
                        state.result.exportedImages
                    )
                    val warnings = if (state.result.warnings.isNotEmpty()) {
                        stringResource(R.string.backup_success_warnings, state.result.warnings.joinToString("\n"))
                    } else {
                        ""
                    }
                    Text(baseMsg + warnings)
                },
                confirmButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text(stringResource(R.string.btn_accept))
                    }
                },
            )
        }
        BackupUiState.ImportReading -> BackupProgressDialog(text = stringResource(R.string.dialog_import_reading))
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
                    stringResource(R.string.dialog_importing_restore)
                } else {
                    stringResource(R.string.dialog_importing_import)
                },
            )
        }
        is BackupUiState.ImportSuccess -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text(stringResource(R.string.backup_restore_success_title)) },
                text = {
                    val mainMsg = stringResource(
                        R.string.backup_restore_success_msg,
                        state.result.importedMedications,
                        state.result.importedPressureReadings,
                        state.result.importedMedicationLogs,
                        state.result.importedImages
                    )
                    val skippedMsg = if (state.result.skippedDuplicates > 0) {
                        stringResource(R.string.backup_restore_skipped_dupes, state.result.skippedDuplicates)
                    } else {
                        ""
                    }
                    val footer = if (state.result.errors.isNotEmpty()) {
                        state.result.errors.joinToString("\n")
                    } else {
                        stringResource(R.string.backup_restore_finished_ok)
                    }
                    Text("\n\n" + mainMsg + "\n\n" + skippedMsg + footer)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupViewModel.dismissState()
                            onOpenHome()
                        },
                    ) {
                        Text(stringResource(R.string.backup_btn_view_data))
                    }
                },
                dismissButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text(stringResource(R.string.backup_btn_close))
                    }
                },
            )
        }
        is BackupUiState.Error -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text(stringResource(R.string.backup_error_title)) },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text(stringResource(R.string.btn_accept))
                    }
                },
            )
        }
    }

    if (showExportPasswordDialog) {
        BackupPasswordDialog(
            title = stringResource(R.string.backup_export_pwd_title),
            description = stringResource(R.string.backup_export_pwd_desc, BackupCrypto.MIN_PASSWORD_LENGTH),
            confirmLabel = stringResource(R.string.backup_export_pwd_confirm),
            passwordRequired = true,
            password = pendingExportPassword,
            onPasswordChange = { pendingExportPassword = it },
            onConfirm = {
                showExportPasswordDialog = false
                createBackupLauncher.launch(backupViewModel.suggestedFileName())
            },
            onDismiss = {
                pendingExportPassword = ""
                showExportPasswordDialog = false
            },
        )
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

    if (showReplaceConfirmation && backupUiState is BackupUiState.ImportPreview) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirmation = false },
            title = { Text(stringResource(R.string.backup_replace_confirm_title)) },
            text = {
                Text(stringResource(R.string.backup_replace_confirm_msg))
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
