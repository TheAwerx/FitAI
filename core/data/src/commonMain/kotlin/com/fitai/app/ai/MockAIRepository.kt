package com.fitai.app.ai

import com.fitai.app.model.AdaptedSession
import com.fitai.app.model.CoachChatReply
import com.fitai.app.model.CoachChatRequest
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.GeneratedWorkoutPlan
import com.fitai.app.model.MealAnalysisResult
import com.fitai.app.model.NextMealRequest
import com.fitai.app.model.NextMealSuggestion
import com.fitai.app.model.TodayModeRequest
import com.fitai.app.model.UserProfilePayload
import com.fitai.app.model.WeeklyCoachReport
import com.fitai.app.model.WeeklyReportRequest
import com.fitai.app.training.StarterPlans
import com.fitai.app.util.appLocale

/**
 * Offline / dev stand-in. Plans come from [StarterPlans], which uses every onboarding answer in the payload
 * (days, session length, equipment, experience, goal, obstacle, focus, gender, activity, body, diet).
 */
class MockAIRepository : AIRepository {

    override suspend fun generateWorkoutPlan(profile: UserProfilePayload): Result<GeneratedWorkoutPlan> =
        runCatching { StarterPlans.workout(profile, profile.locale ?: appLocale(), personal = true) }

    override suspend fun generateDietPlan(profile: UserProfilePayload): Result<GeneratedDietPlan> =
        runCatching { StarterPlans.diet(profile) }

    override suspend fun analyzeMealPhoto(
        imageBase64: String,
        mimeType: String,
        locale: String,
    ): Result<MealAnalysisResult> =
        Result.success(
            MealAnalysisResult(
                mealName = if (locale == "en") "Meal" else "Öğün",
                calories = 450,
                proteinG = 25.0,
                carbsG = 40.0,
                fatG = 18.0,
            ),
        )

    override suspend fun suggestNextMeal(request: NextMealRequest): Result<NextMealSuggestion> =
        runCatching { MockCoachAnswers.suggestNextMeal(request) }

    override suspend fun adaptTodaySession(request: TodayModeRequest): Result<AdaptedSession> =
        runCatching { MockCoachAnswers.adaptTodaySession(request) }

    override suspend fun coachChat(request: CoachChatRequest): Result<CoachChatReply> =
        runCatching { MockCoachAnswers.coachChat(request) }

    override suspend fun weeklyCoachReport(request: WeeklyReportRequest): Result<WeeklyCoachReport> =
        runCatching { MockCoachAnswers.weeklyCoachReport(request) }
}