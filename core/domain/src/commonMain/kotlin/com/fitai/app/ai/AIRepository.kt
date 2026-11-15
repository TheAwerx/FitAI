package com.fitai.app.ai

import com.fitai.app.access.PlanGenerationPurpose
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

interface AIRepository {
    /** [purpose] tells the server whether this call spends a regeneration right (see `PlanQuotaPolicy`). */
    suspend fun generateWorkoutPlan(
        profile: UserProfilePayload,
        purpose: PlanGenerationPurpose = PlanGenerationPurpose.Regenerate,
    ): Result<GeneratedWorkoutPlan>
    suspend fun generateDietPlan(profile: UserProfilePayload): Result<GeneratedDietPlan>
    suspend fun analyzeMealPhoto(
        imageBase64: String,
        mimeType: String,
        locale: String = "tr",
    ): Result<MealAnalysisResult>
    suspend fun suggestNextMeal(request: NextMealRequest): Result<NextMealSuggestion>
    suspend fun adaptTodaySession(request: TodayModeRequest): Result<AdaptedSession>
    suspend fun coachChat(request: CoachChatRequest): Result<CoachChatReply>
    suspend fun weeklyCoachReport(request: WeeklyReportRequest): Result<WeeklyCoachReport>
}
