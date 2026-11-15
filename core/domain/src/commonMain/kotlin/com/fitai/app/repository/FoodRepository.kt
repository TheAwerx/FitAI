package com.fitai.app.repository

import com.fitai.app.model.CustomFoodDraft
import com.fitai.app.model.FoodItem

/** A food's most recent serving in the diary, to prefill the amount picker. */
data class LastServing(val unit: String, val amount: Double)

/**
 * The food database: the bundled offline catalog plus the user's custom foods, and what the user logged
 * from it (recent and frequent foods come from `meal_logs.food_id`).
 */
interface FoodRepository {
    /** Bundled foods only; loaded once and cached. */
    suspend fun bundledFoods(): List<FoodItem>

    /** Custom foods of [userId] first, then the bundled catalog. */
    suspend fun catalog(userId: String?): List<FoodItem>

    suspend fun getFood(id: String): FoodItem?

    suspend fun customFoods(userId: String): List<FoodItem>

    /** Creates the food, or replaces it when [id] is given. */
    suspend fun saveCustomFood(userId: String, draft: CustomFoodDraft, id: String? = null): FoodItem

    suspend fun deleteCustomFood(id: String)

    /** Food ids logged most recently, newest first. */
    suspend fun recentFoodIds(userId: String, limit: Int): List<String>

    /** Food ids logged most often, most used first. */
    suspend fun frequentFoodIds(userId: String, limit: Int): List<String>

    suspend fun lastServing(userId: String, foodId: String): LastServing?
}
