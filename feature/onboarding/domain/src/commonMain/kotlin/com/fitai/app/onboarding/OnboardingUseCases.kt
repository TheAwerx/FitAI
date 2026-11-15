package com.fitai.app.onboarding

import com.fitai.app.access.AccessTier
import com.fitai.app.access.EntitlementRepository
import com.fitai.app.access.PlanGenerationPurpose
import com.fitai.app.ai.AIRepository
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.FocusArea
import com.fitai.app.model.OnboardingAnswers
import com.fitai.app.model.encodeProfileCodes
import com.fitai.app.model.toProfilePayload
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.preferences.onboardingAnswers
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.training.StarterPlans
import com.fitai.app.trial.TrialRepository
import com.fitai.app.util.appLocale
import com.fitai.app.util.todayLocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.random.Random

data class OnboardingProfileInput(
    val name: String,
    val age: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val activityLevel: String,
    val equipment: String,
    val dietRestrictions: String,
)

class SaveGuestProfileUseCase(
    private val localUserRepository: LocalUserRepository,
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(input: OnboardingProfileInput): UseCaseState<Unit> = useCaseCatching {
        val id = (1..32).joinToString("") { Random.nextInt(0, 16).toString(16) }
        val now = com.fitai.app.util.nowIsoTimestamp()
        localUserRepository.insertGuestProfile(
            id = id,
            name = input.name.trim(),
            // users.email is NOT NULL; the real address is collected later in Auth.
            email = "",
            age = input.age,
            heightCm = input.heightCm,
            weightKg = input.weightKg,
            goal = input.goal.trim(),
            activityLevel = input.activityLevel.trim(),
            equipment = input.equipment.trim(),
            dietRestrictions = input.dietRestrictions.trim(),
            createdAt = now,
            updatedAt = now,
        )
        trialRepository.ensureAnonymousAuth().getOrThrow()
    }
}

class SaveOnboardingAnswersUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(answers: OnboardingAnswers): UseCaseState<Unit> = useCaseCatching {
        require(answers.focusAreas.size <= FocusArea.MAX_SELECTED) { "too_many_focus_areas" }
        require(answers.daysPerWeek in StarterPlans.MIN_DAYS..StarterPlans.MAX_DAYS) { "invalid_days_per_week" }
        userPreferencesRepository.gender = answers.gender
        userPreferencesRepository.trainingExperience = answers.experience
        userPreferencesRepository.trainingObstacle = answers.obstacle
        userPreferencesRepository.focusAreas = encodeProfileCodes(answers.focusAreas).ifBlank { null }
        userPreferencesRepository.sessionMinutes = answers.sessionMinutes
        userPreferencesRepository.commitmentDaysPerWeek = answers.daysPerWeek
    }
}

enum class PlanKind { Personal, Starter }

/**
 * Assigns the first plans after sign-up. Pro users get AI plans built from their onboarding answers;
 * everyone else gets [StarterPlans]. [onProgress] reports 0..1 for the loading screen.
 */
class SetupInitialPlansUseCase(
    private val entitlementRepository: EntitlementRepository,
    private val aiRepository: AIRepository,
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(
        forceStarter: Boolean = false,
        onProgress: suspend (Float) -> Unit = {},
    ): UseCaseState<PlanKind> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val locale = appLocale()
        val payload = user.toProfilePayload(userPreferencesRepository.onboardingAnswers(), locale)
        val personal = !forceStarter && entitlementRepository.currentTier() == AccessTier.Pro
        onProgress(0.15f)
        if (personal) {
            val workout = aiRepository.generateWorkoutPlan(payload, PlanGenerationPurpose.Initial).getOrThrow()
            workoutRepository.saveGeneratedPlan(user.id, workout)
            onProgress(0.6f)
            val diet = aiRepository.generateDietPlan(payload).getOrThrow()
            nutritionRepository.saveGeneratedDiet(user.id, diet)
        } else {
            workoutRepository.saveGeneratedPlan(user.id, StarterPlans.workout(payload, locale))
            onProgress(0.6f)
            nutritionRepository.saveGeneratedDiet(user.id, StarterPlans.diet(payload))
        }
        onProgress(1f)
        if (personal) PlanKind.Personal else PlanKind.Starter
    }
}

/** Rough date the plan can deliver visible results by, shown on the onboarding result screen. */
fun estimatedGoalDate(goal: String, today: LocalDate = todayLocalDate()): LocalDate {
    val weeks = when (FitnessGoal.fromCode(goal)) {
        FitnessGoal.BuildMuscle -> 12
        FitnessGoal.LoseFat, FitnessGoal.GetStronger -> 10
        FitnessGoal.ImproveEndurance -> 8
        FitnessGoal.StayHealthy, null -> 6
    }
    return today.plus(DatePeriod(days = weeks * 7))
}

/** Profile name and the answers shown back on the result, paywall and commitment screens. */
data class OnboardingSummary(
    val name: String,
    val goal: String,
    val equipment: Set<String>,
    val answers: OnboardingAnswers,
)

class GetOnboardingSummaryUseCase(
    private val localUserRepository: LocalUserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): UseCaseState<OnboardingSummary> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        OnboardingSummary(
            name = user.name,
            goal = user.goal,
            equipment = com.fitai.app.model.decodeProfileCodes(user.equipment),
            answers = userPreferencesRepository.onboardingAnswers(),
        )
    }
}
