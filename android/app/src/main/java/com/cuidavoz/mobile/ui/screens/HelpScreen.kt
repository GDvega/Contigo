package com.cuidavoz.mobile.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard

@Composable
fun HelpScreen(
    innerPadding: PaddingValues,
    contact: FamilyContactEntity?,
    onBack: () -> Unit,
    onOpenCaregiverArea: () -> Unit,
    onSpeak: () -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .safeDrawingPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledTonalButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                Spacer(modifier = Modifier.height(0.dp))
                Text("Volver")
            }
            FilledTonalButton(onClick = onSpeak) {
                Icon(Icons.Outlined.VolumeUp, contentDescription = "Escuchar")
                Text("Escuchar")
            }
        }

        Text(
            text = "¿Quieres pedir ayuda?",
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )

        if (contact == null) {
            AppCard {
                Text(
                    text = "Agrega un familiar en Ajustes.",
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = "Ir a Familiar / Ajustes",
                    onClick = onOpenCaregiverArea,
                    contentDescription = "Botón Ir a Familiar Ajustes",
                )
            }
        } else {
            AppCard {
                Text(
                    text = contact.fullName,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                contact.relationship?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = contact.phone,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AppButton(
                label = "Llamar",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone.replace(" ", "")}")),
                    )
                },
                icon = Icons.Outlined.Call,
                contentDescription = "Botón Llamar",
            )
            AppButton(
                label = "Enviar mensaje",
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${contact.phone.replace(" ", "")}")
                        putExtra("sms_body", "Necesito ayuda. Por favor llámame.")
                    }
                    context.startActivity(intent)
                },
                icon = Icons.Outlined.Message,
                contentDescription = "Botón Enviar mensaje",
            )
            AppButton(
                label = "Cancelar",
                onClick = onBack,
                contentDescription = "Botón Cancelar",
            )
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}
