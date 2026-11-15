package com.fitai.app.viewmodel.nutrition

import com.fitai.app.access.FeatureAccess
import com.fitai.app.model.DietPlan
import com.fitai.app.model.Macros
import com.fitai.app.model.MealLog
import com.fitai.app.model.MealSlot
import com.fitai.app.model.NextMealSuggestion
import com.fitai.app.nutrition.DiarySlot
import com.fitai.app.nutrition.TodayNutritionOverview

data class MealFormFields(
    val mealName: String = "",
    val calories: String = "",
    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
)

sealed interface PlatePhotoState {
    data object Idle : PlatePhotoState
    data object Analyzing : PlatePhotoState
    data object Failed : PlatePhotoState
    data class Review(
        val fields: MealFormFields,
        val saving: Boolean = false,
        val error: ReviewError? = null,
    ) : PlatePhotoState
}

enum class ReviewError { Invalid, SaveFailed }

sealed interface NextMealState {
    data object Hidden : NextMealState
    data object Loading : NextMealState
    data object Locked : NextMealState
    data object Failed : NextMealState
    data class Ready(val suggestion: NextMealSuggestion) : NextMealState
}

data class NutritionState(
    val activePlan: DietPlan? = null,
    val planHistory: List<DietPlan> = emptyList(),
    val todayMeals: List<MealLog> = emptyList(),
    val today: TodayNutritionOverview? = null,
    /** Today's meals in plan order, each with its plan and logged entries. */
    val diary: List<DiarySlot> = emptyList(),
    /** Sum of the selected plan options; null when the plan has targets only. */
    val planTotals: Macros? = null,
    /**
     * Slots whose plan block the user toggled. A plan starts open while nothing is logged for the meal and
     * closed once something is.
     */
    val toggledPlans: Set<String> = emptySet(),
    /** Slot with a running "ate as planned" or swap write. */
    val busySlot: String? = null,
    /** Null until the first access check returns; `remaining` is non-null only for Free. */
    val photoAccess: FeatureAccess? = null,
    val plate: PlatePhotoState = PlatePhotoState.Idle,
    val nextMeal: NextMealState = NextMealState.Hidden,
    val loading: Boolean = false,
    val generating: Boolean = false,
    val errorMessage: String? = null,
)

data class MealLogState(
    /** Set when editing an existing photo or quick-add entry. */
    val editingId: String? = null,
    val mealName: String = "",
    val calories: String = "",
    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
    val slot: MealSlot = MealSlot.Lunch,
    val saving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface NutritionIntent {
    data object Refresh : NutritionIntent
    data object RefreshAccess : NutritionIntent
    data object RegeneratePlan : NutritionIntent
    data object OpenManualLog : NutritionIntent
    data object OpenPaywall : NutritionIntent
    class PhotoPicked(val bytes: ByteArray, val mimeType: String) : NutritionIntent
    data object PhotoPickCancelled : NutritionIntent
    data class ReviewChanged(val fields: MealFormFields) : NutritionIntent
    data object SaveAnalyzedMeal : NutritionIntent
    data object DiscardAnalysis : NutritionIntent
    data object RetryNextMeal : NutritionIntent
    data class AddFood(val slot: MealSlot) : NutritionIntent
    data class OpenEntry(val meal: MealLog) : NutritionIntent
    data class DeleteEntry(val mealId: String) : NutritionIntent
    data class LogPlanned(val slot: MealSlot) : NutritionIntent
    data class SwapOption(val slot: MealSlot, val optionIndex: Int) : NutritionIntent
    data class TogglePlan(val slot: MealSlot) : NutritionIntent
}

sealed interface MealLogIntent {
    data class Load(val mealId: String?, val slot: MealSlot?, val session: Int = 0) : MealLogIntent
    data class MealNameChanged(val value: String) : MealLogIntent
    data class CaloriesChanged(val value: String) : MealLogIntent
    data class ProteinChanged(val value: String) : MealLogIntent
    data class CarbsChanged(val value: String) : MealLogIntent
    data class FatChanged(val value: String) : MealLogIntent
    data class SlotSelected(val slot: MealSlot) : MealLogIntent
    data object Save : MealLogIntent
    data object Delete : MealLogIntent
    data class ApplyAnalysis(
        val mealName: String,
        val calories: Int,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
    ) : MealLogIntent
}

sealed interface NutritionEffect {
    data class NavigateToMealLog(val slot: MealSlot? = null, val mealId: String? = null) : NutritionEffect
    data object NavigateToPaywall : NutritionEffect
    data class NavigateToFoodSearch(val slot: MealSlot) : NutritionEffect
    data class NavigateToFoodDetail(val foodId: String?, val slot: MealSlot, val mealId: String?) : NutritionEffect
    data object MealSaved : NutritionEffect
    data object PlannedMealLogged : NutritionEffect
}
