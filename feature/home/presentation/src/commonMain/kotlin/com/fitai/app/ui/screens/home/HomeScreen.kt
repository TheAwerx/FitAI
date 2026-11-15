package com.fitai.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.fitai.app.localization.LocalStrings
import com.fitai.app.ui.components.SkeletonLoader
import com.fitai.app.ui.components.TrialBanner
import com.fitai.app.ui.components.WorkoutPlanCard
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.viewmodel.HomeViewModel
import com.fitai.app.viewmodel.home.HomeEffect
import com.fitai.app.viewmodel.home.HomeIntent
import org.koin.compose.viewmodel.koinViewModel

/** [coachEntry] is a slot so this module does not depend on the coach feature. */
@Composable
fun HomeScreen(
    onNavigateMealLog: () -> Unit,
    onNavigatePaywall: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenStreak: () -> Unit = {},
    coachEntry: @Composable () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val strings = LocalStrings.current

    LaunchedEffect(Unit) { viewModel.onIntent(HomeIntent.Refresh) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.NavigateToMealLog -> onNavigateMealLog()
                HomeEffect.NavigateToPaywall -> onNavigatePaywall()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(FitAISpacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        HomeHeader(
            streakDays = state.workoutStreak?.currentDays ?: 0,
            trainedToday = state.workoutStreak?.trainedToday == true,
            isPro = state.isPro,
            onStreakClick = onOpenStreak,
        )
        Text(
            strings.homeSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.trialStatus?.takeUnless { state.isPro }?.let { trial ->
            TrialBanner(
                daysRemaining = trial.daysRemaining,
                isActive = trial.isActive,
                activeText = strings.homeTrialRemaining(trial.daysRemaining),
                expiredText = strings.homeTrialExpired,
            )
        }
        if (state.loading) {
            SkeletonLoader()
            return@Column
        }
        state.engagement?.let { engagement ->
            StreakXpRow(engagement.streak, engagement.weeklyXp, engagement.level)
        }
        TodayWorkoutSection(state.activePlan?.title, state.todayExercises.map { "${it.name} — ${it.sets}×${it.reps}" })
        RemainingMacrosCard(state.nutrition) { viewModel.onIntent(HomeIntent.QuickLogMeal) }
        state.weeklyStats?.let { WeeklySummaryCard(it) }
        coachEntry()
    }
}

@Composable
private fun TodayWorkoutSection(planTitle: String?, exerciseLines: List<String>) {
    val strings = LocalStrings.current
    Text(strings.homeTodayWorkout, style = MaterialTheme.typography.titleMedium)
    if (exerciseLines.isEmpty() || planTitle == null) {
        Text(strings.homeNoWorkout, color = FitAIColors.OnSurfaceMuted)
        return
    }
    WorkoutPlanCard(
        title = planTitle,
        subtitle = strings.homeTodayExerciseCount(exerciseLines.size),
        isActive = true,
        activeLabel = strings.workoutActivePlan,
    )
    exerciseLines.forEach { line ->
        Text("• $line", style = MaterialTheme.typography.bodySmall, color = FitAIColors.OnSurfaceMuted)
    }
}
