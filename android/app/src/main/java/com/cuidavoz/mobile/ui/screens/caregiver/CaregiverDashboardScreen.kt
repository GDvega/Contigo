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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.viewmodel.CaregiverDashboardUiState

@Composable
fun CaregiverDashboardScreen(
    innerPadding: PaddingValues,
    uiState: CaregiverDashboardUiState,
    onDismissMessage: () -> Unit,
    onBack: () -> Unit,
    onOpenLinking: () -> Unit,
    onOpenMedications: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenFamilyContact: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onRetrySync: () -> Unit,
    onToggleSync: (Boolean) -> Unit,
    onCallPatient: () -> Unit,
    onReturnPatient: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            FilledTonalButton(onClick = onBack, modifier = Modifier.height(56.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                Text("Volver")
            }
        }
        Text(
            text = "Área para familiar o cuidador",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )
        AppCard {
            Text("Paciente", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.patientName, fontSize = 22.sp, lineHeight = 28.sp)
            Text("Última presión: ${uiState.latestPressure}", fontSize = 18.sp, lineHeight = 24.sp)
            Text("Estado: ${uiState.pressureStatus}", fontSize = 18.sp, lineHeight = 24.sp)
            Text("Pastillas pendientes hoy: ${uiState.pendingMedicationsToday}", fontSize = 18.sp, lineHeight = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Llamar paciente",
                onClick = onCallPatient,
                icon = Icons.Outlined.Phone,
                minHeight = 60.dp,
                textSize = 22.sp,
            )
        }
        AppCard {
            Text("Sincronización", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(uiState.syncStatus, fontSize = 18.sp, lineHeight = 24.sp)
            Text(
                if (uiState.familyLinked) {
                    "Este celular ya está vinculado a una familia."
                } else {
                    "Este celular todavía no está vinculado."
                },
                fontSize = 18.sp,
                lineHeight = 24.sp,
            )
            Text(
                "Si no hay internet, CuidaVoz seguirá funcionando en este celular.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Sincronización activa", fontSize = 18.sp, lineHeight = 24.sp)
                Switch(checked = uiState.syncEnabled, onCheckedChange = onToggleSync)
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Vincular paciente",
                onClick = onOpenLinking,
                icon = Icons.Outlined.Link,
                minHeight = 60.dp,
                textSize = 22.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            AppButton(
                label = "Sincronizar ahora",
                onClick = onRetrySync,
                minHeight = 60.dp,
                textSize = 22.sp,
            )
        }
        if (uiState.recentMedicationEvents.isNotEmpty()) {
            AppCard {
                Text("Últimas tomas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                uiState.recentMedicationEvents.forEach { item ->
                    Text(item, fontSize = 18.sp, lineHeight = 24.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
        CaregiverMenuCard(
            title = "Medicamentos",
            subtitle = "Agregar o cambiar pastillas",
            icon = Icons.Outlined.LocalHospital,
            iconTint = Color(0xFF4D8A4B),
            onClick = onOpenMedications,
        )
        CaregiverMenuCard(
            title = "Registros",
            subtitle = "Ver presión y tomas",
            icon = Icons.Outlined.History,
            iconTint = Color(0xFF0F6B6E),
            onClick = onOpenRecords,
        )
        CaregiverMenuCard(
            title = "Reporte médico",
            subtitle = "Crear reporte para el médico",
            icon = Icons.Outlined.Description,
            iconTint = Color(0xFF7A4BA3),
            onClick = onOpenReports,
        )
        CaregiverMenuCard(
            title = "Contacto familiar",
            subtitle = "Cambiar teléfono de ayuda",
            icon = Icons.Outlined.Favorite,
            iconTint = Color(0xFFC85A5A),
            onClick = onOpenFamilyContact,
        )
        CaregiverMenuCard(
            title = "Ajustes",
            subtitle = "Recordatorios, voz y rangos",
            icon = Icons.Outlined.Settings,
            iconTint = Color(0xFF5E6E8A),
            onClick = onOpenSettings,
        )
        CaregiverMenuCard(
            title = "Guardar copia",
            subtitle = "Guardar o recuperar datos",
            icon = Icons.Outlined.Lock,
            iconTint = Color(0xFF19857B),
            onClick = onOpenBackup,
        )
        AppButton(
            label = "Volver al modo paciente",
            onClick = onReturnPatient,
            minHeight = 60.dp,
            textSize = 22.sp,
            contentDescription = "Botón Volver al modo paciente",
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CaregiverMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
) {
    AppCard {
        Icon(imageVector = icon, contentDescription = title, tint = iconTint)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = subtitle, fontSize = 18.sp, lineHeight = 24.sp)
        Spacer(modifier = Modifier.height(12.dp))
        AppButton(
            label = title,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            minHeight = 60.dp,
            textSize = 22.sp,
            contentDescription = "Botón $title",
        )
    }
}
