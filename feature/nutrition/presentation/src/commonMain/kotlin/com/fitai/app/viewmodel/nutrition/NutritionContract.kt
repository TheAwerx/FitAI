package com.fitai.app.viewmodel.nutrition

import com.fitai.app.access.FeatureAccess
import com.fitai.app.model.DietPlan
import com.fitai.app.model.MealLog
import com.fitai.app.model.NextMealSuggestion
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
    /** Null until the first access check returns; `remaining` is non-null only for Free. */
    val photoAccess: FeatureAccess? = null,
    val plate: PlatePhotoState = PlatePhotoState.Idle,
    val nextMeal: NextMealState = NextMealState.Hidden,
    val loading: Boolean = false,
    val generating: Boolean = false,
    val errorMessage: String? = null,
)

data class MealLogState(
    val mealName: String = "",
    val calories: String = "",
    val proteinG: String = "",
    val carbsG: String = "",
    val fatG: String = "",
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
}

sealed interface MealLogIntent {
    data class MealNameChanged(val value: String) : MealLogIntent
    data class CaloriesChanged(val value: String) : MealLogIntent
    data class ProteinChanged(val value: String) : MealLogIntent
    data class CarbsChanged(val value: String) : MealLogIntent
    data class FatChanged(val value: String) : MealLogIntent
    data object Save : MealLogIntent
    data class ApplyAnalysis(
        val mealName: String,
        val calories: Int,
        val proteinG: Double,
        val carbsG: Double,
        val fatG: Double,
    ) : MealLogIntent
}

sealed interface NutritionEffect {
    data object NavigateToMealLog : NutritionEffect
    data object NavigateToPaywall : NutritionEffect
    data object MealSaved : NutritionEffect
}
