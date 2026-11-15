package com.fitai.app.repository

import com.fitai.app.model.AdaptedSession
import com.fitai.app.model.CompletedSetRecord
import com.fitai.app.model.Exercise
import com.fitai.app.model.GeneratedWorkoutPlan
import com.fitai.app.model.SessionExercise
import com.fitai.app.model.SessionSet
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun observePlansForUser(userId: String): Flow<List<WorkoutPlan>>
    fun observeActivePlan(userId: String): Flow<WorkoutPlan?>
    suspend fun getActivePlan(userId: String): WorkoutPlan?
    suspend fun getExercisesForPlan(planId: String): List<Exercise>
    suspend fun getExercisesForPlanDay(planId: String, dayOfWeek: Int): List<Exercise>
    suspend fun getExerciseById(exerciseId: String): Exercise?
    suspend fun saveGeneratedPlan(userId: String, generated: GeneratedWorkoutPlan): WorkoutPlan
    suspend fun startSession(planId: String, exerciseIds: List<String>, startedAt: String): WorkoutSession
    suspend fun getSessionExercises(sessionId: String): List<SessionExercise>
    suspend fun getSessionSets(sessionExerciseId: String): List<SessionSet>
    suspend fun completeSet(
        setId: String,
        actualReps: String?,
        weightKg: Double?,
        completedAt: String,
    )
    suspend fun completeSession(
        sessionId: String,
        completedAt: String,
        durationMinutes: Int,
        notes: String?,
    )
    suspend fun getSession(sessionId: String): WorkoutSession?
    /** Completed sessions whose start is in [start] (inclusive) .. [end] (exclusive). */
    suspend fun getCompletedSessionsInRange(userId: String, start: String, end: String): List<WorkoutSession>
    /** Sum of weight × reps for completed sets of sessions started in range; reps fall back to target reps. */
    suspend fun getCompletedVolumeInRange(userId: String, start: String, end: String): Double
    suspend fun getPlanById(planId: String): WorkoutPlan?
    /** Every completed set of the user (any session), newest session first. */
    suspend fun getCompletedSetHistory(userId: String): List<CompletedSetRecord>
    /**
     * Renames a plan exercise. When past sessions reference it, a new exercise row is created
     * instead so history keeps the old name. [sessionExerciseId] is re-pointed to the result.
     */
    suspend fun replaceExercise(
        exerciseId: String,
        newName: String,
        newNotes: String?,
        sessionExerciseId: String? = null,
    ): Exercise
    /** Inactive one-off plan (see [WorkoutPlan.isAdHoc]); the active plan is left untouched. */
    suspend fun saveAdHocPlan(userId: String, adapted: AdaptedSession, dayOfWeek: Int): WorkoutPlan
}
