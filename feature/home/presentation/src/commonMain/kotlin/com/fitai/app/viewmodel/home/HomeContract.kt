package com.fitai.app.viewmodel.home

import com.fitai.app.engagement.EngagementSummary
import com.fitai.app.engagement.WorkoutStreak
import com.fitai.app.model.Exercise
import com.fitai.app.model.TrialStatus
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.nutrition.TodayNutritionOverview
import com.fitai.app.stats.WeeklyStats

data class HomeState(
    val userName: String = "",
    val activePlan: WorkoutPlan? = null,
    val todayExercises: List<Exercise> = emptyList(),
    val trialStatus: TrialStatus? = null,
    val engagement: EngagementSummary? = null,
    val nutrition: TodayNutritionOverview? = null,
    val weeklyStats: WeeklyStats? = null,
    val workoutStreak: WorkoutStreak? = null,
    val isPro: Boolean = false,
    val loading: Boolean = true,
)

sealed interface HomeIntent {
    data object Refresh : HomeIntent
    data object QuickLogMeal : HomeIntent
}

sealed interface HomeEffect {
    data object NavigateToMealLog : HomeEffect
    data object NavigateToPaywall : HomeEffect
}
