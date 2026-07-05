package com.cuidavoz.mobile.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.cuidavoz.mobile.R
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
            Text(stringResource(R.string.backup_summary_title))
        },
        text = {
            Text(
                stringResource(
                    R.string.backup_summary_content,
                    summary.patientName,
                    summary.medicationsCount,
                    summary.pressureReadingsCount,
                    summary.medicationLogsCount,
                    summary.imagesCount,
                    formatDateTime(summary.createdAt),
                    summary.backupVersion
                ),
            )
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onMerge) {
                    Text(stringResource(R.string.backup_btn_merge))
                }
                TextButton(onClick = onReplace) {
                    Text(stringResource(R.string.backup_btn_replace))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
    )
}
