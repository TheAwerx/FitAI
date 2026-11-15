package com.fitai.app.nutrition

import com.fitai.app.access.CheckFeatureAccessUseCase
import com.fitai.app.access.FeatureAccess
import com.fitai.app.access.GatedFeature
import com.fitai.app.access.RecordFeatureUseUseCase
import com.fitai.app.ai.AIRepository
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.MealAnalysisResult
import com.fitai.app.model.MealLog
import com.fitai.app.model.NextMealRequest
import com.fitai.app.model.NextMealSuggestion
import com.fitai.app.model.toProfilePayload
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.trial.TrialRepository
import com.fitai.app.util.appLocale
import com.fitai.app.util.nowIsoTimestamp
import kotlin.io.encoding.Base64
import kotlin.math.roundToInt

sealed interface MealPhotoAnalysisOutcome {
    data class Analyzed(val result: MealAnalysisResult) : MealPhotoAnalysisOutcome
    data class Locked(val access: FeatureAccess) : MealPhotoAnalysisOutcome
}

sealed interface NextMealOutcome {
    data class Suggested(val suggestion: NextMealSuggestion, val isTrainingDay: Boolean) : NextMealOutcome
    data class Locked(val access: FeatureAccess) : NextMealOutcome
}

/** Gated by [GatedFeature.MealPhotoAnalysis]; a use is recorded only after a successful analysis. */
class AnalyzeMealPhotoWithQuotaUseCase(
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val recordFeatureUseUseCase: RecordFeatureUseUseCase,
    private val trackEventUseCase: TrackEventUseCase,
    private val aiRepository: AIRepository,
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(imageBytes: ByteArray, mimeType: String): UseCaseState<MealPhotoAnalysisOutcome> {
        val access = when (val check = checkFeatureAccessUseCase(GatedFeature.MealPhotoAnalysis)) {
            is UseCaseState.Success -> check.data
            is UseCaseState.Error -> return check
        }
        if (!access.allowed) return UseCaseState.Success(MealPhotoAnalysisOutcome.Locked(access))
        return useCaseCatching {
            trialRepository.ensureAnonymousAuth().getOrThrow()
            val result = aiRepository.analyzeMealPhoto(Base64.encode(imageBytes), mimeType, appLocale()).getOrThrow()
            recordFeatureUseUseCase(GatedFeature.MealPhotoAnalysis)
            trackEventUseCase(
                AnalyticsEvents.MEAL_PHOTO_ANALYZED,
                mapOf("tier" to access.tier.name.lowercase(), "calories" to result.calories.toString()),
            )
            MealPhotoAnalysisOutcome.Analyzed(result)
        }
    }
}

class LogAnalyzedMealUseCase(
    private val nutritionRepository: NutritionRepository,
    private val localUserRepository: LocalUserRepository,
) {
    suspend operator fun invoke(
        mealName: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
    ): UseCaseState<MealLog> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        nutritionRepository.logMeal(
            userId = user.id,
            mealName = mealName,
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            loggedAt = nowIsoTimestamp(),
            photoUrl = MealLog.ANALYZED_PHOTO_MARKER,
        )
    }
}

/** Gated by [GatedFeature.NextMealSuggestion] (Pro/Trial only); fails with "no_plan" without an active diet. */
class SuggestNextMealUseCase(
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val getTodayNutritionOverviewUseCase: GetTodayNutritionOverviewUseCase,
    private val trackEventUseCase: TrackEventUseCase,
    private val localUserRepository: LocalUserRepository,
    private val aiRepository: AIRepository,
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(): UseCaseState<NextMealOutcome> {
        val access = when (val check = checkFeatureAccessUseCase(GatedFeature.NextMealSuggestion)) {
            is UseCaseState.Success -> check.data
            is UseCaseState.Error -> return check
        }
        if (!access.allowed) return UseCaseState.Success(NextMealOutcome.Locked(access))
        val overview = when (val today = getTodayNutritionOverviewUseCase()) {
            is UseCaseState.Success -> today.data
            is UseCaseState.Error -> return today
        }
        return useCaseCatching {
            val remaining = overview.remaining ?: error("no_plan")
            val user = localUserRepository.getLatestUser() ?: error("no_user")
            trialRepository.ensureAnonymousAuth().getOrThrow()
            val suggestion = aiRepository.suggestNextMeal(
                NextMealRequest(
                    profile = user.toProfilePayload(),
                    remainingCalories = remaining.calories,
                    remainingProteinG = remaining.proteinG.roundToOneDecimal(),
                    remainingCarbsG = remaining.carbsG.roundToOneDecimal(),
                    remainingFatG = remaining.fatG.roundToOneDecimal(),
                    mealsLoggedToday = overview.meals.map { it.mealName },
                    isTrainingDay = overview.isTrainingDay,
                    locale = appLocale(),
                ),
            ).getOrThrow()
            trackEventUseCase(
                AnalyticsEvents.NEXT_MEAL_SUGGESTED,
                mapOf(
                    "tier" to access.tier.name.lowercase(),
                    "training_day" to overview.isTrainingDay.toString(),
                ),
            )
            NextMealOutcome.Suggested(suggestion, overview.isTrainingDay)
        }
    }

    private fun Double.roundToOneDecimal(): Double = (this * 10).roundToInt() / 10.0
}
