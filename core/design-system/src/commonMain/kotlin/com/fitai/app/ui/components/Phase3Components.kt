package com.fitai.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAISpacing

@Composable
fun MacroRing(
    caloriesConsumed: Int,
    caloriesTarget: Int,
    proteinG: Double,
    proteinTarget: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (caloriesTarget > 0) {
        (caloriesConsumed.toFloat() / caloriesTarget.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(96.dp)) {
            val stroke = 10.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(
                color = FitAIColors.SurfaceVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = FitAIColors.Primary,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs)) {
            Text(
                text = "$caloriesConsumed / $caloriesTarget kcal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "P: ${proteinG.toInt()} / $proteinTarget g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun TrialBanner(
    daysRemaining: Int,
    isActive: Boolean,
    activeText: String,
    expiredText: String,
    modifier: Modifier = Modifier,
) {
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isActive) activeText else expiredText,
            modifier = Modifier.padding(FitAISpacing.md),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) FitAIColors.Primary else FitAIColors.Accent,
        )
    }
}

@Composable
fun WorkoutPlanCard(
    title: String,
    subtitle: String,
    isActive: Boolean,
    activeLabel: String,
    modifier: Modifier = Modifier,
) {
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (isActive) {
                Text(activeLabel, style = MaterialTheme.typography.labelSmall, color = FitAIColors.Primary)
            }
        }
    }
}

@Composable
fun MealLogCard(
    mealName: String,
    calories: Int,
    macrosLine: String,
    modifier: Modifier = Modifier,
) {
    FitAICard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(FitAISpacing.md),
            verticalArrangement = Arrangement.spacedBy(FitAISpacing.xs),
        ) {
            Text(mealName, style = MaterialTheme.typography.titleSmall)
            Text("$calories kcal", style = MaterialTheme.typography.bodyMedium, color = FitAIColors.Primary)
            Text(macrosLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SkeletonLoader(
    lines: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        repeat(lines) {
            FitAICard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "",
                    modifier = Modifier
                        .padding(FitAISpacing.md)
                        .fillMaxWidth(),
                )
            }
        }
    }
}
