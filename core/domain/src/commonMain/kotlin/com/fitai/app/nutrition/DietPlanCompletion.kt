package com.fitai.app.nutrition

import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.UserProfilePayload
import com.fitai.app.repository.FoodRepository

/**
 * Makes every saved plan detailed. A generator that returned targets only (an older server, or the mock)
 * gets meals built from the food database; AI meals get their foods linked to the database by name.
 * A food database that fails to load leaves the plan as it is, so plan generation never fails over it.
 */
suspend fun GeneratedDietPlan.withDetailedMeals(
    foodRepository: FoodRepository,
    profile: UserProfilePayload,
    locale: String,
): GeneratedDietPlan {
    val foods = runCatching { foodRepository.bundledFoods() }.getOrNull().orEmpty()
    if (foods.isEmpty()) return this
    return if (meals.isEmpty()) {
        copy(meals = StarterDiet.meals(this, profile, foods, locale))
    } else {
        copy(meals = meals.linkFoods(foods))
    }
}
