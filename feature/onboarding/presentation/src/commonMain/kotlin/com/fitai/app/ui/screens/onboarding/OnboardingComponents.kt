package com.fitai.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing

/** Onboarding selection card: `Surface` + 2dp border, mint fill and border when selected. */
@Composable
internal fun SelectionCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    multiSelect: Boolean = false,
    showRadio: Boolean = true,
) {
    val borderColor by animateColorAsState(if (selected) FitAIColors.Primary else FitAIColors.SurfaceVariant)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = selected,
                onValueChange = { onClick() },
                role = if (multiSelect) Role.Checkbox else Role.RadioButton,
            ),
        shape = FitAIShapes.button,
        color = if (selected) FitAIColors.Primary.copy(alpha = 0.05f) else FitAIColors.Surface,
        border = BorderStroke(2.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            when {
                leading != null -> leading()
                icon != null -> IconWell(icon = icon, selected = selected)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) FitAIColors.Primary else FitAIColors.OnSurfaceMuted,
                )
            }
            if (showRadio) RadioMark(selected = selected, square = multiSelect)
        }
    }
}

@Composable
internal fun IconWell(
    icon: ImageVector,
    selected: Boolean,
    size: Dp = 48.dp,
    tint: Color? = null,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(
                if (selected) FitAIColors.Primary.copy(alpha = 0.2f) else FitAIColors.SurfaceVariant,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint ?: if (selected) FitAIColors.Primary else FitAIColors.OnSurface,
            modifier = Modifier.size(size / 2),
        )
    }
}

@Composable
internal fun RadioMark(selected: Boolean, square: Boolean = false) {
    val shape = if (square) RoundedCornerShape(6.dp) else CircleShape
    Surface(
        modifier = Modifier.size(24.dp),
        shape = shape,
        color = if (selected) FitAIColors.Primary else Color.Transparent,
        border = if (selected) null else BorderStroke(2.dp, FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f)),
    ) {
        if (selected) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = FitAIIcons.Check,
                    contentDescription = null,
                    tint = FitAIColors.OnPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/** Ascending bars; [level] of them are filled (0 = never trained). */
@Composable
internal fun SignalBars(level: Int, selected: Boolean, total: Int = 3) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(
                if (selected) FitAIColors.Primary.copy(alpha = 0.2f) else FitAIColors.SurfaceVariant,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(20.dp),
        ) {
            repeat(total) { index ->
                val filled = index < level
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((8 + index * 4).dp)
                        .background(
                            when {
                                filled && selected -> FitAIColors.Primary
                                filled -> FitAIColors.OnSurface
                                else -> FitAIColors.OnSurfaceMuted.copy(alpha = 0.3f)
                            },
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
    }
}

/** Muted card with icon rows, e.g. the privacy note under the weight picker. */
@Composable
internal fun InfoNoteCard(rows: List<Pair<ImageVector, String>>, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            rows.forEach { (icon, text) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
                ) {
                    Icon(icon, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(20.dp))
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
                }
            }
        }
    }
}
