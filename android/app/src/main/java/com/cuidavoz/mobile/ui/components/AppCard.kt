package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import com.cuidavoz.mobile.ui.theme.ContigoTheme

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit,
) {
    val dimensions = ContigoTheme.dimensions
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(dimensions.cardPadding),
        ) {
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppCardPreview() {
    ContigoTheme {
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(16.dp)) {
            AppCard {
                Text("Título de la Tarjeta", style = MaterialTheme.typography.headlineSmall)
                Text("Este es un ejemplo de contenido dentro de la tarjeta AppCard.")
            }
        }
    }
}
