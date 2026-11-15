package com.fitai.app.ai

import com.fitai.app.access.PlanGenerationPurpose
import com.fitai.app.firebase.WorkoutPlanCallableRequest
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
import com.fitai.app.network.ApiMethod
import com.fitai.app.network.ApiRequest

internal object AiApi {
    fun generateWorkoutPlan(profile: UserProfilePayload, purpose: PlanGenerationPurpose) = ApiRequest<GeneratedWorkoutPlan>(
        method = ApiMethod.CALLABLE,
        path = "generateWorkoutPlan",
        requestBody = WorkoutPlanCallableRequest(profile = profile, planPurpose = purpose.code),
    )

    fun generateDietPlan(body: UserProfilePayload) = ApiRequest<GeneratedDietPlan>(
        method = ApiMethod.CALLABLE,
        path = "generateDietPlan",
        requestBody = body,
    )

    fun analyzeMealPhoto(imageBase64: String, mimeType: String, locale: String) = ApiRequest<MealAnalysisResult>(
        method = ApiMethod.CALLABLE,
        path = "analyzeMealPhoto",
        requestBody = mapOf(
            "imageBase64" to imageBase64,
            "mimeType" to mimeType,
            "locale" to locale,
        ),
    )

    fun suggestNextMeal(body: NextMealRequest) = ApiRequest<NextMealSuggestion>(
        method = ApiMethod.CALLABLE,
        path = "suggestNextMeal",
        requestBody = body,
    )

    fun adaptTodaySession(body: TodayModeRequest) = ApiRequest<AdaptedSession>(
        method = ApiMethod.CALLABLE,
        path = "adaptTodaySession",
        requestBody = body,
    )

    fun coachChat(body: CoachChatRequest) = ApiRequest<CoachChatReply>(
        method = ApiMethod.CALLABLE,
        path = "coachChat",
        requestBody = body,
    )

    fun weeklyCoachReport(body: WeeklyReportRequest) = ApiRequest<WeeklyCoachReport>(
        method = ApiMethod.CALLABLE,
        path = "weeklyCoachReport",
        requestBody = body,
    )
}
