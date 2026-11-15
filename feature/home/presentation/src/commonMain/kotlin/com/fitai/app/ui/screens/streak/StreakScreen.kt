package com.fitai.app.ui.screens.streak

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fitai.app.engagement.StreakDay
import com.fitai.app.engagement.WorkoutStreak
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.StreakIntent
import com.fitai.app.viewmodel.StreakViewModel
import kotlinx.datetime.isoDayNumber
import org.koin.compose.viewmodel.koinViewModel

/** Workout streak with this week's strip; shown after a finished session and from the Home flame. */
@Composable
fun StreakScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StreakViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.onIntent(StreakIntent.Load) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to FitAIColors.Accent.copy(alpha = 0.12f), 0.55f to FitAIColors.Background))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(FitAISpacing.xl))
        Text(strings.streakTitle, style = MaterialTheme.typography.headlineLarge, color = FitAIColors.OnSurface)
        Spacer(Modifier.weight(1f))
        val streak = state.streak
        if (state.loading || streak == null) {
            CircularProgressIndicator(color = FitAIColors.Accent)
        } else {
            StreakBody(streak)
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { viewModel.onIntent(StreakIntent.ToggleHowItWorks(true)) }) {
            Text(strings.streakHowItWorks, style = MaterialTheme.typography.bodyLarge, color = FitAIColors.OnSurfaceMuted)
        }
        Spacer(Modifier.height(FitAISpacing.sm))
        FitAIButton(text = strings.actionContinue, onClick = onContinue, modifier = Modifier.fillMaxWidth())
    }

    if (state.showHowItWorks) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(StreakIntent.ToggleHowItWorks(false)) },
            containerColor = FitAIColors.Surface,
            shape = FitAIShapes.card,
            icon = { Icon(FitAIIcons.LocalFireDepartment, contentDescription = null, tint = FitAIColors.Accent) },
            title = { Text(strings.streakHowItWorks, color = FitAIColors.OnSurface) },
            text = { Text(strings.streakHowItWorksBody, color = FitAIColors.OnSurfaceMuted) },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(StreakIntent.ToggleHowItWorks(false)) }) {
                    Text(strings.workoutSummaryDone, color = FitAIColors.Primary)
                }
            },
        )
    }
}

@Composable
private fun StreakBody(streak: WorkoutStreak) {
    val strings = LocalStrings.current
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
    val active = streak.currentDays > 0
    Row(
        modifier = Modifier.scale(scale),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        StreakFlame(
            active = active,
            modifier = Modifier
                .size(width = 120.dp, height = 144.dp)
                .semantics { contentDescription = strings.streakA11y(streak.currentDays) },
        )
        Text(
            text = streak.currentDays.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
            color = if (active) FitAIColors.Accent else FitAIColors.OnSurfaceMuted,
        )
    }
    Spacer(Modifier.height(FitAISpacing.xl))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        streak.week.forEach { day -> DayBubble(day) }
    }
    Spacer(Modifier.height(FitAISpacing.xl))
    Text(
        text = strings.streakQuote(streak.currentDays),
        style = MaterialTheme.typography.bodyLarge,
        color = FitAIColors.OnSurface,
        textAlign = TextAlign.Center,
    )
    streak.longestDays?.takeIf { it > streak.currentDays }?.let {
        Spacer(Modifier.height(FitAISpacing.sm))
        Text(strings.streakLongest(it), style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
    }
}

/** Two-layer flame drawn in a 100 × 120 box; grey when the streak is at zero. */
@Composable
private fun StreakFlame(active: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sx = size.width / 100f
        val sy = size.height / 120f
        fun Path.move(x: Float, y: Float) = moveTo(x * sx, y * sy)
        fun Path.curve(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) =
            cubicTo(x1 * sx, y1 * sy, x2 * sx, y2 * sy, x3 * sx, y3 * sy)
        val outer = Path().apply {
            move(50f, 0f)
            curve(62f, 22f, 94f, 40f, 94f, 76f)
            curve(94f, 102f, 74f, 120f, 50f, 120f)
            curve(26f, 120f, 6f, 102f, 6f, 78f)
            curve(6f, 56f, 18f, 44f, 30f, 32f)
            curve(30f, 48f, 36f, 58f, 46f, 60f)
            curve(40f, 38f, 42f, 16f, 50f, 0f)
            close()
        }
        val inner = Path().apply {
            move(50f, 56f)
            curve(60f, 68f, 74f, 80f, 74f, 96f)
            curve(74f, 110f, 63f, 117f, 50f, 117f)
            curve(37f, 117f, 26f, 110f, 26f, 96f)
            curve(26f, 84f, 36f, 76f, 43f, 68f)
            curve(43f, 76f, 47f, 80f, 53f, 80f)
            curve(50f, 72f, 48f, 64f, 50f, 56f)
            close()
        }
        if (active) {
            drawPath(outer, Brush.verticalGradient(listOf(FitAIColors.Warning, FitAIColors.Accent), endY = size.height))
            drawPath(inner, Brush.verticalGradient(listOf(Color(0xFFFFE3A3), FitAIColors.Warning), startY = size.height * 0.45f))
        } else {
            drawPath(outer, FitAIColors.SurfaceVariant)
            drawPath(inner, FitAIColors.OnSurfaceMuted.copy(alpha = 0.25f))
        }
    }
}

@Composable
private fun DayBubble(day: StreakDay) {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(FitAISpacing.sm)) {
        Text(
            text = strings.weekdayShort(day.date.dayOfWeek.isoDayNumber),
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) FitAIColors.OnSurface else FitAIColors.OnSurfaceMuted,
        )
        val base = Modifier.size(40.dp)
        Box(
            modifier = when {
                day.trained -> base
                    .shadow(12.dp, CircleShape, ambientColor = FitAIColors.Accent, spotColor = FitAIColors.Accent)
                    .background(FitAIColors.Accent, CircleShape)
                day.isToday -> base.border(2.dp, FitAIColors.Accent, CircleShape)
                else -> base.background(FitAIColors.SurfaceVariant, CircleShape)
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (day.trained) FitAIIcons.Check else FitAIIcons.FitnessCenter,
                contentDescription = null,
                tint = when {
                    day.trained -> FitAIColors.Background
                    day.isTrainingDay -> FitAIColors.OnSurfaceMuted
                    else -> FitAIColors.OnSurfaceMuted.copy(alpha = 0.35f)
                },
                modifier = Modifier.size(if (day.trained) 22.dp else 18.dp),
            )
        }
    }
}
