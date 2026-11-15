package com.fitai.app.nutrition

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.DietPlan
import com.fitai.app.model.MealLog
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.util.todayDateRangeIso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

data class NutritionOverview(
    val activePlan: DietPlan?,
    val planHistory: List<DietPlan>,
    val todayMeals: List<MealLog>,
)

class ObserveNutritionOverviewUseCase(
    private val localUserRepository: LocalUserRepository,
    private val nutritionRepository: NutritionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(): Flow<NutritionOverview> {
        val user = localUserRepository.getLatestUser() ?: return flowOf(
            NutritionOverview(null, emptyList(), emptyList()),
        )
        return combine(
            nutritionRepository.observeActiveDiet(user.id),
            nutritionRepository.observePlansForUser(user.id),
            nutritionRepository.observeMealsForUser(user.id),
        ) { active, plans, allMeals ->
            val (start, end) = todayDateRangeIso()
            val meals = allMeals.filter { it.loggedAt >= start && it.loggedAt < end }
            NutritionOverview(active, plans, meals)
        }
    }
}
