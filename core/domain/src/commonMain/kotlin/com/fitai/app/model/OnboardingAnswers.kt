package com.fitai.app.model

enum class Gender(val code: String) {
    Male("male"),
    Female("female"),
    ;

    companion object {
        fun fromCode(code: String?): Gender? = entries.find { it.code == code }
    }
}

enum class TrainingExperience(val code: String) {
    None("none"),
    Beginner("beginner"),
    Intermediate("intermediate"),
    Advanced("advanced"),
    ;

    companion object {
        fun fromCode(code: String?): TrainingExperience? = entries.find { it.code == code }
    }
}

enum class TrainingObstacle(val code: String) {
    Time("time"),
    Knowledge("knowledge"),
    Motivation("motivation"),
    Injury("injury"),
    Equipment("equipment"),
    NeverTried("never_tried"),
}

/** What the user wants FitAI for; at most [MAX_SELECTED] in onboarding. */
enum class FocusArea(val code: String) {
    PersonalPlan("personal_plan"),
    SmartOverload("smart_overload"),
    PlatePhoto("plate_photo"),
    LogLifts("log_lifts"),
    Streak("streak"),
    AiCoach("ai_coach"),
    ;

    companion object {
        const val MAX_SELECTED = 3
    }
}

object SessionLength {
    val options = listOf(15, 30, 45, 60, 90)
    const val DEFAULT = 45
}

/** Extra onboarding answers kept in preferences (the `users` table has no columns for them). */
data class OnboardingAnswers(
    val gender: String?,
    val experience: String?,
    val obstacle: String?,
    val focusAreas: Set<String>,
    val daysPerWeek: Int,
    val sessionMinutes: Int,
)

fun LocalUserProfile.toProfilePayload(answers: OnboardingAnswers?) = toProfilePayload().copy(
    gender = answers?.gender,
    experience = answers?.experience,
    daysPerWeek = answers?.daysPerWeek,
    sessionMinutes = answers?.sessionMinutes,
)
