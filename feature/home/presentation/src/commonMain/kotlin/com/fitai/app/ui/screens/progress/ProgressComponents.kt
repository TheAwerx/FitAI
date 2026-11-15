package com.fitai.app.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fitai.app.engagement.Badge
import com.fitai.app.engagement.BadgeId
import com.fitai.app.engagement.WeeklyVolume
import com.fitai.app.engagement.WeightPoint
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.training.PersonalRecord
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.home.formatOneDecimal
import kotlinx.datetime.number

@Composable
internal fun WeightTrendChart(points: List<WeightPoint>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val pad = 6.dp.toPx()
        val firstDay = points.first().date.toEpochDays()
        val spanDays = (points.last().date.toEpochDays() - firstDay).coerceAtLeast(1)
        val minWeight = points.minOf { it.weightKg }
        val maxWeight = points.maxOf { it.weightKg }
        val range = (maxWeight - minWeight).coerceAtLeast(1.0)
        val chartWidth = size.width - 2 * pad
        val chartHeight = size.height - 2 * pad

        fun offsetOf(point: WeightPoint): Offset {
            val x = if (points.size == 1) size.width / 2 else pad + (point.date.toEpochDays() - firstDay).toFloat() / spanDays * chartWidth
            val y = pad + ((maxWeight - point.weightKg) / range).toFloat() * chartHeight
            return Offset(x, y)
        }

        repeat(3) { index ->
            val y = pad + chartHeight * index / 2
            drawLine(FitAIColors.SurfaceVariant, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        val offsets = points.map(::offsetOf)
        if (offsets.size > 1) {
            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(
                path = path,
                color = FitAIColors.Primary,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        offsets.forEachIndexed { index, offset ->
            val last = index == offsets.lastIndex
            drawCircle(FitAIColors.Primary, radius = (if (last) 5.dp else 3.dp).toPx(), center = offset)
        }
    }
}

@Composable
internal fun WeeklyVolumeBars(weeks: List<WeeklyVolume>, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val max = weeks.maxOfOrNull { it.volumeKg }?.takeIf { it > 0.0 } ?: 1.0
    Row(
        modifier = modifier.fillMaxWidth().height(132.dp),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm),
    ) {
        weeks.forEachIndexed { index, week ->
            val current = index == weeks.lastIndex
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((week.volumeKg / max).toFloat().coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (current) FitAIColors.Primary else FitAIColors.Primary.copy(alpha = 0.35f)),
                    )
                }
                Text(
                    text = strings.progressWeekShort(week.weekStart.day, week.weekStart.month.number),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (current) FitAIColors.OnSurface else FitAIColors.OnSurfaceMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun PersonalRecordRow(record: PersonalRecord, showDivider: Boolean) {
    val strings = LocalStrings.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
                Text(
                    text = record.exerciseName,
                    style = MaterialTheme.typography.titleSmall,
                    color = FitAIColors.OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.progressPrValue(formatOneDecimal(record.weightKg), record.reps),
                    style = MaterialTheme.typography.bodySmall,
                    color = FitAIColors.OnSurfaceMuted,
                )
            }
            Text(
                text = strings.progressE1rm(formatOneDecimal(record.e1rm)),
                style = MaterialTheme.typography.labelMedium,
                color = FitAIColors.OnSurface,
            )
        }
        if (showDivider) {
            Spacer(Modifier.height(FitAISpacing.sm))
            HorizontalDivider(color = FitAIColors.SurfaceVariant)
            Spacer(Modifier.height(FitAISpacing.sm))
        }
    }
}

@Composable
internal fun BadgeGrid(badges: List<Badge>, columns: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
        badges.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
                row.forEach { badge -> BadgeTile(badge, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BadgeTile(badge: Badge, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (badge.unlocked) FitAIColors.Primary.copy(alpha = 0.2f) else FitAIColors.SurfaceVariant,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (badge.unlocked) badge.id.icon() else FitAIIcons.Lock,
                contentDescription = null,
                tint = if (badge.unlocked) FitAIColors.Primary else FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = strings.badgeName(badge.id),
            style = MaterialTheme.typography.labelSmall,
            color = if (badge.unlocked) FitAIColors.OnSurface else FitAIColors.OnSurfaceMuted.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun BadgeId.icon(): ImageVector = when (this) {
    BadgeId.FirstSession, BadgeId.Volume10t -> FitAIIcons.FitnessCenter
    BadgeId.FirstMealPhoto, BadgeId.Meals100 -> FitAIIcons.Favorite
    BadgeId.FirstWeighIn -> FitAIIcons.CheckCircle
    BadgeId.FirstPr -> FitAIIcons.TipsAndUpdates
    BadgeId.Streak7, BadgeId.Streak28 -> FitAIIcons.LocalFireDepartment
    BadgeId.Sessions10 -> FitAIIcons.DirectionsRun
}

private fun Strings.badgeName(id: BadgeId): String = when (id) {
    BadgeId.FirstSession -> badgeFirstSession
    BadgeId.FirstMealPhoto -> badgeFirstMealPhoto
    BadgeId.FirstWeighIn -> badgeFirstWeighIn
    BadgeId.FirstPr -> badgeFirstPr
    BadgeId.Streak7 -> badgeStreak7
    BadgeId.Streak28 -> badgeStreak28
    BadgeId.Sessions10 -> badgeSessions10
    BadgeId.Meals100 -> badgeMeals100
    BadgeId.Volume10t -> badgeVolume10t
}
