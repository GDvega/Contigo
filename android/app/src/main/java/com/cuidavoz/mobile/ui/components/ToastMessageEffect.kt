package com.cuidavoz.mobile.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun ToastMessageEffect(
    message: String?,
    onConsumed: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(message) {
        if (message.isNullOrBlank()) {
            return@LaunchedEffect
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        onConsumed()
    }
}
