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
import io.github.aakira.napier.Napier

/**
 * Dev builds never call the plan Cloud Functions. Workout and diet plans come from
 * [localPlans]; meal photo analysis stays on [remote]. Coach calls try [remote] first and
 * fall back to [localPlans] so dev keeps working before the callables are deployed.
 */
internal class DevPlanAIRepository(
    private val remote: AIRepository,
    private val localPlans: AIRepository,
) : AIRepository {

    override suspend fun generateWorkoutPlan(
        profile: UserProfilePayload,
        purpose: PlanGenerationPurpose,
    ): Result<GeneratedWorkoutPlan> = localPlans.generateWorkoutPlan(profile, purpose)

    override suspend fun generateDietPlan(profile: UserProfilePayload): Result<GeneratedDietPlan> =
        localPlans.generateDietPlan(profile)

    override suspend fun analyzeMealPhoto(
        imageBase64: String,
        mimeType: String,
        locale: String,
    ): Result<MealAnalysisResult> = remote.analyzeMealPhoto(imageBase64, mimeType, locale)

    override suspend fun suggestNextMeal(request: NextMealRequest): Result<NextMealSuggestion> =
        remoteOrLocal("suggestNextMeal", { remote.suggestNextMeal(request) }, { localPlans.suggestNextMeal(request) })

    override suspend fun adaptTodaySession(request: TodayModeRequest): Result<AdaptedSession> =
        remoteOrLocal("adaptTodaySession", { remote.adaptTodaySession(request) }, { localPlans.adaptTodaySession(request) })

    override suspend fun coachChat(request: CoachChatRequest): Result<CoachChatReply> =
        remoteOrLocal("coachChat", { remote.coachChat(request) }, { localPlans.coachChat(request) })

    override suspend fun weeklyCoachReport(request: WeeklyReportRequest): Result<WeeklyCoachReport> =
        remoteOrLocal("weeklyCoachReport", { remote.weeklyCoachReport(request) }, { localPlans.weeklyCoachReport(request) })

    private suspend fun <T> remoteOrLocal(
        name: String,
        remoteCall: suspend () -> Result<T>,
        localCall: suspend () -> Result<T>,
    ): Result<T> {
        val result = remoteCall()
        if (result.isSuccess) return result
        Napier.w("$name remote call failed; using offline answer", result.exceptionOrNull())
        return localCall()
    }
}
