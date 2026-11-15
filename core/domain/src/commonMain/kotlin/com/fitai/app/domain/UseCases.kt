package com.fitai.app.domain

import com.fitai.app.access.PlanGenerationPurpose
import com.fitai.app.ai.AIRepository
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.GeneratedWorkoutPlan
import com.fitai.app.model.MealAnalysisResult
import com.fitai.app.model.toProfilePayload
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.preferences.onboardingAnswers
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.trial.TrialRepository

class GenerateWorkoutPlanUseCase(
    private val aiRepository: AIRepository,
    private val workoutRepository: WorkoutRepository,
    private val localUserRepository: LocalUserRepository,
    private val trialRepository: TrialRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    /** Regenerations go through `RegenerateWorkoutPlanUseCase`, which spends the plan quota. */
    suspend operator fun invoke(
        purpose: PlanGenerationPurpose = PlanGenerationPurpose.Regenerate,
    ): UseCaseState<GeneratedWorkoutPlan> = useCaseCatching {
        trialRepository.ensureAnonymousAuth().getOrThrow()
        check(trialRepository.canAccessAiFeatures().getOrThrow()) { "trial_expired" }
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val payload = user.toProfilePayload(userPreferencesRepository.onboardingAnswers())
        val generated = aiRepository.generateWorkoutPlan(payload, purpose).getOrThrow()
        workoutRepository.saveGeneratedPlan(user.id, generated)
        generated
    }
}

class GenerateDietPlanUseCase(
    private val aiRepository: AIRepository,
    private val nutritionRepository: NutritionRepository,
    private val localUserRepository: LocalUserRepository,
    private val trialRepository: TrialRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): UseCaseState<GeneratedDietPlan> = useCaseCatching {
        trialRepository.ensureAnonymousAuth().getOrThrow()
        check(trialRepository.canAccessAiFeatures().getOrThrow()) { "trial_expired" }
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val payload = user.toProfilePayload(userPreferencesRepository.onboardingAnswers())
        val generated = aiRepository.generateDietPlan(payload).getOrThrow()
        nutritionRepository.saveGeneratedDiet(user.id, generated)
        generated
    }
}

class AnalyzeMealPhotoUseCase(
    private val aiRepository: AIRepository,
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(
        imageBase64: String,
        mimeType: String = "image/jpeg",
    ): UseCaseState<MealAnalysisResult> = useCaseCatching {
        trialRepository.ensureAnonymousAuth().getOrThrow()
        check(trialRepository.canAccessAiFeatures().getOrThrow()) { "trial_expired" }
        aiRepository.analyzeMealPhoto(imageBase64, mimeType).getOrThrow()
    }
}

class LogMealUseCase(
    private val nutritionRepository: NutritionRepository,
    private val localUserRepository: LocalUserRepository,
) {
    suspend operator fun invoke(
        mealName: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        loggedAt: String = com.fitai.app.util.nowIsoTimestamp(),
    ): UseCaseState<Unit> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        nutritionRepository.logMeal(
            userId = user.id,
            mealName = mealName,
            calories = calories,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            loggedAt = loggedAt,
        )
    }
}

class StartWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository,
    private val localUserRepository: LocalUserRepository,
) {
    suspend operator fun invoke(dayOfWeek: Int): UseCaseState<com.fitai.app.model.WorkoutSession> =
        useCaseCatching {
            val user = localUserRepository.getLatestUser() ?: error("no_user")
            val plan = workoutRepository.getActivePlan(user.id) ?: error("no_plan")
            val exercises = workoutRepository.getExercisesForPlanDay(plan.id, dayOfWeek)
            check(exercises.isNotEmpty()) { "no_exercises" }
            workoutRepository.startSession(
                planId = plan.id,
                exerciseIds = exercises.map { it.id },
                startedAt = com.fitai.app.util.nowIsoTimestamp(),
            )
        }
}

class CompleteSetUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(
        setId: String,
        actualReps: String?,
        weightKg: Double?,
    ): UseCaseState<Unit> = useCaseCatching {
        workoutRepository.completeSet(
            setId = setId,
            actualReps = actualReps,
            weightKg = weightKg,
            completedAt = com.fitai.app.util.nowIsoTimestamp(),
        )
    }
}

class CheckTrialAccessUseCase(
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(): UseCaseState<Boolean> =
        trialRepository.canAccessAiFeatures().toUseCaseState()
}

class InitGuestTrialUseCase(
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(): UseCaseState<com.fitai.app.model.TrialStatus> =
        trialRepository.initGuestTrial().toUseCaseState()
}
