package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.cuidavoz.mobile.R
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
    onCancel: () -> Unit,
) {
    AppCard {
        when (state.status) {
            VoiceAssistantStatus.Idle,
            VoiceAssistantStatus.Success -> {
                Text(
                    text = stringResource(R.string.voice_label_talk),
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
                    text = stringResource(state.assistantTitleResId),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = transcript ?: stringResource(state.assistantHintResId),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                if (state.status == VoiceAssistantStatus.Listening) {
                    Spacer(modifier = Modifier.height(16.dp))
                    VoiceWaveform(rmsLevel = state.audioLevel)
                }
                state.message
                    ?.takeIf { it.isNotBlank() && it != transcript }
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
                    label = stringResource(R.string.btn_cancel),
                    onClick = onCancel,
                    contentDescription = "Botón cancelar voz",
                    minHeight = 68.dp,
                    textSize = 24.sp,
                )
            }

            VoiceAssistantStatus.ErrorRecoverable -> {
                Text(
                    text = stringResource(R.string.voice_title_unknown),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                transcript?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.voice_hint_unknown),
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = stringResource(R.string.voice_btn_retry),
                    onClick = onRetry,
                    icon = Icons.Outlined.Mic,
                    contentDescription = "Botón intentar voz otra vez",
                    minHeight = 68.dp,
                    textSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = stringResource(R.string.voice_btn_fallback),
                    onClick = onUseButtons,
                    contentDescription = "Botón usar botones",
                    minHeight = 68.dp,
                    textSize = 24.sp,
                )
            }

            VoiceAssistantStatus.PermissionDenied,
            VoiceAssistantStatus.RecognizerUnavailable -> {
                Text(
                    text = stringResource(state.assistantTitleResId),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message ?: stringResource(state.assistantHintResId),
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
                        label = stringResource(R.string.voice_btn_fallback),
                        onClick = onUseButtons,
                        icon = Icons.Outlined.Mic,
                        contentDescription = "Botón usar botones",
                        minHeight = 68.dp,
                        textSize = 24.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceWaveform(rmsLevel: Float) {
    val bars = 5
    // Normalize RMS (-10 to 10 typical) to 0.1 to 1.0 range
    val normalized = ((rmsLevel + 10) / 20f).coerceIn(0.1f, 1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(bars) { index ->
            val heightMultiplier = when(index) {
                0, 4 -> 0.4f
                1, 3 -> 0.7f
                else -> 1.0f
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = 8.dp, height = (40.dp * normalized * heightMultiplier).coerceAtLeast(8.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
