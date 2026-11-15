package com.fitai.app.ui.plangeneration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fitai.app.access.PlanQuota
import com.fitai.app.access.PlanQuotaPolicy
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIQuotaChip
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.util.epochMillis

/** "2/3 hak kaldı" chip with a one-line explanation; switches to the exhausted copy at zero. */
@Composable
fun PlanQuotaRow(
    quota: PlanQuota,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val resetsAt = quota.resetsAtEpochMillis
    val caption = when {
        !quota.exhausted -> strings.planQuotaHint
        resetsAt != null -> strings.planQuotaExhausted + " " + strings.planQuotaResetsInDays(daysUntil(resetsAt))
        else -> strings.planQuotaExhausted
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FitAIQuotaChip(label = strings.planQuotaRemaining(quota.remaining, quota.limit), exhausted = quota.exhausted)
        Text(
            text = caption,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = FitAIColors.OnSurfaceMuted,
        )
    }
}

/** Asks before a regeneration spends a right; says how many are left afterwards. */
@Composable
fun PlanRegenerateConfirmDialog(
    quota: PlanQuota?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalStrings.current
    val limit = quota?.limit ?: PlanQuotaPolicy.LIMIT
    val remainingAfter = ((quota?.remaining ?: limit) - 1).coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FitAIColors.Surface,
        shape = FitAIShapes.card,
        icon = { Icon(FitAIIcons.AutoAwesome, contentDescription = null, tint = FitAIColors.Primary) },
        title = { Text(strings.planQuotaConfirmTitle, color = FitAIColors.OnSurface) },
        text = { Text(strings.planQuotaConfirmBody(remainingAfter, limit), color = FitAIColors.OnSurfaceMuted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(strings.planQuotaConfirmAction, color = FitAIColors.Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.actionCancel, color = FitAIColors.OnSurfaceMuted)
            }
        },
    )
}

private const val DAY_MILLIS = 86_400_000L

private fun daysUntil(resetsAtMillis: Long): Int =
    ((resetsAtMillis - epochMillis() + DAY_MILLIS - 1) / DAY_MILLIS).toInt().coerceAtLeast(1)
