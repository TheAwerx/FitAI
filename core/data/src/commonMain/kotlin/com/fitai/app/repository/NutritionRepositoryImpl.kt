package com.fitai.app.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.fitai.app.db.FitAiDatabase
import com.fitai.app.model.DailyMacroSummary
import com.fitai.app.model.DietPlan
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.MealLog
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
            )
            DietPlan(
                id = planId,
                userId = userId,
                dailyCalories = generated.dailyCalories,
                proteinG = generated.proteinG,
                carbsG = generated.carbsG,
                fatG = generated.fatG,
                createdAt = now,
                isActive = true,
            )
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
    ): MealLog = withContext(Dispatchers.Default) {
        val id = newId()
        database.nutritionQueries.insertMealLog(
            id = id,
            user_id = userId,
            meal_name = mealName,
            calories = calories.toLong(),
            protein_g = proteinG,
            carbs_g = carbsG,
            fat_g = fatG,
            photo_url = photoUrl,
            logged_at = loggedAt,
        )
        MealLog(
            id = id,
            userId = userId,
            mealName = mealName,
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            photoUrl = photoUrl,
            loggedAt = loggedAt,
        )
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

    private fun newId(): String = buildString {
        repeat(16) { append(Random.nextInt(16).toString(16)) }
    }
}
