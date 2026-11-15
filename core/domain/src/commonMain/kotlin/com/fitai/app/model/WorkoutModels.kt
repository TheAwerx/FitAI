package com.fitai.app.model

import kotlinx.serialization.Serializable

data class WorkoutPlan(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val daysPerWeek: Int,
    val createdAt: String,
    val isActive: Boolean,
) {
    /** Today Mode one-off plans; never active and hidden from plan lists. */
    val isAdHoc: Boolean get() = daysPerWeek == AD_HOC_DAYS_PER_WEEK

    companion object {
        const val AD_HOC_DAYS_PER_WEEK = 0
    }
}

data class Exercise(
    val id: String,
    val planId: String,
    val dayOfWeek: Int,
    val name: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val muscleGroup: String,
    val notes: String?,
)

data class WorkoutSession(
    val id: String,
    val planId: String,
    val startedAt: String,
    val completedAt: String?,
    val durationMinutes: Int?,
    val notes: String?,
)

data class SessionExercise(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val orderIndex: Int,
)

data class SessionSet(
    val id: String,
    val sessionExerciseId: String,
    val setNumber: Int,
    val targetReps: String,
    val actualReps: String?,
    val weightKg: Double?,
    val completed: Boolean,
    val completedAt: String?,
)

/** A completed set joined with its session and exercise, used for overload and PR history. */
data class CompletedSetRecord(
    val sessionId: String,
    val sessionStartedAt: String,
    val sessionExerciseId: String,
    val exerciseName: String,
    val muscleGroup: String,
    val setNumber: Int,
    val targetReps: String,
    val actualReps: String?,
    val weightKg: Double?,
    val completedAt: String?,
)

@Serializable
data class GeneratedWorkoutPlan(
    val title: String,
    val description: String,
    val daysPerWeek: Int,
    val exercises: List<GeneratedExercise>,
)

@Serializable
data class GeneratedExercise(
    val dayOfWeek: Int,
    val name: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val muscleGroup: String,
    val notes: String? = null,
)
