package com.fitai.app.viewmodel.nutrition

import com.fitai.app.model.FoodItem
import com.fitai.app.model.FoodServing
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.Macros
import com.fitai.app.model.MealLog
import com.fitai.app.model.MealSlot

// Food search

data class FoodSearchState(
    val slot: MealSlot = MealSlot.Lunch,
    val query: String = "",
    val results: List<FoodItem> = emptyList(),
    val fromPlan: List<FoodItem> = emptyList(),
    val recent: List<FoodItem> = emptyList(),
    val frequent: List<FoodItem> = emptyList(),
    val custom: List<FoodItem> = emptyList(),
    val loading: Boolean = false,
    /** True once results for the current non-blank query arrived. */
    val searched: Boolean = false,
    val failed: Boolean = false,
)

sealed interface FoodSearchIntent {
    /** [session] changes on every fresh open of the screen, so a new search starts empty. */
    data class Load(val slot: MealSlot, val session: Int = 0) : FoodSearchIntent
    data class QueryChanged(val query: String) : FoodSearchIntent
    data class Select(val food: FoodItem) : FoodSearchIntent
    data object CreateCustom : FoodSearchIntent
    data object QuickAdd : FoodSearchIntent
}

sealed interface FoodSearchEffect {
    data class OpenFood(val foodId: String, val slot: MealSlot) : FoodSearchEffect
    data class OpenCustomFood(val slot: MealSlot) : FoodSearchEffect
    data class OpenQuickAdd(val slot: MealSlot) : FoodSearchEffect
}

// Food detail: add a food, or change the amount of a logged entry

enum class FoodDetailError { InvalidAmount, LoadFailed, SaveFailed }

data class FoodDetailState(
    val loading: Boolean = true,
    val food: FoodItem? = null,
    /** The diary entry being edited; null when adding. */
    val entry: MealLog? = null,
    val servings: List<FoodServing> = emptyList(),
    val unit: String = FoodUnits.GRAM,
    val amountText: String = "",
    val slot: MealSlot = MealSlot.Lunch,
    val preview: Macros? = null,
    val saving: Boolean = false,
    val error: FoodDetailError? = null,
) {
    val isEditing: Boolean get() = entry != null
}

sealed interface FoodDetailIntent {
    data class Load(val foodId: String?, val mealId: String?, val slot: MealSlot, val session: Int = 0) : FoodDetailIntent
    data class UnitSelected(val unit: String) : FoodDetailIntent
    data class AmountChanged(val text: String) : FoodDetailIntent
    data class Step(val up: Boolean) : FoodDetailIntent
    data class SlotSelected(val slot: MealSlot) : FoodDetailIntent
    data object Save : FoodDetailIntent
    data object Delete : FoodDetailIntent
    data object DeleteCustomFood : FoodDetailIntent
}

sealed interface FoodDetailEffect {
    data object Saved : FoodDetailEffect
    data object Deleted : FoodDetailEffect
}

// Custom food

data class CustomFoodState(
    val slot: MealSlot = MealSlot.Lunch,
    val name: String = "",
    val servingUnit: String = FoodUnits.PORTION,
    val servingGrams: String = "",
    val calories: String = "",
    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
    val saving: Boolean = false,
    val invalid: Boolean = false,
    val failed: Boolean = false,
)

sealed interface CustomFoodIntent {
    data class Load(val slot: MealSlot, val session: Int = 0) : CustomFoodIntent
    data class NameChanged(val value: String) : CustomFoodIntent
    data class UnitSelected(val unit: String) : CustomFoodIntent
    data class GramsChanged(val value: String) : CustomFoodIntent
    data class CaloriesChanged(val value: String) : CustomFoodIntent
    data class ProteinChanged(val value: String) : CustomFoodIntent
    data class CarbsChanged(val value: String) : CustomFoodIntent
    data class FatChanged(val value: String) : CustomFoodIntent
    data object Save : CustomFoodIntent
}

sealed interface CustomFoodEffect {
    data class Saved(val foodId: String, val slot: MealSlot) : CustomFoodEffect
}
