package com.fitai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes

enum class FitAIButtonVariant { Primary, Secondary, Ghost }

@Composable
fun FitAIButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FitAIButtonVariant = FitAIButtonVariant.Primary,
    enabled: Boolean = true,
) {
    val colors = when (variant) {
        FitAIButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = FitAIColors.Primary,
            contentColor = FitAIColors.Background,
            disabledContainerColor = FitAIColors.SurfaceVariant,
            disabledContentColor = FitAIColors.OnSurfaceMuted,
        )
        FitAIButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = FitAIColors.SurfaceVariant,
            contentColor = FitAIColors.OnSurface,
            disabledContainerColor = FitAIColors.SurfaceVariant,
            disabledContentColor = FitAIColors.OnSurfaceMuted,
        )
        FitAIButtonVariant.Ghost -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = FitAIColors.PrimaryLight,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = FitAIColors.OnSurfaceMuted,
        )
    }
    when (variant) {
        FitAIButtonVariant.Primary, FitAIButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = FitAIShapes.button,
                colors = colors,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(text)
            }
        }
        FitAIButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = FitAIShapes.button,
                colors = colors,
            ) {
                Text(text)
            }
        }
    }
}

@Composable
fun FitAIIconButton(
    imageVector: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = FitAIColors.Primary,
        )
    }
}

@Composable
fun FitAIOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = FitAIShapes.button,
        border = BorderStroke(1.dp, FitAIColors.Primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = FitAIColors.Primary,
        ),
    ) {
        Text(text)
    }
}
