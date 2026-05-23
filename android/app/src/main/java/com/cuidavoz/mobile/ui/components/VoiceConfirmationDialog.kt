package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.ui.viewmodel.VoiceConfirmation
import com.cuidavoz.mobile.util.formatScheduleTime

@Composable
fun VoiceConfirmationDialog(
    confirmation: VoiceConfirmation,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val (title, body, confirmLabel) = when (confirmation) {
        is VoiceConfirmation.Pressure -> Triple(
            "Confirmar presión",
            buildString {
                append("Te escuché: presión ${confirmation.systolic} sobre ${confirmation.diastolic}")
                confirmation.pulse?.let { pulse -> append(" con pulso $pulse") }
                append(". ¿Deseas guardarla?")
            },
            "Sí, guardar",
        )

        is VoiceConfirmation.Medication -> {
            if (confirmation.medication != null) {
                Triple(
                    "Confirmar pastilla",
                    "¿Confirmas que ya tomaste ${confirmation.medication.name}?",
                    "Sí, registrar",
                )
            } else {
                Triple(
                    "Confirmar pastillas",
                    "¿Confirmas que ya tomaste tus pastillas de las ${formatScheduleTime(confirmation.scheduleTime)}?",
                    "Sí, registrar",
                )
            }
        }

        is VoiceConfirmation.Help -> Triple(
            "Pedir ayuda",
            "¿Deseas llamar a ${confirmation.contact.fullName}?",
            "Abrir llamada",
        )
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = body,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(12.dp))
                Text(
                    text = "También puedes responder con tu voz: sí o no.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
            }
        },
        confirmButton = {
            AppButton(
                label = confirmLabel,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            AppButton(
                label = "No",
                onClick = onCancel,
            )
        },
    )
}
