package com.fitai.app.nutrition

import com.fitai.app.model.DietPlan
import com.fitai.app.model.Macros
import com.fitai.app.model.MealLog
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.util.todayDateRangeIso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** [diary] is today's meals in order with the plan for each; [planTotals] is null for target-only plans. */
data class NutritionOverview(
    val activePlan: DietPlan?,
    val planHistory: List<DietPlan>,
    val todayMeals: List<MealLog>,
    val diary: List<DiarySlot> = emptyList(),
    val planTotals: Macros? = null,
)

class ObserveNutritionOverviewUseCase(
    private val localUserRepository: LocalUserRepository,
    private val nutritionRepository: NutritionRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(): Flow<NutritionOverview> {
        val user = localUserRepository.getLatestUser() ?: return flowOf(
            NutritionOverview(null, emptyList(), emptyList(), buildDiary(null, emptyList()) { null }),
        )
        return combine(
            nutritionRepository.observeActiveDiet(user.id),
            nutritionRepository.observePlansForUser(user.id),
            nutritionRepository.observeMealsForUser(user.id),
        ) { active, plans, allMeals ->
            val (start, end) = todayDateRangeIso()
            val meals = allMeals.filter { it.loggedAt >= start && it.loggedAt < end }
            val zone = TimeZone.currentSystemDefault()
            NutritionOverview(
                activePlan = active,
                planHistory = plans,
                todayMeals = meals,
                diary = buildDiary(active, meals) { localMinutesOf(it.loggedAt, zone) },
                planTotals = active?.takeIf { it.meals.isNotEmpty() }?.plannedTotals(),
            )
        }
    }
}

/** Minutes after local midnight of an ISO instant, or null when it can't be parsed. */
@OptIn(ExperimentalTime::class)
internal fun localMinutesOf(iso: String, zone: TimeZone): Int? = runCatching {
    val time = Instant.parse(iso).toLocalDateTime(zone)
    time.hour * 60 + time.minute
}.getOrNull()
