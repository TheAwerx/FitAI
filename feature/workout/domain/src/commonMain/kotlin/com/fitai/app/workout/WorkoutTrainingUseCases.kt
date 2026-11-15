package com.fitai.app.workout

import com.fitai.app.ai.AIRepository
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.Exercise
import com.fitai.app.model.GeneratedExercise
import com.fitai.app.model.TodayModeRequest
import com.fitai.app.model.WorkoutSession
import com.fitai.app.model.decodeProfileCodes
import com.fitai.app.model.toProfilePayload
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.training.EquipmentContext
import com.fitai.app.training.ExerciseAlternativesCatalog
import com.fitai.app.trial.TrialRepository
import com.fitai.app.util.appLocale
import com.fitai.app.util.currentDayOfWeek
import com.fitai.app.util.nowIsoTimestamp

data class ExerciseAlternativeOption(
    val name: String,
    val equipment: EquipmentContext,
)

/** Same-muscle alternatives in the app language; the user's equipment context comes first. */
class GetExerciseAlternativesUseCase(
    private val workoutRepository: WorkoutRepository,
    private val localUserRepository: LocalUserRepository,
) {
    suspend operator fun invoke(exerciseId: String): UseCaseState<List<ExerciseAlternativeOption>> = useCaseCatching {
        val exercise = workoutRepository.getExerciseById(exerciseId) ?: error("no_exercise")
        val equipment = localUserRepository.getLatestUser()?.equipment.orEmpty()
        val locale = appLocale()
        ExerciseAlternativesCatalog
            .alternativesFor(
                muscleGroup = exercise.muscleGroup,
                exerciseName = exercise.name,
                preferred = ExerciseAlternativesCatalog.contextFor(decodeProfileCodes(equipment)),
            )
            .map { ExerciseAlternativeOption(it.displayName(locale), it.equipment) }
    }
}

class ReplaceExerciseUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    /** Pass [sessionExerciseId] when replacing from an active session so the session follows the swap. */
    suspend operator fun invoke(
        exerciseId: String,
        newName: String,
        sessionExerciseId: String? = null,
    ): UseCaseState<Exercise> = useCaseCatching {
        require(newName.isNotBlank()) { "blank_name" }
        workoutRepository.replaceExercise(
            exerciseId = exerciseId,
            newName = newName.trim(),
            newNotes = null,
            sessionExerciseId = sessionExerciseId,
        )
    }
}

data class TodayModeInput(
    val availableMinutes: Int,
    val energy: Int,
    val painAreas: List<String>,
    /** One of [com.fitai.app.model.TodayLocation]. */
    val location: String,
)

data class TodayModeSession(
    val session: WorkoutSession,
    val title: String,
    val note: String,
)

/**
 * Adapts today's planned exercises (may be empty on a rest day), stores the result as an inactive
 * ad-hoc plan and starts a session from it. Quota gating and usage recording stay with the caller.
 */
class StartTodayModeSessionUseCase(
    private val aiRepository: AIRepository,
    private val workoutRepository: WorkoutRepository,
    private val localUserRepository: LocalUserRepository,
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(input: TodayModeInput): UseCaseState<TodayModeSession> = useCaseCatching {
        trialRepository.ensureAnonymousAuth().getOrThrow()
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val day = currentDayOfWeek()
        val planned = workoutRepository.getActivePlan(user.id)
            ?.let { workoutRepository.getExercisesForPlanDay(it.id, day) }
            .orEmpty()
        val adapted = aiRepository.adaptTodaySession(
            TodayModeRequest(
                profile = user.toProfilePayload(),
                availableMinutes = input.availableMinutes,
                energy = input.energy.coerceIn(1, 5),
                painAreas = input.painAreas,
                location = input.location,
                plannedExercises = planned.map { it.toGenerated() },
                locale = appLocale(),
            ),
        ).getOrThrow()
        check(adapted.exercises.isNotEmpty()) { "no_exercises" }

        val plan = workoutRepository.saveAdHocPlan(user.id, adapted, day)
        val aiOrder = adapted.exercises.map { it.name }
        val exercises = workoutRepository.getExercisesForPlanDay(plan.id, day)
            .sortedBy { exercise -> aiOrder.indexOf(exercise.name).let { if (it < 0) Int.MAX_VALUE else it } }
        val session = workoutRepository.startSession(
            planId = plan.id,
            exerciseIds = exercises.map { it.id },
            startedAt = nowIsoTimestamp(),
        )
        TodayModeSession(session = session, title = adapted.title, note = adapted.note)
    }

    private fun Exercise.toGenerated() = GeneratedExercise(
        dayOfWeek = dayOfWeek,
        name = name,
        sets = sets,
        reps = reps,
        restSeconds = restSeconds,
        muscleGroup = muscleGroup,
        notes = notes,
    )
}

data class SessionSummary(
    val sessionId: String,
    val durationMinutes: Int,
    val totalVolumeKg: Double,
    val completedSets: Int,
    val totalSets: Int,
    val exerciseCount: Int,
)

class GetSessionSummaryUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionId: String): UseCaseState<SessionSummary> = useCaseCatching {
        val session = workoutRepository.getSession(sessionId) ?: error("no_session")
        val sessionExercises = workoutRepository.getSessionExercises(sessionId)
        val sets = sessionExercises.flatMap { workoutRepository.getSessionSets(it.id) }
        val completed = sets.filter { it.completed }
        SessionSummary(
            sessionId = sessionId,
            durationMinutes = session.durationMinutes ?: 0,
            totalVolumeKg = completed.sumOf { set ->
                val reps = leadingNumber.find(set.actualReps ?: set.targetReps)?.value?.toIntOrNull() ?: 0
                (set.weightKg ?: 0.0) * reps
            },
            completedSets = completed.size,
            totalSets = sets.size,
            exerciseCount = sessionExercises.count { se -> completed.any { it.sessionExerciseId == se.id } },
        )
    }

    private companion object {
        val leadingNumber = Regex("""\d+""")
    }
}
