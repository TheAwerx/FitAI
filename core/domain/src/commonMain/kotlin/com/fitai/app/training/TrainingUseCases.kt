package com.fitai.app.training

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.WorkoutRepository

/** Suggestion per session exercise id, based on earlier sessions of the same exercise name. */
class GetSetSuggestionsUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionId: String): UseCaseState<Map<String, SetSuggestion>> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val history = workoutRepository.getCompletedSetHistory(user.id)
        workoutRepository.getSessionExercises(sessionId).mapNotNull { sessionExercise ->
            val exercise = workoutRepository.getExerciseById(sessionExercise.exerciseId) ?: return@mapNotNull null
            sessionExercise.id to ProgressiveOverload.suggestFromHistory(
                exerciseName = exercise.name,
                targetReps = exercise.reps,
                muscleGroup = exercise.muscleGroup,
                history = history,
                excludeSessionId = sessionId,
            )
        }.toMap()
    }
}

class DetectSessionPRsUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(sessionId: String): UseCaseState<List<PersonalRecord>> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        PersonalRecords.detectForSession(workoutRepository.getCompletedSetHistory(user.id), sessionId)
    }
}

/** Best set (highest e1RM) per exercise name, highest first. */
class GetPersonalRecordsUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(): UseCaseState<List<PersonalRecord>> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        PersonalRecords.bestPerExercise(workoutRepository.getCompletedSetHistory(user.id))
    }
}
