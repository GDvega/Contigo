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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                Text("Volver")
            }
        }

        Text(
            text = "Copia de seguridad",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        AppCard {
            Text(
                text = "Guardar datos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Crea un archivo cifrado con paciente, contacto, medicamentos, presiones, registros, ajustes e imágenes.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Elige una contraseña segura (mínimo ${BackupCrypto.MIN_PASSWORD_LENGTH} caracteres). Sin ella no podrás abrir la copia.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Guardar copia",
                onClick = {
                    pendingExportPassword = ""
                    showExportPasswordDialog = true
                },
            )
        }

        AppCard {
            Text(
                text = "Recuperar datos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Importa un respaldo cifrado de Contigo. Podrás unirlo con lo actual o reemplazar los datos de este celular.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Necesitarás la contraseña del respaldo. Los respaldos antiguos sin cifrado pueden dejarse vacíos.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Recuperar copia",
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
        BackupUiState.Exporting -> BackupProgressDialog(text = "Creando respaldo...")
        is BackupUiState.ExportSuccess -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text("Respaldo creado") },
                text = {
                    Text(
                        "Respaldo creado correctamente.\n\n" +
                            "Medicamentos: ${state.result.exportedMedications}\n" +
                            "Presiones: ${state.result.exportedPressureReadings}\n" +
                            "Registros de pastillas: ${state.result.exportedMedicationLogs}\n" +
                            "Imagenes: ${state.result.exportedImages}" +
                            if (state.result.warnings.isNotEmpty()) {
                                "\n\nAvisos:\n${state.result.warnings.joinToString("\n")}"
                            } else {
                                ""
                            },
                    )
                },
                confirmButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text("Aceptar")
                    }
                },
            )
        }
        BackupUiState.ImportReading -> BackupProgressDialog(text = "Leyendo respaldo...")
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
                    "Restaurando datos..."
                } else {
                    "Importando datos..."
                },
            )
        }
        is BackupUiState.ImportSuccess -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text("Importación completada") },
                text = {
                    Text(
                        "Se importaron ${state.result.importedMedications} medicamentos, " +
                            "${state.result.importedPressureReadings} presiones, " +
                            "${state.result.importedMedicationLogs} registros de pastillas " +
                            "y ${state.result.importedImages} imágenes.\n\n" +
                            if (state.result.skippedDuplicates > 0) {
                                "Duplicados omitidos: ${state.result.skippedDuplicates}\n\n"
                            } else {
                                ""
                            } +
                            if (state.result.errors.isNotEmpty()) {
                                state.result.errors.joinToString("\n")
                            } else {
                                "Datos restaurados correctamente."
                            },
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            backupViewModel.dismissState()
                            onOpenHome()
                        },
                    ) {
                        Text("Ver mis datos")
                    }
                },
                dismissButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text("Cerrar")
                    }
                },
            )
        }
        is BackupUiState.Error -> {
            AlertDialog(
                onDismissRequest = backupViewModel::dismissState,
                title = { Text("No pudimos continuar") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = backupViewModel::dismissState) {
                        Text("Aceptar")
                    }
                },
            )
        }
    }

    if (showExportPasswordDialog) {
        BackupPasswordDialog(
            title = "Proteger respaldo",
            description = "Elige una contraseña para cifrar la copia. Mínimo ${BackupCrypto.MIN_PASSWORD_LENGTH} caracteres.",
            confirmLabel = "Elegir ubicación",
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
            title = "Abrir respaldo",
            description = "Escribe la contraseña del respaldo. Déjala vacía solo si es un respaldo antiguo sin cifrado.",
            confirmLabel = "Continuar",
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
            title = { Text("Confirmar reemplazo") },
            text = {
                Text("Esto cambiará tus datos. Borrará lo actual de este celular y pondrá la copia guardada.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirmation = false
                        backupViewModel.importBackup(ImportStrategy.REPLACE_ALL)
                    },
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmation = false }) {
                    Text("Cancelar")
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
