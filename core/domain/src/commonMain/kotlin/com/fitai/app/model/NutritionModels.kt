package com.fitai.app.model

import kotlinx.serialization.Serializable

/** [meals] is empty for plans saved before detailed plans existed (targets only). */
data class DietPlan(
    val id: String,
    val userId: String,
    val dailyCalories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val createdAt: String,
    val isActive: Boolean,
    val meals: List<PlanMeal> = emptyList(),
)

/**
 * One diary entry. Food database entries carry [foodId], [servingUnit], [servingAmount] and [grams] so they
 * can be edited; photo and quick-add entries only have the totals. [slot] is a [MealSlot] code (null for
 * entries logged before slots existed; they are placed by time of day).
 */
data class MealLog(
    val id: String,
    val userId: String,
    val mealName: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val photoUrl: String?,
    val loggedAt: String,
    val slot: String? = null,
    val foodId: String? = null,
    val servingUnit: String? = null,
    val servingAmount: Double? = null,
    val grams: Double? = null,
) {
    /** True for meals logged through photo analysis (see [ANALYZED_PHOTO_MARKER]). */
    val hasPhoto: Boolean get() = photoUrl != null

    /** Logged with an amount (food database or plan), so the amount can be changed later. */
    val isPortioned: Boolean get() = servingUnit != null && servingAmount != null && (grams ?: 0.0) > 0.0

    companion object {
        /** Stored in `photo_url` for photo-analyzed meals; the image itself is not persisted. */
        const val ANALYZED_PHOTO_MARKER = "analyzed_photo"
    }
}

/** A food to add to the diary; [calories] and macros are for the whole amount. */
data class MealEntryDraft(
    val name: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val slot: String?,
    val foodId: String? = null,
    val servingUnit: String? = null,
    val servingAmount: Double? = null,
    val grams: Double? = null,
    val photoUrl: String? = null,
)

data class DailyMacroSummary(
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

/**
 * A planned food. [amount] of [unit] (a [FoodUnits] code) weighs [grams]; [calories] and macros are for that
 * amount. [foodId] links to the food database when the food is known there.
 */
@Serializable
data class PlanFood(
    val foodId: String? = null,
    val name: String = "",
    val amount: Double = 0.0,
    val unit: String = FoodUnits.GRAM,
    val grams: Double = 0.0,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
)

/** One way to eat a meal: the main suggestion or an alternative. [note] is a short preparation tip. */
@Serializable
data class PlanMealOption(
    val title: String = "",
    val note: String = "",
    val foods: List<PlanFood> = emptyList(),
) {
    val macros: Macros
        get() = foods.map { Macros(it.calories.toDouble(), it.proteinG, it.carbsG, it.fatG) }.sum()
}

/**
 * A meal of the plan at [time] ("HH:mm"). [options] holds the main suggestion first, then 1–2 alternatives;
 * [selected] is the one the user eats (swapping changes it).
 */
@Serializable
data class PlanMeal(
    val slot: String = MealSlot.Breakfast.code,
    val time: String = "",
    val options: List<PlanMealOption> = emptyList(),
    val selected: Int = 0,
) {
    val current: PlanMealOption? get() = options.getOrNull(selected) ?: options.firstOrNull()

    val alternatives: List<IndexedValue<PlanMealOption>>
        get() = options.withIndex().filter { it.index != selectedIndex }

    val selectedIndex: Int get() = if (selected in options.indices) selected else 0

    val mealSlot: MealSlot? get() = MealSlot.fromCode(slot)
}

/**
 * Callable response of `generateDietPlan` and the starter plan. Older servers return only the targets
 * ([meals] empty); the app then builds the meals from the food database.
 */
@Serializable
data class GeneratedDietPlan(
    val dailyCalories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val meals: List<PlanMeal> = emptyList(),
)

@Serializable
data class MealAnalysisResult(
    val mealName: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

/**
 * Profile sent to the plan and coach callables. [equipment] and [dietRestrictions] are comma-joined codes
 * (see `ProfileEnums`); the optional fields are the extra onboarding answers in `OnboardingAnswers`.
 * [locale] ("tr" / "en") is the language of the generated plan text.
 */
@Serializable
data class UserProfilePayload(
    val name: String,
    val age: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val activityLevel: String,
    val equipment: String,
    val dietRestrictions: String,
    val gender: String? = null,
    val experience: String? = null,
    val daysPerWeek: Int? = null,
    val sessionMinutes: Int? = null,
    val obstacle: String? = null,
    val focusAreas: List<String>? = null,
    val locale: String? = null,
)
