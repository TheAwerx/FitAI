package com.fitai.app.firebase

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
import com.fitai.app.trial.TrialDeviceRequest
import com.fitai.app.trial.TrialStatusDto
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.functions.FirebaseFunctions
import dev.gitlive.firebase.functions.functions
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

internal object FitAiFunctions {
    const val REGION = "us-central1"
    private val timeout = 120.seconds

    /** The detailed diet plan (meals with alternatives) takes longer to generate. */
    private val dietTimeout = 240.seconds

    fun client(): FirebaseFunctions = Firebase.functions(REGION)

    suspend fun generateWorkoutPlan(request: WorkoutPlanCallableRequest): GeneratedWorkoutPlan =
        call("generateWorkoutPlan", request)

    suspend fun generateDietPlan(profile: UserProfilePayload): GeneratedDietPlan =
        client().httpsCallable("generateDietPlan", dietTimeout).invoke(profile).data()

    suspend fun analyzeMealPhoto(imageBase64: String, mimeType: String, locale: String): MealAnalysisResult =
        call("analyzeMealPhoto", MealPhotoCallableRequest(imageBase64, mimeType, locale))

    suspend fun suggestNextMeal(request: NextMealRequest): NextMealSuggestion =
        call("suggestNextMeal", request)

    suspend fun adaptTodaySession(request: TodayModeRequest): AdaptedSession =
        call("adaptTodaySession", request)

    suspend fun coachChat(request: CoachChatRequest): CoachChatReply =
        call("coachChat", request)

    suspend fun weeklyCoachReport(request: WeeklyReportRequest): WeeklyCoachReport =
        call("weeklyCoachReport", request)

    suspend fun getTrialStatus(request: TrialDeviceRequest): TrialStatusDto = call("getTrialStatus", request)

    suspend fun startTrial(request: TrialDeviceRequest): TrialStatusDto = call("startTrial", request)

    private suspend inline fun <reified Req : Any, reified Res> call(name: String, data: Req): Res =
        client().httpsCallable(name, timeout).invoke(data).data()
}

/** `planPurpose` is a `PlanGenerationPurpose` code; the server unwraps [profile] before prompting. */
@Serializable
internal data class WorkoutPlanCallableRequest(
    val profile: UserProfilePayload,
    val planPurpose: String,
)

@Serializable
internal data class MealPhotoCallableRequest(
    val imageBase64: String,
    val mimeType: String,
    val locale: String,
)
