package com.fitai.app.ui.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing

/** Stateless Home entry point to [CoachScreen]. */
@Composable
fun CoachEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    FitAICard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(FitAIColors.Primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = FitAIIcons.TipsAndUpdates,
                    contentDescription = null,
                    tint = FitAIColors.Primary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
            ) {
                Text(
                    text = strings.coachTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = FitAIColors.OnSurface,
                )
                Text(
                    text = strings.coachEntryTeaser,
                    style = MaterialTheme.typography.bodySmall,
                    color = FitAIColors.OnSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = FitAIIcons.ChevronRight,
                contentDescription = null,
                tint = FitAIColors.OnSurfaceMuted,
            )
        }
    }
}
