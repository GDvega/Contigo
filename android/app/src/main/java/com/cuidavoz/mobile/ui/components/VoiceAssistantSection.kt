package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantStatus
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantUiState

@Composable
fun VoiceAssistantSection(
    state: VoiceAssistantUiState,
    transcript: String?,
    onButtonPressed: () -> Unit,
    onPermissionAlreadyGranted: () -> Unit,
    onPermissionRequestStarted: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onUseButtons: () -> Unit,
    onMedicationTaken: (() -> Unit)?,
    onMeasurePressure: () -> Unit,
    onAskHelp: () -> Unit,
    onCancel: () -> Unit,
) {
    AppCard {
        when (state.status) {
            VoiceAssistantStatus.Idle,
            VoiceAssistantStatus.Success -> {
                Text(
                    text = "Hablar",
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                VoiceAssistantButton(
                    state = state,
                    onButtonPressed = onButtonPressed,
                    onPermissionAlreadyGranted = onPermissionAlreadyGranted,
                    onPermissionRequestStarted = onPermissionRequestStarted,
                    onPermissionResult = onPermissionResult,
                )
            }

            VoiceAssistantStatus.RequestingPermission,
            VoiceAssistantStatus.Preparing,
            VoiceAssistantStatus.Listening,
            VoiceAssistantStatus.Speaking,
            VoiceAssistantStatus.Processing,
            VoiceAssistantStatus.ConfirmationRequired -> {
                Text(
                    text = when (state.status) {
                        VoiceAssistantStatus.Preparing -> "Preparando el micrófono..."
                        VoiceAssistantStatus.Listening -> "Te escucho"
                        VoiceAssistantStatus.Processing -> "Estoy revisando lo que dijiste."
                        VoiceAssistantStatus.ConfirmationRequired -> "Confirma tu respuesta"
                        VoiceAssistantStatus.RequestingPermission -> "Necesito permiso para escucharte."
                        else -> "Hablar"
                    },
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = transcript ?: state.assistantHint,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                state.message
                    ?.takeIf { it.isNotBlank() && it != state.assistantTitle && it != transcript }
                    ?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            fontSize = 18.sp,
                            lineHeight = 24.sp,
                        )
                    }
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Cancelar",
                    onClick = onCancel,
                    contentDescription = "Botón cancelar voz",
                    minHeight = 68.dp,
                    textSize = 24.sp,
                )
            }

            VoiceAssistantStatus.ErrorRecoverable -> {
                Text(
                    text = "No pude escucharte bien.",
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                transcript?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Te escuché: $it",
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Puedes intentar otra vez o usar los botones.",
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Intentar otra vez",
                    onClick = onRetry,
                    icon = Icons.Outlined.Mic,
                    contentDescription = "Botón intentar voz otra vez",
                    minHeight = 68.dp,
                    textSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Usar botones",
                    onClick = onUseButtons,
                    contentDescription = "Botón usar botones",
                    minHeight = 68.dp,
                    textSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                onMedicationTaken?.let {
                    AppButton(
                        label = "Ya tomé",
                        onClick = it,
                        icon = Icons.Outlined.LocalHospital,
                        contentDescription = "Botón ya tomé",
                        minHeight = 68.dp,
                        textSize = 24.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                QuickActionButton(
                    label = "Medir presión",
                    icon = Icons.Outlined.Favorite,
                    backgroundColor = Color(0xFFE3F5F2),
                    onClick = onMeasurePressure,
                    contentDescription = "Botón medir presión",
                )
                Spacer(modifier = Modifier.height(12.dp))
                QuickActionButton(
                    label = "Pedir ayuda",
                    icon = Icons.Outlined.Call,
                    backgroundColor = Color(0xFFFDE8EA),
                    onClick = onAskHelp,
                    contentDescription = "Botón pedir ayuda",
                )
            }

            VoiceAssistantStatus.PermissionDenied,
            VoiceAssistantStatus.RecognizerUnavailable -> {
                Text(
                    text = state.assistantTitle,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message ?: state.assistantHint,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (state.status == VoiceAssistantStatus.PermissionDenied) {
                    VoiceAssistantButton(
                        state = state,
                        onButtonPressed = onButtonPressed,
                        onPermissionAlreadyGranted = onPermissionAlreadyGranted,
                        onPermissionRequestStarted = onPermissionRequestStarted,
                        onPermissionResult = onPermissionResult,
                    )
                } else {
                    AppButton(
                        label = "Usar botones",
                        onClick = onUseButtons,
                        icon = Icons.Outlined.Mic,
                        contentDescription = "Botón usar botones",
                        minHeight = 68.dp,
                        textSize = 24.sp,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                onMedicationTaken?.let {
                    AppButton(
                        label = "Ya tomé",
                        onClick = it,
                        icon = Icons.Outlined.LocalHospital,
                        contentDescription = "Botón ya tomé",
                        minHeight = 68.dp,
                        textSize = 24.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                QuickActionButton(
                    label = "Medir presión",
                    icon = Icons.Outlined.Favorite,
                    backgroundColor = Color(0xFFE3F5F2),
                    onClick = onMeasurePressure,
                    contentDescription = "Botón medir presión",
                )
                Spacer(modifier = Modifier.height(12.dp))
                QuickActionButton(
                    label = "Pedir ayuda",
                    icon = Icons.Outlined.Call,
                    backgroundColor = Color(0xFFFDE8EA),
                    onClick = onAskHelp,
                    contentDescription = "Botón pedir ayuda",
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    contentDescription: String,
) {
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = MaterialTheme.shapes.large,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
        )
        Spacer(modifier = Modifier.height(0.dp))
        Text(
            text = label,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
