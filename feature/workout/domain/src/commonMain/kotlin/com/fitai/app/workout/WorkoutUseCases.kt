package com.fitai.app.workout

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.Exercise
import com.fitai.app.model.SessionExercise
import com.fitai.app.model.SessionSet
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.util.currentDayOfWeek
import com.fitai.app.util.epochMillis
import com.fitai.app.util.nowIsoTimestamp
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest

data class WorkoutOverview(
    val plans: List<WorkoutPlan>,
    val activePlan: WorkoutPlan?,
    val selectedDay: Int,
    val dayExercises: List<Exercise>,
)

class ObserveWorkoutOverviewUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(selectedDay: Int = currentDayOfWeek()): Flow<WorkoutOverview> {
        val user = localUserRepository.getLatestUser() ?: return flowOf(
            WorkoutOverview(emptyList(), null, selectedDay, emptyList()),
        )
        return workoutRepository.observePlansForUser(user.id).mapLatest { allPlans ->
            val plans = allPlans.filterNot { it.isAdHoc }
            val active = plans.firstOrNull { it.isActive }
            val exercises = active?.let { workoutRepository.getExercisesForPlanDay(it.id, selectedDay) }
                .orEmpty()
            WorkoutOverview(plans, active, selectedDay, exercises)
        }
    }
}

class GetExercisesForDayUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(planId: String, day: Int): UseCaseState<List<Exercise>> =
        useCaseCatching { workoutRepository.getExercisesForPlanDay(planId, day) }
}

class GetExerciseByIdUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(exerciseId: String): UseCaseState<Exercise?> =
        useCaseCatching { workoutRepository.getExerciseById(exerciseId) }
}

data class ActiveSessionSnapshot(
    val session: WorkoutSession,
    val sessionExercises: List<SessionExercise>,
    val exerciseDetails: Map<String, Exercise>,
    val setsBySessionExercise: Map<String, List<SessionSet>>,
    val plan: WorkoutPlan? = null,
)

class LoadActiveSessionUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionId: String): UseCaseState<ActiveSessionSnapshot?> =
        useCaseCatching {
            val session = workoutRepository.getSession(sessionId) ?: return@useCaseCatching null
            val sessionExercises = workoutRepository.getSessionExercises(sessionId)
            val details = sessionExercises.associate { se ->
                se.exerciseId to workoutRepository.getExerciseById(se.exerciseId)
            }.filterValues { it != null }.mapValues { it.value!! }
            val sets = sessionExercises.associate { se ->
                se.id to workoutRepository.getSessionSets(se.id)
            }
            ActiveSessionSnapshot(session, sessionExercises, details, sets, workoutRepository.getPlanById(session.planId))
        }
}

class RefreshSessionSetsUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionExerciseId: String): UseCaseState<List<SessionSet>> =
        useCaseCatching { workoutRepository.getSessionSets(sessionExerciseId) }
}

class FinishWorkoutSessionUseCase(
    private val workoutRepository: WorkoutRepository,
) {
    /** [durationMinutes] null → minutes since the session's `startedAt`. */
    suspend operator fun invoke(
        sessionId: String,
        durationMinutes: Int? = null,
        notes: String? = null,
    ): UseCaseState<Unit> = useCaseCatching {
        val minutes = durationMinutes ?: workoutRepository.getSession(sessionId)
            ?.let { minutesSince(it.startedAt) }
            ?: 1
        workoutRepository.completeSession(
            sessionId = sessionId,
            completedAt = nowIsoTimestamp(),
            durationMinutes = minutes.coerceAtLeast(1),
            notes = notes,
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun minutesSince(iso: String): Int? =
        runCatching { ((epochMillis() - Instant.parse(iso).toEpochMilliseconds()) / 60_000).toInt() }.getOrNull()
}
