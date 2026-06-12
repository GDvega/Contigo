package com.cuidavoz.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cuidavoz.mobile.ui.components.AppButton
import com.cuidavoz.mobile.ui.components.AppCard
import com.cuidavoz.mobile.ui.components.ToastMessageEffect
import com.cuidavoz.mobile.ui.viewmodel.ContactField
import com.cuidavoz.mobile.ui.viewmodel.SettingsViewModel
import com.cuidavoz.mobile.util.PhoneVisualTransformation

@Composable
fun FamilyContactScreen(
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ToastMessageEffect(
        message = uiState.message,
        onConsumed = viewModel::dismissMessage,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
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
            text = "Contacto familiar",
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Este contacto se usa para pedir ayuda, llamar rápidamente y avisar al cuidador cuando corresponda.",
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )

        AppCard {
            ContactTextField(
                label = "Nombre del familiar",
                value = uiState.familyName,
            ) { viewModel.updateContactField(ContactField.NAME, it) }
            ContactTextField(
                label = "Teléfono",
                value = uiState.familyPhone,
                keyboardType = KeyboardType.Phone,
                visualTransformation = PhoneVisualTransformation(),
            ) { viewModel.updateContactField(ContactField.PHONE, it) }
            ContactTextField(
                label = "Relación",
                value = uiState.familyRelationship,
            ) { viewModel.updateContactField(ContactField.RELATIONSHIP, it) }
            Spacer(modifier = Modifier.height(12.dp))
            AppButton(
                label = "Guardar contacto",
                onClick = viewModel::saveContact,
            )
        }
    }
}

@Composable
private fun ContactTextField(
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}
