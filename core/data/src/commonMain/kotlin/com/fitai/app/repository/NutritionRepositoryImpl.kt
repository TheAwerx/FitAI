package com.fitai.app.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.fitai.app.db.FitAiDatabase
import com.fitai.app.model.DailyMacroSummary
import com.fitai.app.model.DietPlan
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.MealEntryDraft
import com.fitai.app.model.MealLog
import com.fitai.app.model.PlanMeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random

class NutritionRepositoryImpl(
    private val database: FitAiDatabase,
) : NutritionRepository {

    override fun observeActiveDiet(userId: String): Flow<DietPlan?> =
        database.nutritionQueries
            .selectActiveDiet(userId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }

    override fun observePlansForUser(userId: String): Flow<List<DietPlan>> =
        database.nutritionQueries
            .selectPlansForUser(userId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { plans -> plans.map { it.toDomain() } }

    override fun observeMealsForUser(userId: String): Flow<List<MealLog>> =
        database.nutritionQueries
            .selectMealsForUser(userId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { meals -> meals.map { it.toDomain() } }

    override suspend fun getActiveDiet(userId: String): DietPlan? = withContext(Dispatchers.Default) {
        database.nutritionQueries.selectActiveDiet(userId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveGeneratedDiet(userId: String, generated: GeneratedDietPlan): DietPlan =
        withContext(Dispatchers.Default) {
            val planId = newId()
            val now = com.fitai.app.util.nowIsoTimestamp()
            database.transaction {
                database.nutritionQueries.deactivateDietPlansForUser(userId)
                database.nutritionQueries.insertDietPlan(
                    id = planId,
                    user_id = userId,
                    daily_calories = generated.dailyCalories.toLong(),
                    protein_g = generated.proteinG.toLong(),
                    carbs_g = generated.carbsG.toLong(),
                    fat_g = generated.fatG.toLong(),
                    created_at = now,
                    is_active = 1,
                    meals_json = PlanMealsJson.encode(generated.meals),
                )
            }
            DietPlan(
                id = planId,
                userId = userId,
                dailyCalories = generated.dailyCalories,
                proteinG = generated.proteinG,
                carbsG = generated.carbsG,
                fatG = generated.fatG,
                createdAt = now,
                isActive = true,
                meals = generated.meals,
            )
        }

    override suspend fun updateDietPlanMeals(planId: String, meals: List<PlanMeal>) = withContext(Dispatchers.Default) {
        database.nutritionQueries.updateDietPlanMeals(PlanMealsJson.encode(meals), planId)
        Unit
    }

    override suspend fun logMeal(
        userId: String,
        mealName: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        loggedAt: String,
        photoUrl: String?,
        slot: String?,
    ): MealLog = withContext(Dispatchers.Default) {
        insert(
            userId,
            MealEntryDraft(mealName, calories, proteinG, carbsG, fatG, slot = slot, photoUrl = photoUrl),
            loggedAt,
        )
    }

    override suspend fun logEntries(userId: String, entries: List<MealEntryDraft>, loggedAt: String): List<MealLog> =
        withContext(Dispatchers.Default) {
            database.transactionWithResult { entries.map { insert(userId, it, loggedAt) } }
        }

    override suspend fun getMeal(mealId: String): MealLog? = withContext(Dispatchers.Default) {
        database.nutritionQueries.selectMealLogById(mealId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun updateMeal(meal: MealLog) = withContext(Dispatchers.Default) {
        database.nutritionQueries.updateMealLog(
            meal_name = meal.mealName,
            calories = meal.calories.toLong(),
            protein_g = meal.proteinG,
            carbs_g = meal.carbsG,
            fat_g = meal.fatG,
            photo_url = meal.photoUrl,
            logged_at = meal.loggedAt,
            slot = meal.slot,
            food_id = meal.foodId,
            serving_unit = meal.servingUnit,
            serving_amount = meal.servingAmount,
            grams = meal.grams,
            id = meal.id,
        )
        Unit
    }

    override suspend fun deleteMeal(mealId: String) = withContext(Dispatchers.Default) {
        database.nutritionQueries.deleteMealLog(mealId)
        Unit
    }

    override suspend fun getMealsForDate(userId: String, dayStart: String, dayEnd: String): List<MealLog> =
        withContext(Dispatchers.Default) {
            database.nutritionQueries.selectMealsForUserOnDate(userId, dayStart, dayEnd)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun getMealsInRange(userId: String, start: String, end: String): List<MealLog> =
        getMealsForDate(userId, start, end)

    override suspend fun getDailyMacroSummary(userId: String, dayStart: String, dayEnd: String): DailyMacroSummary =
        withContext(Dispatchers.Default) {
            val meals = getMealsForDate(userId, dayStart, dayEnd)
            DailyMacroSummary(
                calories = meals.sumOf { it.calories },
                proteinG = meals.sumOf { it.proteinG },
                carbsG = meals.sumOf { it.carbsG },
                fatG = meals.sumOf { it.fatG },
            )
        }

    private fun insert(userId: String, entry: MealEntryDraft, loggedAt: String): MealLog {
        val meal = MealLog(
            id = newId(),
            userId = userId,
            mealName = entry.name,
            calories = entry.calories,
            proteinG = entry.proteinG,
            carbsG = entry.carbsG,
            fatG = entry.fatG,
            photoUrl = entry.photoUrl,
            loggedAt = loggedAt,
            slot = entry.slot,
            foodId = entry.foodId,
            servingUnit = entry.servingUnit,
            servingAmount = entry.servingAmount,
            grams = entry.grams,
        )
        database.nutritionQueries.insertMealLog(
            id = meal.id,
            user_id = userId,
            meal_name = meal.mealName,
            calories = meal.calories.toLong(),
            protein_g = meal.proteinG,
            carbs_g = meal.carbsG,
            fat_g = meal.fatG,
            photo_url = meal.photoUrl,
            logged_at = meal.loggedAt,
            slot = meal.slot,
            food_id = meal.foodId,
            serving_unit = meal.servingUnit,
            serving_amount = meal.servingAmount,
            grams = meal.grams,
        )
        return meal
    }

    private fun newId(): String = buildString {
        repeat(16) { append(Random.nextInt(16).toString(16)) }
    }
}
