package com.cuidavoz.mobile.ui.screens.caregiver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.viewmodel.CaregiverDashboardUiState

@Composable
fun LinkCaregiverScreen(
    innerPadding: PaddingValues,
    uiState: CaregiverDashboardUiState,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit,
    onCreateCode: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onLinkWithCode: () -> Unit,
) {
    ToastMessageEffect(
        message = uiState.message,
        onConsumed = onDismissMessage,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FilledTonalButton(onClick = onBack, modifier = Modifier.height(56.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
            Text("Volver")
        }
        Text(
            text = "Vincular cuidador o paciente",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Estos datos de salud se compartirán con tu cuidador.",
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
        Text(
            text = "Puedes desactivar la sincronización cuando quieras.",
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
        AppCard {
            Text("En el celular del paciente", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Crea un código para que el cuidador vincule este paciente desde su propio celular.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Crear código",
                onClick = onCreateCode,
                minHeight = 60.dp,
                textSize = 22.sp,
            )
            uiState.createdLinkCode?.let { code ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Código", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(code, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold)
                Text("Vence en 10 minutos.", fontSize = 16.sp, lineHeight = 22.sp)
            }
        }
        AppCard {
            Text("En el celular del cuidador", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Escribe el código de 6 dígitos que ves en el celular del paciente.",
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.linkCodeInput,
                onValueChange = onCodeChanged,
                label = { Text("Código") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Vincular paciente",
                onClick = onLinkWithCode,
                minHeight = 60.dp,
                textSize = 22.sp,
            )
        }
        if (uiState.isWorking) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator()
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
