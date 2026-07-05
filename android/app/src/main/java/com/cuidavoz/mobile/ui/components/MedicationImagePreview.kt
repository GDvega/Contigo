package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.tooling.preview.Preview
import com.cuidavoz.mobile.ui.theme.ContigoTheme

@Composable
fun MedicationImagePreview(
    imageUri: String?,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val extraColors = ContigoTheme.extraColors
    if (imageUri.isNullOrBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(20.dp))
                .background(extraColors.statusBackground.copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalHospital,
                    contentDescription = "Sin foto de $label",
                    tint = extraColors.statusText.copy(alpha = 0.6f * alpha),
                    modifier = Modifier.size(if (size >= 96.dp) 44.dp else 28.dp),
                )
                Text(
                    text = "Sin foto",
                    fontSize = if (size >= 96.dp) 14.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = extraColors.statusText.copy(alpha = 0.6f * alpha),
                )
            }
        }
        return
    }

    AsyncImage(
        model = imageUri,
        contentDescription = "Foto de $label",
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(20.dp))
            .then(if (alpha < 1f) Modifier.alpha(alpha) else Modifier),
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun MedicationImageGroupPreview(
    imageUris: List<String>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3,
) {
    val visible = imageUris.take(maxVisible)
    val overflowCount = (imageUris.size - visible.size).coerceAtLeast(0)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEachIndexed { index, imageUri ->
            MedicationImagePreview(
                imageUri = imageUri,
                label = labels.getOrNull(index).orEmpty().ifBlank { "Pastilla" },
                size = 56.dp,
            )
        }
        if (overflowCount > 0) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$overflowCount",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedicationImagePreviewEmptyPreview() {
    ContigoTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            MedicationImagePreview(imageUri = null, label = "Paracetamol", size = 88.dp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MedicationImageGroupPreviewPreview() {
    ContigoTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            MedicationImageGroupPreview(
                imageUris = listOf("", "", "", ""),
                labels = listOf("P1", "P2", "P3", "P4")
            )
        }
    }
}
