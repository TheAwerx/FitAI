package com.fitai.app.model

import kotlinx.serialization.Serializable

data class DietPlan(
    val id: String,
    val userId: String,
    val dailyCalories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val createdAt: String,
    val isActive: Boolean,
)

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
) {
    /** True for meals logged through photo analysis (see [ANALYZED_PHOTO_MARKER]). */
    val hasPhoto: Boolean get() = photoUrl != null

    companion object {
        /** Stored in `photo_url` for photo-analyzed meals; the image itself is not persisted. */
        const val ANALYZED_PHOTO_MARKER = "analyzed_photo"
    }
}

data class DailyMacroSummary(
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

@Serializable
data class GeneratedDietPlan(
    val dailyCalories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
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
