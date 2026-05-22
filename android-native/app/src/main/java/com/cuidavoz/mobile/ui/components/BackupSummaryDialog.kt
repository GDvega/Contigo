package com.cuidavoz.mobile.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.cuidavoz.mobile.data.backup.BackupSummary
import com.cuidavoz.mobile.util.formatDateTime

@Composable
fun BackupSummaryDialog(
    summary: BackupSummary,
    onCancel: () -> Unit,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Este respaldo contiene:")
        },
        text = {
            Text(
                "Paciente: ${summary.patientName}\n" +
                    "Medicamentos: ${summary.medicationsCount}\n" +
                    "Presiones: ${summary.pressureReadingsCount}\n" +
                    "Registros de pastillas: ${summary.medicationLogsCount}\n" +
                    "Imágenes: ${summary.imagesCount}\n" +
                    "Fecha: ${formatDateTime(summary.createdAt)}\n" +
                    "Versión: ${summary.backupVersion}\n\n" +
                    "¿Cómo deseas recuperar la copia?",
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onMerge) {
                    Text("Unir con lo actual")
                }
                TextButton(onClick = onReplace) {
                    Text("Reemplazar todo")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        },
    )
}
