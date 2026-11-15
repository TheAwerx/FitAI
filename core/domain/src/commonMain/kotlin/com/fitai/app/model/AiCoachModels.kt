package com.fitai.app.model

import com.fitai.app.stats.WeeklyStats
import kotlinx.serialization.Serializable

/** Property names are the Firebase callable wire format shared with `firebase/functions`. */
@Serializable
data class NextMealRequest(
    val profile: UserProfilePayload,
    val remainingCalories: Int,
    val remainingProteinG: Double,
    val remainingCarbsG: Double,
    val remainingFatG: Double,
    val mealsLoggedToday: List<String>,
    val isTrainingDay: Boolean,
    val locale: String,
)

@Serializable
data class NextMealSuggestion(
    val title: String,
    val description: String = "",
    val items: List<String> = emptyList(),
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
)

object TodayLocation {
    const val GYM = "gym"
    const val HOME = "home"
    const val HOTEL = "hotel"
    const val OUTDOOR = "outdoor"
}

@Serializable
data class TodayModeRequest(
    val profile: UserProfilePayload,
    val availableMinutes: Int,
    /** 1 (exhausted) to 5 (great). */
    val energy: Int,
    val painAreas: List<String>,
    /** One of [TodayLocation]. */
    val location: String,
    val plannedExercises: List<GeneratedExercise>,
    val locale: String,
)

@Serializable
data class AdaptedSession(
    val title: String,
    val note: String = "",
    val exercises: List<GeneratedExercise> = emptyList(),
)

@Serializable
data class CoachMessage(
    /** [ROLE_USER] or [ROLE_COACH]. */
    val role: String,
    val text: String,
) {
    companion object {
        const val ROLE_USER = "user"
        const val ROLE_COACH = "coach"
    }
}

@Serializable
data class CoachChatRequest(
    val profile: UserProfilePayload,
    val contextSummary: String,
    val history: List<CoachMessage>,
    val message: String,
    val locale: String,
)

@Serializable
data class CoachChatReply(
    val reply: String,
)

@Serializable
data class WeeklyReportRequest(
    val profile: UserProfilePayload,
    val stats: WeeklyStats,
    val locale: String,
)

@Serializable
data class WeeklyCoachReport(
    val headline: String,
    val summary: String = "",
    val highlights: List<String> = emptyList(),
    val nextWeekFocus: List<String> = emptyList(),
)
