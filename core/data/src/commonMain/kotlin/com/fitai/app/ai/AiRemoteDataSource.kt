package com.fitai.app.ai

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.firebase.FitAiFunctions
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
import com.fitai.app.network.ApiRequest

class AiRemoteDataSource {
    private val initialized: Boolean by lazy { FirebaseBootstrap.ensureInitialized() }

    suspend fun generateWorkoutPlan(request: ApiRequest<GeneratedWorkoutPlan>): GeneratedWorkoutPlan {
        check(initialized) { "Firebase is not configured" }
        return FitAiFunctions.generateWorkoutPlan(request.requestBody as WorkoutPlanCallableRequest)
    }

    suspend fun generateDietPlan(request: ApiRequest<GeneratedDietPlan>): GeneratedDietPlan {
        check(initialized) { "Firebase is not configured" }
        val profile = request.requestBody as UserProfilePayload
        return FitAiFunctions.generateDietPlan(profile)
    }

    suspend fun analyzeMealPhoto(request: ApiRequest<MealAnalysisResult>): MealAnalysisResult {
        check(initialized) { "Firebase is not configured" }
        @Suppress("UNCHECKED_CAST")
        val payload = request.requestBody as Map<String, String>
        return FitAiFunctions.analyzeMealPhoto(
            imageBase64 = payload.getValue("imageBase64"),
            mimeType = payload["mimeType"] ?: "image/jpeg",
            locale = payload["locale"] ?: "tr",
        )
    }

    suspend fun suggestNextMeal(request: ApiRequest<NextMealSuggestion>): NextMealSuggestion {
        check(initialized) { "Firebase is not configured" }
        return FitAiFunctions.suggestNextMeal(request.requestBody as NextMealRequest)
    }

    suspend fun adaptTodaySession(request: ApiRequest<AdaptedSession>): AdaptedSession {
        check(initialized) { "Firebase is not configured" }
        return FitAiFunctions.adaptTodaySession(request.requestBody as TodayModeRequest)
    }

    suspend fun coachChat(request: ApiRequest<CoachChatReply>): CoachChatReply {
        check(initialized) { "Firebase is not configured" }
        return FitAiFunctions.coachChat(request.requestBody as CoachChatRequest)
    }

    suspend fun weeklyCoachReport(request: ApiRequest<WeeklyCoachReport>): WeeklyCoachReport {
        check(initialized) { "Firebase is not configured" }
        return FitAiFunctions.weeklyCoachReport(request.requestBody as WeeklyReportRequest)
    }
}
