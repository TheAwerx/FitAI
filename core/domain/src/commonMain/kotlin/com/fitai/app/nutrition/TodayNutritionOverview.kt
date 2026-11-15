package com.fitai.app.nutrition

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.DailyMacroSummary
import com.fitai.app.model.DietPlan
import com.fitai.app.model.MealLog
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.util.currentDayOfWeek
import com.fitai.app.util.todayDateRangeIso

/** [targets] and [remaining] are null when the user has no active diet plan. */
data class TodayNutritionOverview(
    val activePlan: DietPlan?,
    val targets: DayMacroTargets?,
    val consumed: DailyMacroSummary,
    val remaining: DailyMacroSummary?,
    val isTrainingDay: Boolean,
    val meals: List<MealLog>,
) {
    companion object {
        val Empty = TodayNutritionOverview(
            activePlan = null,
            targets = null,
            consumed = DailyMacroSummary(0, 0.0, 0.0, 0.0),
            remaining = null,
            isTrainingDay = false,
            meals = emptyList(),
        )
    }
}

fun buildTodayNutritionOverview(
    activePlan: DietPlan?,
    todayMeals: List<MealLog>,
    isTrainingDay: Boolean,
): TodayNutritionOverview {
    val consumed = todayMeals.sumMacros()
    val targets = activePlan?.targetsFor(isTrainingDay)
    return TodayNutritionOverview(
        activePlan = activePlan,
        targets = targets,
        consumed = consumed,
        remaining = targets?.remainingAfter(consumed),
        isTrainingDay = isTrainingDay,
        meals = todayMeals,
    )
}

/** True when the active (non ad-hoc) workout plan has exercises on [dayOfWeek] (1 = Monday). */
suspend fun WorkoutRepository.isPlannedTrainingDay(
    userId: String,
    dayOfWeek: Int = currentDayOfWeek(),
): Boolean {
    val plan = getActivePlan(userId)?.takeUnless { it.isAdHoc } ?: return false
    return getExercisesForPlanDay(plan.id, dayOfWeek).isNotEmpty()
}

class GetTodayNutritionOverviewUseCase(
    private val localUserRepository: LocalUserRepository,
    private val nutritionRepository: NutritionRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(): UseCaseState<TodayNutritionOverview> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: return@useCaseCatching TodayNutritionOverview.Empty
        val (start, end) = todayDateRangeIso()
        buildTodayNutritionOverview(
            activePlan = nutritionRepository.getActiveDiet(user.id),
            todayMeals = nutritionRepository.getMealsForDate(user.id, start, end),
            isTrainingDay = workoutRepository.isPlannedTrainingDay(user.id),
        )
    }
}
