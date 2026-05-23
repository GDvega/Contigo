package com.cuidavoz.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.util.formatDateTime

data class PressureSavedData(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val measuredAt: Long,
    val statusText: String,
)

@Composable
fun PressureSavedScreen(
    innerPadding: PaddingValues,
    summary: PressureSavedData,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = "Presión guardada",
            tint = Color(0xFF1E8E3E),
            modifier = Modifier.height(120.dp),
        )
        Text(
            text = "¡Listo!",
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Tu presión fue guardada.",
            fontSize = 22.sp,
            lineHeight = 28.sp,
        )
        AppCard {
            Text(
                text = "${summary.systolic} / ${summary.diastolic} mmHg",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
            summary.pulse?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pulso: $it por min",
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatDateTime(summary.measuredAt),
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = summary.statusText,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AppButton(
            label = "Entendido",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Botón Entendido",
        )
    }
}
