package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.util.formatScheduleTime

@Composable
fun ConfirmMedicationDialog(
    medications: List<MedicationEntity>,
    scheduleTime: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onRequestHelp: (() -> Unit)? = null,
) {
    val isMultiple = medications.size > 1

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isMultiple) "¿Ya tomaste estas pastillas?" else "¿Ya tomaste esta pastilla?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                scheduleTime?.let {
                    Text(
                        text = formatScheduleTime(it),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                medications.forEach { medication ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MedicationImagePreview(
                            imageUri = medication.imageUri,
                            label = medication.name,
                            size = 92.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = medication.name,
                                fontSize = 24.sp,
                                lineHeight = 30.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = medication.dose,
                                fontSize = 20.sp,
                                lineHeight = 26.sp,
                            )
                            medication.instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
                                Text(
                                    text = instructions,
                                    fontSize = 18.sp,
                                    lineHeight = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppButton(
                    label = if (isMultiple) "Sí, ya tomé todas" else "Sí, ya tomé",
                    onClick = onConfirm,
                    contentDescription = if (isMultiple) {
                        "Botón Sí, ya tomé todas"
                    } else {
                        "Botón Sí, ya tomé"
                    },
                )
                onRequestHelp?.let {
                    AppButton(
                        label = "Pedir ayuda",
                        onClick = it,
                        contentDescription = "Botón Pedir ayuda",
                    )
                }
            }
        },
        dismissButton = {
            AppButton(
                label = if (isMultiple) "No, todavía no" else "No, todavía no",
                onClick = onDismiss,
                contentDescription = "Botón No, todavía no",
            )
        },
    )
}
