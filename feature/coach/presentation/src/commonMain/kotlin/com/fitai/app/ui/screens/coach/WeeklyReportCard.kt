package com.fitai.app.ui.screens.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fitai.app.coach.WeeklyHeadline
import com.fitai.app.coach.WeeklyReport
import com.fitai.app.coach.volumeChangePercent
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.model.WeeklyCoachReport
import com.fitai.app.stats.WeeklyStats
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.components.FitAIProGateCard
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun WeeklyReportCard(
    report: WeeklyReport?,
    loading: Boolean,
    error: Boolean,
    onRetry: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
            Text(
                text = strings.coachWeekTitle,
                style = MaterialTheme.typography.titleMedium,
                color = FitAIColors.OnSurface,
            )
            when {
                report != null -> ReportBody(report = report, onRetry = onRetry, onUpgrade = onUpgrade)
                error -> InlineRetry(message = strings.coachReportLoadError, onRetry = onRetry)
                loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = FitAIColors.Primary,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportBody(
    report: WeeklyReport,
    onRetry: () -> Unit,
    onUpgrade: () -> Unit,
) {
    val strings = LocalStrings.current
    StatsRow(report.stats)
    val coachReport = report.coachReport
    Text(
        text = coachReport?.headline?.takeIf { it.isNotBlank() } ?: report.headline.text(strings),
        style = MaterialTheme.typography.titleMedium,
        color = FitAIColors.OnSurface,
    )
    when {
        coachReport != null -> CoachReportDetails(coachReport)
        report.locked -> FitAIProGateCard(
            title = strings.coachReportLockedTitle,
            body = strings.coachReportLockedBody,
            ctaLabel = strings.upgradeToPro,
            onClick = onUpgrade,
            badgeLabel = strings.proBadge,
        )
        report.coachReportFailed -> InlineRetry(message = strings.coachReportFailed, onRetry = onRetry)
    }
}

@Composable
private fun StatsRow(stats: WeeklyStats) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        StatChip(strings.coachStatSessions, strings.coachStatSessionsValue(stats.sessionsCompleted, stats.sessionsPlanned))
        StatChip(strings.coachStatVolume, strings.coachStatVolumeValue(stats.totalVolumeKg.roundToInt(), stats.volumeChangePercent()))
        StatChip(strings.coachStatProtein, strings.coachStatProteinValue(stats.avgProteinG.roundToInt(), stats.proteinTargetG))
        StatChip(strings.coachStatWeight, strings.coachStatWeightValue(stats.weightChangeKg?.signedOneDecimal()))
        StatChip(strings.coachStatPrs, stats.prCount.toString())
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(FitAIColors.SurfaceVariant, FitAIShapes.button)
            .padding(horizontal = FitAISpacing.md, vertical = FitAISpacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = FitAIColors.OnSurface, maxLines = 1)
    }
}

@Composable
private fun CoachReportDetails(report: WeeklyCoachReport) {
    val strings = LocalStrings.current
    if (report.summary.isNotBlank()) {
        Text(text = report.summary, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
    }
    BulletSection(title = strings.coachReportHighlights, items = report.highlights)
    BulletSection(title = strings.coachReportNextWeek, items = report.nextWeekFocus)
}

@Composable
private fun BulletSection(title: String, items: List<String>) {
    val lines = items.filter { it.isNotBlank() }
    if (lines.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = FitAIColors.OnSurface)
        lines.forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .background(FitAIColors.OnSurfaceMuted, FitAIShapes.chip),
                )
                Text(text = line, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
            }
        }
    }
}

@Composable
internal fun InlineRetry(
    message: String,
    onRetry: () -> Unit,
    color: Color = FitAIColors.OnSurfaceMuted,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
        TextButton(onClick = onRetry) {
            Text(text = LocalStrings.current.actionRetry, color = FitAIColors.Primary)
        }
    }
}

internal fun WeeklyHeadline.text(strings: Strings): String = when (this) {
    WeeklyHeadline.NoActivity -> strings.coachHeadlineNoActivity
    is WeeklyHeadline.PlanCompleted -> strings.coachHeadlinePlanCompleted(sessions)
    is WeeklyHeadline.VolumeUp -> strings.coachHeadlineVolumeUp(percent)
    is WeeklyHeadline.ProteinLow -> strings.coachHeadlineProteinLow(avgProteinG, targetG)
    is WeeklyHeadline.InProgress -> strings.coachHeadlineInProgress(done, planned)
}

private fun Double.signedOneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    val sign = when {
        tenths > 0 -> "+"
        tenths < 0 -> "-"
        else -> ""
    }
    return "$sign${abs(tenths) / 10}.${abs(tenths) % 10}"
}
