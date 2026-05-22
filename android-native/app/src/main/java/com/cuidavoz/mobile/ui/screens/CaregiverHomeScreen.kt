package com.cuidavoz.mobile.ui.screens

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

@Composable
fun CaregiverHomeScreen(
    innerPadding: PaddingValues,
    onOpenMedications: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenFamilyContact: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenBackup: () -> Unit,
    onBack: () -> Unit,
    onReturnPatient: () -> Unit,
) {
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
            FilledTonalButton(
                onClick = onBack,
                modifier = Modifier.height(56.dp),
            ) {
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
        Spacer(modifier = Modifier.height(4.dp))
        AppButton(
            label = "Volver al modo paciente",
            onClick = onReturnPatient,
            contentDescription = "Botón Volver al modo paciente",
            minHeight = 60.dp,
            textSize = 22.sp,
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
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppButton(
            label = title,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = "Botón $title",
            minHeight = 60.dp,
            textSize = 22.sp,
        )
    }
}
