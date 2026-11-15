package com.fitai.app.repository

import com.fitai.app.model.DailyMacroSummary
import com.fitai.app.model.DietPlan
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.MealEntryDraft
import com.fitai.app.model.MealLog
import com.fitai.app.model.PlanMeal
import kotlinx.coroutines.flow.Flow

interface NutritionRepository {
    fun observeActiveDiet(userId: String): Flow<DietPlan?>
    fun observePlansForUser(userId: String): Flow<List<DietPlan>>
    fun observeMealsForUser(userId: String): Flow<List<MealLog>>
    suspend fun getActiveDiet(userId: String): DietPlan?
    suspend fun saveGeneratedDiet(userId: String, generated: GeneratedDietPlan): DietPlan

    /** Replaces the meals of a plan (used when the user swaps in an alternative). */
    suspend fun updateDietPlanMeals(planId: String, meals: List<PlanMeal>)

    suspend fun logMeal(
        userId: String,
        mealName: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        loggedAt: String,
        photoUrl: String? = null,
        slot: String? = null,
    ): MealLog

    /** Adds several diary entries at once (a food, or a whole planned meal), in one transaction. */
    suspend fun logEntries(userId: String, entries: List<MealEntryDraft>, loggedAt: String): List<MealLog>

    suspend fun getMeal(mealId: String): MealLog?

    /** Rewrites name, macros, slot and serving of an entry; keeps its id, owner, photo marker and log time. */
    suspend fun updateMeal(meal: MealLog)

    suspend fun deleteMeal(mealId: String)
    suspend fun getMealsForDate(userId: String, dayStart: String, dayEnd: String): List<MealLog>
    /** [start] inclusive, [end] exclusive ISO timestamps; newest first. */
    suspend fun getMealsInRange(userId: String, start: String, end: String): List<MealLog>
    suspend fun getDailyMacroSummary(userId: String, dayStart: String, dayEnd: String): DailyMacroSummary
}
