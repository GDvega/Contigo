package com.cuidavoz.mobile.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantStatus
import com.cuidavoz.mobile.ui.viewmodel.VoiceAssistantUiState

@Composable
fun VoiceAssistantButton(
    state: VoiceAssistantUiState,
    onButtonPressed: () -> Unit,
    onPermissionAlreadyGranted: () -> Unit,
    onPermissionRequestStarted: () -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    label: String = state.statusLabel,
    icon: ImageVector? = null,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        onPermissionResult(granted)
    }

    AppButton(
        label = label,
        enabled = state.status != VoiceAssistantStatus.Listening &&
            state.status != VoiceAssistantStatus.Processing &&
            state.status != VoiceAssistantStatus.Preparing &&
            state.status != VoiceAssistantStatus.Speaking,
        icon = icon,
        onClick = {
            onButtonPressed()
            if (context.hasMicrophonePermission()) {
                onPermissionAlreadyGranted()
            } else {
                onPermissionRequestStarted()
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
    )
}

fun Context.hasMicrophonePermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO,
    ) == PackageManager.PERMISSION_GRANTED
}
