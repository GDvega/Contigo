package com.cuidavoz.mobile.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    testTag: String? = null,
    minHeight: androidx.compose.ui.unit.Dp? = null,
    textSize: TextUnit? = null,
    containerColor: androidx.compose.ui.graphics.Color? = null,
    contentColor: androidx.compose.ui.graphics.Color? = null,
) {
    val dimensions = com.cuidavoz.mobile.ui.theme.ContigoTheme.dimensions
    val finalMinHeight = minHeight ?: dimensions.buttonMinHeight
    val finalTextSize = textSize ?: MaterialTheme.typography.labelLarge.fontSize

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = finalMinHeight)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.primary,
            contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(dimensions.iconSize),
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
        }
        Text(
            text = label,
            fontSize = finalTextSize,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
