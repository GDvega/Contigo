package com.cuidavoz.mobile.ui.navigation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.cuidavoz.mobile.R

@Composable
fun StartupPermissionEffect() {
    val context = LocalContext.current
    var runtimePermissionsRequested by rememberSaveable { mutableStateOf(false) }
    var runtimePermissionsHandled by rememberSaveable { mutableStateOf(false) }
    var unusedRestrictionsChecked by rememberSaveable { mutableStateOf(false) }
    var showUnusedRestrictionsDialog by rememberSaveable { mutableStateOf(false) }
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // Android owns the final grant/deny decision; the app re-checks permissions where needed.
        runtimePermissionsHandled = true
    }
    val unusedRestrictionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        showUnusedRestrictionsDialog = false
    }

    LaunchedEffect(Unit) {
        if (!runtimePermissionsRequested) {
            runtimePermissionsRequested = true
            val permissions = buildStartupRuntimePermissions(context)
            if (permissions.isNotEmpty()) {
                runtimePermissionLauncher.launch(permissions.toTypedArray())
            } else {
                runtimePermissionsHandled = true
            }
        }
    }

    LaunchedEffect(runtimePermissionsHandled) {
        if (runtimePermissionsHandled && !unusedRestrictionsChecked) {
            unusedRestrictionsChecked = true
            showUnusedRestrictionsDialog = context.areUnusedAppRestrictionsEnabled()
        }
    }

    if (showUnusedRestrictionsDialog) {
        AlertDialog(
            onDismissRequest = { showUnusedRestrictionsDialog = false },
            title = { Text(stringResource(R.string.startup_unused_restrictions_title)) },
            text = { Text(stringResource(R.string.startup_unused_restrictions_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        showUnusedRestrictionsDialog = false
                        unusedRestrictionsLauncher.launch(context.manageUnusedRestrictionsIntent())
                    },
                ) {
                    Text(stringResource(R.string.startup_unused_restrictions_open))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnusedRestrictionsDialog = false }) {
                    Text(stringResource(R.string.startup_unused_restrictions_later))
                }
            },
        )
    }
}

private fun buildStartupRuntimePermissions(context: Context): List<String> = buildList {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    ) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
    if (!context.hasPermission(Manifest.permission.RECORD_AUDIO)) {
        add(Manifest.permission.RECORD_AUDIO)
    }
}

private fun Context.areUnusedAppRestrictionsEnabled(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        !packageManager.isAutoRevokeWhitelisted
}

private fun Context.manageUnusedRestrictionsIntent(): Intent {
    return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
    }
}

private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
