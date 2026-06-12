package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.data.model.MedicationEntity
import com.cuidavoz.mobile.domain.MedicationDoseOutcome
import com.cuidavoz.mobile.domain.MedicationDoseStatus
import com.cuidavoz.mobile.domain.MedicationSkipReason
import com.cuidavoz.mobile.domain.medicationSkipReasonLabel
import com.cuidavoz.mobile.util.formatScheduleTime

@Composable
fun ConfirmMedicationDialog(
    medications: List<MedicationEntity>,
    scheduleTime: String?,
    onSave: (List<MedicationDoseOutcome>) -> Unit,
    onDismiss: () -> Unit,
    onRequestHelp: (() -> Unit)? = null,
) {
    val isMultiple = medications.size > 1
    val selections = remember(medications) {
        mutableStateMapOf<String, MedicationDoseStatus>()
    }
    val skipReasons = remember(medications) {
        mutableStateMapOf<String, MedicationSkipReason>()
    }
    var pendingSkipMedication by remember(medications) {
        mutableStateOf<MedicationEntity?>(null)
    }
    val hasSelection = selections.isNotEmpty()

    pendingSkipMedication?.let { medication ->
        MedicationSkipReasonDialog(
            medicationName = medication.name,
            onReasonSelected = { reason ->
                if (isMultiple) {
                    selections[medication.id] = MedicationDoseStatus.SKIPPED
                    skipReasons[medication.id] = reason
                } else {
                    onSave(
                        listOf(
                            MedicationDoseOutcome(
                                medicationId = medication.id,
                                status = MedicationDoseStatus.SKIPPED,
                                skipReason = reason,
                            ),
                        ),
                    )
                }
                pendingSkipMedication = null
            },
            onDismiss = { pendingSkipMedication = null },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isMultiple) "Registrar tomas" else "¿Ya tomaste esta pastilla?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                scheduleTime?.let {
                    Text(
                        text = formatScheduleTime(it),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (isMultiple) {
                    Text(
                        text = "Marca cada pastilla que ya tomaste o indica por qué no pudiste tomarla.",
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                medications.forEach { medication ->
                    MedicationOutcomeRow(
                        medication = medication,
                        selectedStatus = selections[medication.id],
                        selectedSkipReason = skipReasons[medication.id],
                        showPerMedicationActions = isMultiple,
                        onSelectTaken = {
                            if (isMultiple) {
                                selections[medication.id] = MedicationDoseStatus.TAKEN
                                skipReasons.remove(medication.id)
                            } else {
                                onSave(
                                    listOf(
                                        MedicationDoseOutcome(
                                            medicationId = medication.id,
                                            status = MedicationDoseStatus.TAKEN,
                                        ),
                                    ),
                                )
                            }
                        },
                        onSelectSkipped = {
                            pendingSkipMedication = medication
                        },
                    )
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isMultiple) {
                    AppButton(
                        label = "Guardar",
                        onClick = {
                            onSave(buildOutcomes(selections, skipReasons))
                        },
                        enabled = hasSelection,
                        contentDescription = "Botón guardar tomas",
                    )
                } else {
                    AppButton(
                        label = "Sí, ya tomé",
                        onClick = {
                            val medication = medications.first()
                            onSave(
                                listOf(
                                    MedicationDoseOutcome(
                                        medicationId = medication.id,
                                        status = MedicationDoseStatus.TAKEN,
                                    ),
                                ),
                            )
                        },
                        contentDescription = "Botón Sí, ya tomé",
                    )
                    AppButton(
                        label = "No pude tomarla",
                        onClick = {
                            pendingSkipMedication = medications.first()
                        },
                        contentDescription = "Botón No pude tomarla",
                    )
                }
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
                label = if (isMultiple) "Cancelar" else "No, todavía no",
                onClick = onDismiss,
                contentDescription = if (isMultiple) {
                    "Botón cancelar"
                } else {
                    "Botón No, todavía no"
                },
            )
        },
    )
}

@Composable
private fun MedicationSkipReasonDialog(
    medicationName: String,
    onReasonSelected: (MedicationSkipReason) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "¿Por qué no tomaste $medicationName?",
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MedicationSkipReason.entries.forEach { reason ->
                    AppButton(
                        label = reason.displayLabel(),
                        onClick = { onReasonSelected(reason) },
                        contentDescription = "Motivo ${reason.displayLabel()}",
                        minHeight = 56.dp,
                        textSize = 20.sp,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            AppButton(
                label = "Cancelar",
                onClick = onDismiss,
                contentDescription = "Botón cancelar motivo",
            )
        },
    )
}

@Composable
private fun MedicationOutcomeRow(
    medication: MedicationEntity,
    selectedStatus: MedicationDoseStatus?,
    selectedSkipReason: MedicationSkipReason?,
    showPerMedicationActions: Boolean,
    onSelectTaken: () -> Unit,
    onSelectSkipped: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MedicationImagePreview(
                imageUri = medication.imageUri,
                label = medication.name,
                size = if (showPerMedicationActions) 72.dp else 92.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = medication.name,
                    fontSize = if (showPerMedicationActions) 22.sp else 24.sp,
                    lineHeight = 28.sp,
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
                if (showPerMedicationActions) {
                    selectedStatus?.let { status ->
                        val statusText = when (status) {
                            MedicationDoseStatus.TAKEN -> "Marcada como tomada"
                            MedicationDoseStatus.SKIPPED -> {
                                val reasonLabel = selectedSkipReason?.displayLabel()
                                if (reasonLabel != null) {
                                    "No tomada · $reasonLabel"
                                } else {
                                    "Marcada como no tomada"
                                }
                            }
                        }
                        Text(
                            text = statusText,
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (showPerMedicationActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppButton(
                    label = "Ya tomé",
                    onClick = onSelectTaken,
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp,
                    textSize = 20.sp,
                    containerColor = if (selectedStatus == MedicationDoseStatus.TAKEN) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        null
                    },
                    contentColor = if (selectedStatus == MedicationDoseStatus.TAKEN) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        null
                    },
                    contentDescription = "Botón ya tomé ${medication.name}",
                )
                AppButton(
                    label = "No pude",
                    onClick = onSelectSkipped,
                    modifier = Modifier.weight(1f),
                    minHeight = 56.dp,
                    textSize = 20.sp,
                    containerColor = if (selectedStatus == MedicationDoseStatus.SKIPPED) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        null
                    },
                    contentDescription = "Botón no pude ${medication.name}",
                )
            }
        }
    }
}

private fun buildOutcomes(
    selections: Map<String, MedicationDoseStatus>,
    skipReasons: Map<String, MedicationSkipReason>,
): List<MedicationDoseOutcome> {
    return selections.map { (medicationId, status) ->
        MedicationDoseOutcome(
            medicationId = medicationId,
            status = status,
            skipReason = if (status == MedicationDoseStatus.SKIPPED) {
                skipReasons[medicationId] ?: MedicationSkipReason.OTHER
            } else {
                null
            },
        )
    }
}
