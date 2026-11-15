package com.fitai.app.ai

import com.fitai.app.access.PlanGenerationPurpose
import com.fitai.app.access.PlanQuotaErrors
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

class FirebaseAIRepository(
    private val remote: AiRemoteDataSource,
) : AIRepository {

    override suspend fun generateWorkoutPlan(
        profile: UserProfilePayload,
        purpose: PlanGenerationPurpose,
    ): Result<GeneratedWorkoutPlan> =
        runCatching { remote.generateWorkoutPlan(AiApi.generateWorkoutPlan(profile, purpose)) }
            .recoverCatching { error -> throw error.asPlanQuotaError() }

    override suspend fun generateDietPlan(profile: UserProfilePayload): Result<GeneratedDietPlan> =
        runCatching { remote.generateDietPlan(AiApi.generateDietPlan(profile)) }

    override suspend fun analyzeMealPhoto(
        imageBase64: String,
        mimeType: String,
        locale: String,
    ): Result<MealAnalysisResult> =
        runCatching {
            remote.analyzeMealPhoto(AiApi.analyzeMealPhoto(imageBase64, mimeType, locale))
        }

    override suspend fun suggestNextMeal(request: NextMealRequest): Result<NextMealSuggestion> =
        runCatching { remote.suggestNextMeal(AiApi.suggestNextMeal(request)) }

    override suspend fun adaptTodaySession(request: TodayModeRequest): Result<AdaptedSession> =
        runCatching { remote.adaptTodaySession(AiApi.adaptTodaySession(request)) }

    override suspend fun coachChat(request: CoachChatRequest): Result<CoachChatReply> =
        runCatching { remote.coachChat(AiApi.coachChat(request)) }

    override suspend fun weeklyCoachReport(request: WeeklyReportRequest): Result<WeeklyCoachReport> =
        runCatching { remote.weeklyCoachReport(AiApi.weeklyCoachReport(request)) }
}

/** The callable reports quota problems as `HttpsError` messages; surface them as the bare domain code. */
private fun Throwable.asPlanQuotaError(): Throwable {
    val text = message.orEmpty()
    val code = PlanQuotaErrors.all.firstOrNull { text.contains(it) } ?: return this
    return IllegalStateException(code, this)
}
