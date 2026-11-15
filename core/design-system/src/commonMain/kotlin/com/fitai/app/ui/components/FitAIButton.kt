package com.fitai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing

enum class FitAIButtonVariant { Primary, Secondary, Ghost }

@Composable
fun FitAIButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: FitAIButtonVariant = FitAIButtonVariant.Primary,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    val colors = when (variant) {
        FitAIButtonVariant.Primary -> ButtonDefaults.buttonColors(
            containerColor = FitAIColors.Primary,
            contentColor = FitAIColors.OnPrimary,
            disabledContainerColor = FitAIColors.SurfaceVariant,
            disabledContentColor = FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f),
        )
        FitAIButtonVariant.Secondary -> ButtonDefaults.buttonColors(
            containerColor = FitAIColors.SurfaceVariant,
            contentColor = FitAIColors.OnSurface,
            disabledContainerColor = FitAIColors.SurfaceVariant,
            disabledContentColor = FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f),
        )
        FitAIButtonVariant.Ghost -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = FitAIColors.PrimaryLight,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = FitAIColors.OnSurfaceMuted,
        )
    }
    val padding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
    val content: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(FitAISpacing.sm))
            }
            Text(text = text, style = MaterialTheme.typography.titleMedium)
            if (trailingIcon != null) {
                Spacer(Modifier.width(FitAISpacing.sm))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
    when (variant) {
        FitAIButtonVariant.Primary, FitAIButtonVariant.Secondary -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = FitAIShapes.button,
                colors = colors,
                contentPadding = padding,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (variant == FitAIButtonVariant.Primary) 0.dp else 0.dp,
                ),
            ) {
                content()
            }
        }
        FitAIButtonVariant.Ghost -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = FitAIShapes.button,
                colors = colors,
                contentPadding = padding,
            ) {
                content()
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
