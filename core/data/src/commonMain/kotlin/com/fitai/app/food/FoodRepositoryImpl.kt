package com.fitai.app.food

import com.fitai.app.db.Custom_foods
import com.fitai.app.db.FitAiDatabase
import com.fitai.app.model.CustomFoodDraft
import com.fitai.app.model.FoodCategory
import com.fitai.app.model.FoodItem
import com.fitai.app.model.FoodServing
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.roundToTenth
import com.fitai.app.repository.FoodRepository
import com.fitai.app.repository.LastServing
import com.fitai.app.util.nowIsoTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class FoodRepositoryImpl(
    private val database: FitAiDatabase,
    private val bundled: BundledFoodDataSource,
) : FoodRepository {

    private val queries get() = database.foodQueries

    override suspend fun bundledFoods(): List<FoodItem> = bundled.foods()

    override suspend fun catalog(userId: String?): List<FoodItem> {
        val custom = userId?.let { customFoods(it) }.orEmpty()
        return custom + bundled.foods()
    }

    override suspend fun getFood(id: String): FoodItem? =
        if (id.startsWith(CUSTOM_PREFIX)) {
            io { queries.selectCustomFoodById(id).executeAsOneOrNull()?.toFoodItem() }
        } else {
            bundled.foods().firstOrNull { it.id == id }
        }

    override suspend fun customFoods(userId: String): List<FoodItem> = io {
        queries.selectCustomFoodsForUser(userId).executeAsList().map { it.toFoodItem() }
    }

    override suspend fun saveCustomFood(userId: String, draft: CustomFoodDraft, id: String?): FoodItem = io {
        require(draft.isValid) { "invalid_custom_food" }
        val now = nowIsoTimestamp()
        val name = draft.name.trim().take(MAX_NAME)
        val unit = draft.servingUnit
        val grams = if (unit == FoodUnits.GRAM) 100.0 else draft.servingGrams
        val existing = id?.let { queries.selectCustomFoodById(it).executeAsOneOrNull() }
        val foodId = existing?.id ?: id ?: newCustomId()
        if (existing != null) {
            queries.updateCustomFood(
                name = name,
                kcal_per_100g = draft.per100g(draft.caloriesPerServing).roundToTenth(),
                protein_per_100g = draft.per100g(draft.proteinPerServing).roundToTenth(),
                carbs_per_100g = draft.per100g(draft.carbsPerServing).roundToTenth(),
                fat_per_100g = draft.per100g(draft.fatPerServing).roundToTenth(),
                serving_unit = unit,
                serving_grams = grams,
                updated_at = now,
                id = foodId,
            )
        } else {
            queries.insertCustomFood(
                id = foodId,
                user_id = userId,
                name = name,
                kcal_per_100g = draft.per100g(draft.caloriesPerServing).roundToTenth(),
                protein_per_100g = draft.per100g(draft.proteinPerServing).roundToTenth(),
                carbs_per_100g = draft.per100g(draft.carbsPerServing).roundToTenth(),
                fat_per_100g = draft.per100g(draft.fatPerServing).roundToTenth(),
                serving_unit = unit,
                serving_grams = grams,
                created_at = now,
                updated_at = now,
            )
        }
        queries.selectCustomFoodById(foodId).executeAsOneOrNull()?.toFoodItem() ?: error("custom_food_not_saved")
    }

    override suspend fun deleteCustomFood(id: String) = io {
        queries.deleteCustomFood(id)
        Unit
    }

    override suspend fun recentFoodIds(userId: String, limit: Int): List<String> = io {
        database.nutritionQueries.selectRecentFoodIds(userId, limit.toLong()).executeAsList().mapNotNull { it.food_id }
    }

    override suspend fun frequentFoodIds(userId: String, limit: Int): List<String> = io {
        database.nutritionQueries.selectFrequentFoodIds(userId, limit.toLong()).executeAsList().mapNotNull { it.food_id }
    }

    override suspend fun lastServing(userId: String, foodId: String): LastServing? = io {
        database.nutritionQueries.selectLastServingForFood(userId, foodId).executeAsOneOrNull()?.let { row ->
            val unit = row.serving_unit ?: return@let null
            val amount = row.serving_amount ?: return@let null
            LastServing(unit, amount)
        }
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.Default) { block() }

    companion object {
        /** Custom food ids are `custom_` + 16 hex chars; bundled ids never start with it. */
        const val CUSTOM_PREFIX = "custom_"
        private const val MAX_NAME = 80

        fun newCustomId(): String = CUSTOM_PREFIX + buildString { repeat(16) { append(Random.nextInt(16).toString(16)) } }
    }
}

internal fun Custom_foods.toFoodItem(): FoodItem = FoodItem(
    id = id,
    nameTr = name,
    nameEn = name,
    category = FoodCategory.Custom.code,
    kcalPer100g = kcal_per_100g,
    proteinPer100g = protein_per_100g,
    carbsPer100g = carbs_per_100g,
    fatPer100g = fat_per_100g,
    servings = if (serving_unit == FoodUnits.GRAM) emptyList() else listOf(FoodServing(serving_unit, serving_grams)),
    isCustom = true,
    source = "custom",
)
