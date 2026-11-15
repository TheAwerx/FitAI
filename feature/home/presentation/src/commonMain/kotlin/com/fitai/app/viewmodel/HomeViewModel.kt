package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.access.AccessTier
import com.fitai.app.access.GetAccessTierUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.engagement.GetEngagementSummaryUseCase
import com.fitai.app.engagement.GetWorkoutStreakUseCase
import com.fitai.app.home.CanAccessQuickActionsUseCase
import com.fitai.app.home.GetHomeDashboardUseCase
import com.fitai.app.nutrition.GetTodayNutritionOverviewUseCase
import com.fitai.app.stats.ComputeWeeklyStatsUseCase
import com.fitai.app.viewmodel.home.HomeEffect
import com.fitai.app.viewmodel.home.HomeIntent
import com.fitai.app.viewmodel.home.HomeState
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getHomeDashboardUseCase: GetHomeDashboardUseCase,
    private val canAccessQuickActionsUseCase: CanAccessQuickActionsUseCase,
    private val getEngagementSummaryUseCase: GetEngagementSummaryUseCase,
    private val getTodayNutritionOverviewUseCase: GetTodayNutritionOverviewUseCase,
    private val computeWeeklyStatsUseCase: ComputeWeeklyStatsUseCase,
    private val getWorkoutStreakUseCase: GetWorkoutStreakUseCase,
    private val getAccessTierUseCase: GetAccessTierUseCase,
) : BaseViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.Refresh -> refresh()
            HomeIntent.QuickLogMeal -> viewModelScope.launch {
                when (val access = canAccessQuickActionsUseCase()) {
                    is UseCaseState.Success -> {
                        if (access.data) setEffect(HomeEffect.NavigateToMealLog)
                        else setEffect(HomeEffect.NavigateToPaywall)
                    }
                    is UseCaseState.Error -> setEffect(HomeEffect.NavigateToPaywall)
                }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val dashboard = async { getHomeDashboardUseCase() }
            val engagement = async { getEngagementSummaryUseCase() }
            val nutrition = async { getTodayNutritionOverviewUseCase() }
            val weekly = async { computeWeeklyStatsUseCase() }
            val streak = async { getWorkoutStreakUseCase() }
            val tier = async { getAccessTierUseCase() }

            val dashboardData = (dashboard.await() as? UseCaseState.Success)?.data
            val engagementData = (engagement.await() as? UseCaseState.Success)?.data
            val nutritionData = (nutrition.await() as? UseCaseState.Success)?.data
            val weeklyData = (weekly.await() as? UseCaseState.Success)?.data
            val streakData = (streak.await() as? UseCaseState.Success)?.data
            val tierData = (tier.await() as? UseCaseState.Success)?.data
            setState {
                copy(
                    workoutStreak = streakData,
                    isPro = tierData == AccessTier.Pro,
                    userName = dashboardData?.userName ?: userName,
                    activePlan = dashboardData?.activePlan,
                    todayExercises = dashboardData?.todayExercises.orEmpty(),
                    trialStatus = dashboardData?.trialStatus,
                    engagement = engagementData,
                    nutrition = nutritionData,
                    weeklyStats = weeklyData,
                    loading = false,
                )
            }
        }
    }
}
