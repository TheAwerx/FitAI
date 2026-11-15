package com.fitai.app.nutrition

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.CustomFoodDraft
import com.fitai.app.model.FoodItem
import com.fitai.app.model.MealLog
import com.fitai.app.model.MealSlot
import com.fitai.app.repository.FoodRepository
import com.fitai.app.repository.LastServing
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.util.appLocale
import com.fitai.app.util.nowIsoTimestamp

/**
 * Search screen content: matches for [query], or (blank query) the foods of the plan for this meal plus
 * recent, frequent and custom foods.
 */
data class FoodSearchResult(
    val query: String,
    val results: List<FoodItem> = emptyList(),
    val fromPlan: List<FoodItem> = emptyList(),
    val recent: List<FoodItem> = emptyList(),
    val frequent: List<FoodItem> = emptyList(),
    val custom: List<FoodItem> = emptyList(),
)

class SearchFoodsUseCase(
    private val localUserRepository: LocalUserRepository,
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(query: String, slot: String? = null): UseCaseState<FoodSearchResult> = useCaseCatching {
        val userId = localUserRepository.getLatestUser()?.id
        val catalog = foodRepository.catalog(userId)
        if (query.isNotBlank()) return@useCaseCatching FoodSearchResult(query, results = searchFoods(catalog, query, appLocale()))
        val byId = catalog.associateBy { it.id }
        val planFoods = userId?.let { nutritionRepository.getActiveDiet(it) }?.meals
            ?.firstOrNull { it.slot == slot }
            ?.options.orEmpty()
            .flatMap { option -> option.foods.mapNotNull { food -> food.foodId?.let(byId::get) } }
            .distinctBy { it.id }
        FoodSearchResult(
            query = query,
            fromPlan = planFoods,
            recent = userId?.let { foodRepository.recentFoodIds(it, LIST_LIMIT) }.orEmpty().mapNotNull(byId::get),
            frequent = userId?.let { foodRepository.frequentFoodIds(it, LIST_LIMIT) }.orEmpty().mapNotNull(byId::get),
            custom = catalog.filter { it.isCustom },
        )
    }

    private companion object {
        const val LIST_LIMIT = 15
    }
}

/** A food to log, or an existing diary entry to edit ([entry] set). [lastServing] prefills the picker. */
data class FoodDetail(
    val food: FoodItem?,
    val entry: MealLog?,
    val lastServing: LastServing?,
)

class GetFoodDetailUseCase(
    private val localUserRepository: LocalUserRepository,
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(foodId: String?, mealId: String?): UseCaseState<FoodDetail> = useCaseCatching {
        val entry = mealId?.let { nutritionRepository.getMeal(it) }
        val id = foodId ?: entry?.foodId
        val food = id?.let { foodRepository.getFood(it) }
        val user = localUserRepository.getLatestUser()
        val last = if (entry == null && food != null && user != null) foodRepository.lastServing(user.id, food.id) else null
        check(food != null || entry != null) { "food_not_found" }
        FoodDetail(food, entry, last)
    }
}

class AddFoodToDiaryUseCase(
    private val localUserRepository: LocalUserRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(food: FoodItem, unit: String, amount: Double, slot: MealSlot): UseCaseState<MealLog> =
        useCaseCatching {
            check(amount > 0) { "invalid_amount" }
            val user = localUserRepository.getLatestUser() ?: error("no_user")
            val entry = food.entryFor(unit, amount, slot.code, appLocale())
            nutritionRepository.logEntries(user.id, listOf(entry), nowIsoTimestamp()).first()
        }
}

/** New amount (and meal) for an entry logged with a serving; recomputed from the database when possible. */
class UpdateLoggedFoodUseCase(
    private val foodRepository: FoodRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(mealId: String, unit: String, amount: Double, slot: MealSlot): UseCaseState<MealLog> =
        useCaseCatching {
            check(amount > 0) { "invalid_amount" }
            val meal = nutritionRepository.getMeal(mealId) ?: error("meal_not_found")
            val food = meal.foodId?.let { foodRepository.getFood(it) }
            val updated = meal.withAmount(food, unit, amount, slot.code)
            nutritionRepository.updateMeal(updated)
            updated
        }
}

/** Name, totals and meal of a photo or quick-add entry. */
class EditMealEntryUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(
        mealId: String,
        name: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        slot: MealSlot?,
    ): UseCaseState<MealLog> = useCaseCatching {
        check(name.isNotBlank() && calories >= 0 && proteinG >= 0 && carbsG >= 0 && fatG >= 0) { "validation_fields_required" }
        val meal = nutritionRepository.getMeal(mealId) ?: error("meal_not_found")
        val updated = meal.copy(
            mealName = name.trim(),
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            slot = slot?.code ?: meal.slot,
        )
        nutritionRepository.updateMeal(updated)
        updated
    }
}

class GetMealEntryUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(mealId: String): UseCaseState<MealLog> = useCaseCatching {
        nutritionRepository.getMeal(mealId) ?: error("meal_not_found")
    }
}

class DeleteMealEntryUseCase(
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(mealId: String): UseCaseState<Unit> = useCaseCatching {
        nutritionRepository.deleteMeal(mealId)
    }
}

/** "Planladığımı yedim": logs every food of the plan option for [slot] in one go. */
class LogPlannedMealUseCase(
    private val localUserRepository: LocalUserRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(slot: String, optionIndex: Int? = null): UseCaseState<List<MealLog>> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val plan = nutritionRepository.getActiveDiet(user.id) ?: error("no_plan")
        val meal = plan.meals.firstOrNull { it.slot == slot } ?: error("no_plan_meal")
        val entries = meal.toEntries(optionIndex ?: meal.selectedIndex)
        check(entries.isNotEmpty()) { "no_plan_meal" }
        nutritionRepository.logEntries(user.id, entries, nowIsoTimestamp())
    }
}

/** Swaps an alternative in as the meal's plan; the choice is saved with the plan. */
class SelectPlanMealOptionUseCase(
    private val localUserRepository: LocalUserRepository,
    private val nutritionRepository: NutritionRepository,
) {
    suspend operator fun invoke(slot: String, optionIndex: Int): UseCaseState<Unit> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val plan = nutritionRepository.getActiveDiet(user.id) ?: error("no_plan")
        val meal = plan.meals.firstOrNull { it.slot == slot } ?: error("no_plan_meal")
        check(optionIndex in meal.options.indices) { "no_plan_option" }
        nutritionRepository.updateDietPlanMeals(plan.id, plan.meals.selectOption(slot, optionIndex))
    }
}

class SaveCustomFoodUseCase(
    private val localUserRepository: LocalUserRepository,
    private val foodRepository: FoodRepository,
) {
    suspend operator fun invoke(draft: CustomFoodDraft, id: String? = null): UseCaseState<FoodItem> = useCaseCatching {
        check(draft.isValid) { "validation_fields_required" }
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        foodRepository.saveCustomFood(user.id, draft, id)
    }
}

class DeleteCustomFoodUseCase(
    private val foodRepository: FoodRepository,
) {
    suspend operator fun invoke(id: String): UseCaseState<Unit> = useCaseCatching {
        foodRepository.deleteCustomFood(id)
    }
}
