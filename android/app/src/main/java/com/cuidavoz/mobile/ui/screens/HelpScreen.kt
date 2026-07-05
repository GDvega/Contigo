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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Call
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
import androidx.core.net.toUri
import com.cuidavoz.mobile.data.model.FamilyContactEntity
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard

import androidx.compose.ui.res.stringResource
import com.cuidavoz.mobile.R

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
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.help_btn_back))
                Spacer(modifier = Modifier.height(0.dp))
                Text(stringResource(R.string.help_btn_back))
            }
            FilledTonalButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = stringResource(R.string.help_btn_speak))
                Text(stringResource(R.string.help_btn_speak))
            }
        }

        Text(
            text = stringResource(R.string.help_title),
            fontSize = 32.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
        )

        if (contact == null) {
            AppCard {
                Text(
                    text = stringResource(R.string.help_no_contact),
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppButton(
                    label = stringResource(R.string.help_btn_go_settings),
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
                label = stringResource(R.string.help_btn_call),
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, "tel:${contact.phone.replace(" ", "")}".toUri()),
                    )
                },
                icon = Icons.Outlined.Call,
                contentDescription = "Botón Llamar",
            )
            AppButton(
                label = stringResource(R.string.help_btn_send_message),
                onClick = {
                    val smsBody = context.getString(R.string.help_sms_body)
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "smsto:${contact.phone.replace(" ", "")}".toUri()
                        putExtra("sms_body", smsBody)
                    }
                    context.startActivity(intent)
                },
                icon = Icons.AutoMirrored.Outlined.Message,
                contentDescription = "Botón Enviar mensaje",
            )
            AppButton(
                label = stringResource(R.string.help_btn_cancel),
                onClick = onBack,
                contentDescription = "Botón Cancelar",
            )
        }
        Spacer(modifier = Modifier.height(120.dp))
    }
}
