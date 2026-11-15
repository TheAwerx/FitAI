package com.fitai.app.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.fitai.app.db.FitAiDatabase
import com.fitai.app.db.Workout_sessions
import com.fitai.app.model.AdaptedSession
import com.fitai.app.model.CompletedSetRecord
import com.fitai.app.model.Exercise
import com.fitai.app.model.GeneratedExercise
import com.fitai.app.model.GeneratedWorkoutPlan
import com.fitai.app.model.SessionExercise
import com.fitai.app.model.SessionSet
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.random.Random

class WorkoutRepositoryImpl(
    private val database: FitAiDatabase,
) : WorkoutRepository {

    override fun observePlansForUser(userId: String): Flow<List<WorkoutPlan>> =
        database.workoutQueries
            .selectPlansForUser(userId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { plans -> plans.map { it.toDomain() } }

    override fun observeActivePlan(userId: String): Flow<WorkoutPlan?> =
        database.workoutQueries
            .selectActivePlan(userId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toDomain() }

    override suspend fun getActivePlan(userId: String): WorkoutPlan? = withContext(Dispatchers.Default) {
        database.workoutQueries.selectActivePlan(userId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getExercisesForPlan(planId: String): List<Exercise> = withContext(Dispatchers.Default) {
        database.workoutQueries.selectExercisesForPlan(planId).executeAsList().map { it.toDomain() }
    }

    override suspend fun getExercisesForPlanDay(planId: String, dayOfWeek: Int): List<Exercise> =
        withContext(Dispatchers.Default) {
            database.workoutQueries.selectExercisesForPlanDay(planId, dayOfWeek.toLong())
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun getExerciseById(exerciseId: String): Exercise? = withContext(Dispatchers.Default) {
        database.workoutQueries.selectExerciseById(exerciseId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun saveGeneratedPlan(userId: String, generated: GeneratedWorkoutPlan): WorkoutPlan =
        withContext(Dispatchers.Default) {
            val planId = newId()
            val now = com.fitai.app.util.nowIsoTimestamp()
            database.workoutQueries.deactivatePlansForUser(userId)
            database.workoutQueries.insertPlan(
                id = planId,
                user_id = userId,
                title = generated.title,
                description = generated.description,
                days_per_week = generated.daysPerWeek.toLong(),
                created_at = now,
                is_active = 1,
            )
            generated.exercises.forEach { exercise ->
                database.workoutQueries.insertExercise(
                    id = newId(),
                    plan_id = planId,
                    day_of_week = exercise.dayOfWeek.toLong(),
                    name = exercise.name,
                    sets = exercise.sets.toLong(),
                    reps = exercise.reps,
                    rest_seconds = exercise.restSeconds.toLong(),
                    muscle_group = exercise.muscleGroup,
                    notes = exercise.notes,
                )
            }
            WorkoutPlan(
                id = planId,
                userId = userId,
                title = generated.title,
                description = generated.description,
                daysPerWeek = generated.daysPerWeek,
                createdAt = now,
                isActive = true,
            )
        }

    override suspend fun startSession(
        planId: String,
        exerciseIds: List<String>,
        startedAt: String,
    ): WorkoutSession = withContext(Dispatchers.Default) {
        val sessionId = newId()
        database.workoutQueries.insertSession(
            id = sessionId,
            plan_id = planId,
            started_at = startedAt,
            completed_at = null,
            duration_minutes = null,
            notes = null,
        )
        exerciseIds.forEachIndexed { index, exerciseId ->
            val sessionExerciseId = newId()
            database.workoutQueries.insertSessionExercise(
                id = sessionExerciseId,
                session_id = sessionId,
                exercise_id = exerciseId,
                order_index = index.toLong(),
            )
            val exercise = database.workoutQueries.selectExerciseById(exerciseId).executeAsOne()
            repeat(exercise.sets.toInt()) { setIndex ->
                database.workoutQueries.insertSessionSet(
                    id = newId(),
                    session_exercise_id = sessionExerciseId,
                    set_number = (setIndex + 1).toLong(),
                    target_reps = exercise.reps,
                    actual_reps = null,
                    weight_kg = null,
                    completed = 0,
                    completed_at = null,
                )
            }
        }
        WorkoutSession(
            id = sessionId,
            planId = planId,
            startedAt = startedAt,
            completedAt = null,
            durationMinutes = null,
            notes = null,
        )
    }

    override suspend fun getSessionExercises(sessionId: String): List<SessionExercise> =
        withContext(Dispatchers.Default) {
            database.workoutQueries.selectSessionExercises(sessionId).executeAsList().map { it.toDomain() }
        }

    override suspend fun getSessionSets(sessionExerciseId: String): List<SessionSet> =
        withContext(Dispatchers.Default) {
            database.workoutQueries.selectSessionSets(sessionExerciseId).executeAsList().map { it.toDomain() }
        }

    override suspend fun completeSet(
        setId: String,
        actualReps: String?,
        weightKg: Double?,
        completedAt: String,
    ) = withContext(Dispatchers.Default) {
        database.workoutQueries.updateSessionSet(
            actual_reps = actualReps,
            weight_kg = weightKg,
            completed = 1,
            completed_at = completedAt,
            id = setId,
        )
        Unit
    }

    override suspend fun completeSession(
        sessionId: String,
        completedAt: String,
        durationMinutes: Int,
        notes: String?,
    ) = withContext(Dispatchers.Default) {
        database.workoutQueries.updateSessionComplete(
            completed_at = completedAt,
            duration_minutes = durationMinutes.toLong(),
            notes = notes,
            id = sessionId,
        )
        Unit
    }

    override suspend fun getSession(sessionId: String): WorkoutSession? = withContext(Dispatchers.Default) {
        database.workoutQueries.selectSessionById(sessionId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getCompletedSessionsInRange(
        userId: String,
        start: String,
        end: String,
    ): List<WorkoutSession> = withContext(Dispatchers.Default) {
        database.workoutQueries
            .selectCompletedSessionsForUserInRange(userId, start, end, ::Workout_sessions)
            .executeAsList()
            .map { it.toDomain() }
    }

    override suspend fun getCompletedVolumeInRange(userId: String, start: String, end: String): Double =
        withContext(Dispatchers.Default) {
            database.workoutQueries.selectCompletedVolumeForUserInRange(userId, start, end).executeAsOne()
        }

    override suspend fun getPlanById(planId: String): WorkoutPlan? = withContext(Dispatchers.Default) {
        database.workoutQueries.selectPlanById(planId).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun getCompletedSetHistory(userId: String): List<CompletedSetRecord> =
        withContext(Dispatchers.Default) {
            database.workoutQueries.selectCompletedSetHistoryForUser(userId).executeAsList().map { row ->
                CompletedSetRecord(
                    sessionId = row.session_id,
                    sessionStartedAt = row.started_at,
                    sessionExerciseId = row.session_exercise_id,
                    exerciseName = row.exercise_name,
                    muscleGroup = row.muscle_group,
                    setNumber = row.set_number.toInt(),
                    targetReps = row.target_reps,
                    actualReps = row.actual_reps,
                    weightKg = row.weight_kg,
                    completedAt = row.completed_at,
                )
            }
        }

    override suspend fun replaceExercise(
        exerciseId: String,
        newName: String,
        newNotes: String?,
        sessionExerciseId: String?,
    ): Exercise = withContext(Dispatchers.Default) {
        val queries = database.workoutQueries
        database.transactionWithResult {
            val current = queries.selectExerciseById(exerciseId).executeAsOne()
            val referenced = queries.countSessionReferencesForExercise(exerciseId).executeAsOne() > 0
            val targetId = if (referenced) {
                val copyId = newId()
                queries.insertExercise(
                    id = copyId,
                    plan_id = current.plan_id,
                    day_of_week = current.day_of_week,
                    name = newName,
                    sets = current.sets,
                    reps = current.reps,
                    rest_seconds = current.rest_seconds,
                    muscle_group = current.muscle_group,
                    notes = newNotes,
                )
                queries.archiveExercise(exerciseId)
                copyId
            } else {
                queries.updateExerciseNameAndNotes(name = newName, notes = newNotes, id = exerciseId)
                exerciseId
            }
            if (sessionExerciseId != null) {
                queries.updateSessionExerciseExercise(exercise_id = targetId, id = sessionExerciseId)
            }
            queries.selectExerciseById(targetId).executeAsOne().toDomain()
        }
    }

    override suspend fun saveAdHocPlan(userId: String, adapted: AdaptedSession, dayOfWeek: Int): WorkoutPlan =
        withContext(Dispatchers.Default) {
            val planId = newId()
            val now = com.fitai.app.util.nowIsoTimestamp()
            database.transaction {
                database.workoutQueries.insertPlan(
                    id = planId,
                    user_id = userId,
                    title = adapted.title,
                    description = adapted.note,
                    days_per_week = WorkoutPlan.AD_HOC_DAYS_PER_WEEK.toLong(),
                    created_at = now,
                    is_active = 0,
                )
                adapted.exercises.forEach { exercise ->
                    database.workoutQueries.insertExercise(
                        id = newId(),
                        plan_id = planId,
                        day_of_week = dayOfWeek.toLong(),
                        name = exercise.name,
                        sets = exercise.sets.toLong(),
                        reps = exercise.reps,
                        rest_seconds = exercise.restSeconds.toLong(),
                        muscle_group = exercise.muscleGroup,
                        notes = exercise.notes,
                    )
                }
            }
            WorkoutPlan(
                id = planId,
                userId = userId,
                title = adapted.title,
                description = adapted.note,
                daysPerWeek = WorkoutPlan.AD_HOC_DAYS_PER_WEEK,
                createdAt = now,
                isActive = false,
            )
        }

    private fun newId(): String = buildString {
        repeat(16) { append(Random.nextInt(16).toString(16)) }
    }
}
