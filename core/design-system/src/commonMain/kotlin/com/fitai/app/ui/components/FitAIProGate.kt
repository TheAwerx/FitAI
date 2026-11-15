package com.fitai.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing

/** Locked-feature card that routes to the paywall; pass `LocalStrings` text (e.g. `proBadge`) from the caller. */
@Composable
fun FitAIProGateCard(
    title: String,
    body: String,
    ctaLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeLabel: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = FitAIShapes.card,
        colors = CardDefaults.cardColors(containerColor = FitAIColors.Surface),
        border = BorderStroke(2.dp, FitAIColors.Primary.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Row(
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
                        imageVector = FitAIIcons.Lock,
                        contentDescription = null,
                        tint = FitAIColors.Primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
                ) {
                    if (badgeLabel != null) {
                        FitAIProBadge(label = badgeLabel)
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = FitAIColors.OnSurface,
                    )
                }
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = FitAIColors.OnSurfaceMuted,
            )
            FitAIButton(
                text = ctaLabel,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun FitAIProBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = FitAIColors.OnPrimary,
        modifier = modifier
            .background(FitAIColors.Primary, FitAIShapes.chip)
            .padding(horizontal = FitAISpacing.sm, vertical = 2.dp),
    )
}

/** Free-tier quota indicator, e.g. `strings.quotaRemaining(2, 3)`; [exhausted] switches to the warning tint. */
@Composable
fun FitAIQuotaChip(
    label: String,
    modifier: Modifier = Modifier,
    exhausted: Boolean = false,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (exhausted) FitAIColors.Warning else FitAIColors.OnSurfaceMuted,
        modifier = modifier
            .background(FitAIColors.SurfaceVariant, FitAIShapes.chip)
            .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.xs),
    )
}
