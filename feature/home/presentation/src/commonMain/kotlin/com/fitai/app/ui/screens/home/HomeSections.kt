package com.fitai.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fitai.app.engagement.LevelProgress
import com.fitai.app.engagement.StreakStatus
import com.fitai.app.localization.LocalStrings
import com.fitai.app.nutrition.TodayNutritionOverview
import com.fitai.app.stats.WeeklyStats
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAICard
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.home.formatOneDecimal
import kotlin.math.roundToInt

@Composable
internal fun EngagementProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(FitAIColors.SurfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(FitAIColors.Primary),
        )
    }
}

/** Brand title with the workout streak flame (opens the streak screen) and the PRO badge. */
@Composable
internal fun HomeHeader(streakDays: Int, trainedToday: Boolean, isPro: Boolean, onStreakClick: () -> Unit) {
    val strings = LocalStrings.current
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(FitAIIcons.FitnessCenter, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(26.dp))
        Text(
            text = strings.appName,
            style = MaterialTheme.typography.headlineMedium,
            color = FitAIColors.OnSurface,
            modifier = Modifier.padding(start = FitAISpacing.xs).weight(1f),
        )
        if (isPro) {
            Text(
                text = strings.proBadge,
                style = MaterialTheme.typography.labelLarge,
                color = FitAIColors.OnPrimary,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(FitAIColors.Primary)
                    .padding(horizontal = FitAISpacing.sm + FitAISpacing.xs, vertical = 2.dp),
            )
        }
        Row(
            modifier = Modifier
                .padding(start = FitAISpacing.sm)
                .clip(CircleShape)
                .clickable(onClick = onStreakClick)
                .background(FitAIColors.Surface)
                .border(1.dp, if (streakDays > 0) FitAIColors.Accent.copy(alpha = 0.5f) else FitAIColors.SurfaceVariant, CircleShape)
                .padding(horizontal = FitAISpacing.sm + FitAISpacing.xs, vertical = FitAISpacing.xs)
                .semantics { contentDescription = strings.streakA11y(streakDays) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
        ) {
            Icon(
                imageVector = FitAIIcons.LocalFireDepartment,
                contentDescription = null,
                tint = if (trainedToday || streakDays > 0) FitAIColors.Accent else FitAIColors.OnSurfaceMuted,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = streakDays.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (streakDays > 0) FitAIColors.Accent else FitAIColors.OnSurfaceMuted,
            )
        }
    }
}

@Composable
internal fun StreakXpRow(streak: StreakStatus, weeklyXp: Int, level: LevelProgress) {
    val strings = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        FitAICard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = FitAIIcons.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (streak.currentDays > 0) FitAIColors.Accent else FitAIColors.OnSurfaceMuted,
                    modifier = Modifier.size(18.dp),
                )
                Text(strings.engagementStreakLabel, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
            }
            Text(
                text = strings.engagementStreakDays(streak.currentDays),
                style = MaterialTheme.typography.titleLarge,
                color = FitAIColors.OnSurface,
            )
            Text(
                text = when {
                    streak.todayActive -> strings.engagementTodayActive
                    streak.freezeUsedThisWeek -> strings.engagementFreezeUsed
                    else -> strings.engagementFreezeAvailable
                },
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.OnSurfaceMuted,
            )
        }
        FitAICard(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Text(strings.engagementLevel(level.level), style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
            Text(
                text = strings.engagementWeeklyXp(weeklyXp),
                style = MaterialTheme.typography.titleMedium,
                color = FitAIColors.OnSurface,
            )
            EngagementProgressBar(level.progress, Modifier.padding(vertical = FitAISpacing.xs))
            Text(
                text = strings.engagementXpToNext(level.xpForNextLevel - level.xpIntoLevel),
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.OnSurfaceMuted,
            )
        }
    }
}

@Composable
internal fun RemainingMacrosCard(nutrition: TodayNutritionOverview?, onLogMeal: () -> Unit) {
    val strings = LocalStrings.current
    val targets = nutrition?.targets
    val remaining = nutrition?.remaining
    FitAICard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings.nutritionRemainingTitle,
                style = MaterialTheme.typography.titleMedium,
                color = FitAIColors.OnSurface,
                modifier = Modifier.weight(1f),
            )
            if (targets != null) {
                Text(
                    text = if (targets.isTrainingDay) strings.nutritionTrainingDayTarget else strings.nutritionRestDayTarget,
                    style = MaterialTheme.typography.labelSmall,
                    color = FitAIColors.OnSurfaceMuted,
                )
            }
        }
        if (targets != null && remaining != null) {
            Text(
                text = strings.nutritionKcalLeft(remaining.calories),
                style = MaterialTheme.typography.titleLarge,
                color = FitAIColors.OnSurface,
                modifier = Modifier.padding(top = FitAISpacing.sm),
            )
            val consumed = nutrition.consumed.calories
            EngagementProgressBar(
                progress = if (targets.calories > 0) consumed.toFloat() / targets.calories else 0f,
                modifier = Modifier.padding(vertical = FitAISpacing.sm),
            )
            Text(
                text = strings.homeMacrosLeft(
                    remaining.proteinG.roundToInt(),
                    remaining.carbsG.roundToInt(),
                    remaining.fatG.roundToInt(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = FitAIColors.OnSurfaceMuted,
            )
        } else {
            Text(
                text = strings.homeNoDietPlan,
                style = MaterialTheme.typography.bodySmall,
                color = FitAIColors.OnSurfaceMuted,
                modifier = Modifier.padding(vertical = FitAISpacing.sm),
            )
        }
        FitAIButton(
            text = strings.homeQuickLogMeal,
            onClick = onLogMeal,
            modifier = Modifier.fillMaxWidth().padding(top = FitAISpacing.md),
        )
    }
}

@Composable
internal fun WeeklySummaryCard(stats: WeeklyStats) {
    val strings = LocalStrings.current
    FitAICard(modifier = Modifier.fillMaxWidth()) {
        Text(strings.homeWeekTitle, style = MaterialTheme.typography.titleMedium, color = FitAIColors.OnSurface)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = FitAISpacing.md),
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            WeekStat(
                value = strings.coachStatSessionsValue(stats.sessionsCompleted, stats.sessionsPlanned),
                label = strings.coachStatSessions,
                modifier = Modifier.weight(1f),
            )
            WeekStat(
                value = strings.homeVolumeTonnes(formatOneDecimal(stats.totalVolumeKg / 1000.0)),
                label = strings.coachStatVolume,
                modifier = Modifier.weight(1f),
            )
            WeekStat(
                value = stats.prCount.toString(),
                label = strings.coachStatPrs,
                modifier = Modifier.weight(1f),
            )
        }
        if (stats.previousWeekVolumeKg > 0.0) {
            val percent = ((stats.totalVolumeKg - stats.previousWeekVolumeKg) / stats.previousWeekVolumeKg * 100).roundToInt()
            Text(
                text = strings.homeVolumeChange(percent),
                style = MaterialTheme.typography.labelSmall,
                color = FitAIColors.OnSurfaceMuted,
                modifier = Modifier.padding(top = FitAISpacing.sm),
            )
        }
    }
}

@Composable
private fun WeekStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = FitAIColors.OnSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
    }
}