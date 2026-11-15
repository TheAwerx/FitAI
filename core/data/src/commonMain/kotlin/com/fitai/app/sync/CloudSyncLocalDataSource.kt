package com.fitai.app.sync

import com.fitai.app.db.FitAiDatabase
import com.fitai.app.db.Workout_sessions
import com.fitai.app.model.BodyMeasurement
import com.fitai.app.model.DietPlan
import com.fitai.app.model.Exercise
import com.fitai.app.model.LocalUserProfile
import com.fitai.app.model.MealLog
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.repository.toDomain
import com.fitai.app.repository.toLocalUserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Device-side reads for the cloud copy, and the inserts that restore it (ids are kept). */
internal class CloudSyncLocalDataSource(
    private val database: FitAiDatabase,
) {
    private val workout get() = database.workoutQueries
    private val nutrition get() = database.nutritionQueries
    private val progress get() = database.progressQueries

    suspend fun latestUser(): LocalUserProfile? = io {
        database.usersQueries.selectLatestUser().executeAsOneOrNull()?.toLocalUserProfile()
    }

    suspend fun workoutPlan(planId: String): Pair<WorkoutPlan, List<Exercise>>? = io {
        workout.selectPlanById(planId).executeAsOneOrNull()?.toDomain()?.let { it to exercisesOf(it.id) }
    }

    suspend fun workoutPlans(userId: String): List<Pair<WorkoutPlan, List<Exercise>>> = io {
        workout.selectPlansForUser(userId).executeAsList().map { row -> row.toDomain().let { it to exercisesOf(it.id) } }
    }

    suspend fun activeWorkoutPlanId(userId: String): String? = io {
        workout.selectActivePlan(userId).executeAsOneOrNull()?.id
    }

    suspend fun dietPlan(planId: String): DietPlan? = io {
        nutrition.selectDietPlanById(planId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun dietPlans(userId: String): List<DietPlan> = io {
        nutrition.selectPlansForUser(userId).executeAsList().map { it.toDomain() }
    }

    suspend fun activeDietPlanId(userId: String): String? = io {
        nutrition.selectActiveDiet(userId).executeAsOneOrNull()?.id
    }

    suspend fun session(sessionId: String): SessionSnapshot? = io {
        workout.selectSessionById(sessionId).executeAsOneOrNull()?.let(::snapshotOf)
    }

    suspend fun latestSession(userId: String): SessionSnapshot? = io {
        workout.selectLatestSessionForUser(userId, ::Workout_sessions).executeAsOneOrNull()?.let(::snapshotOf)
    }

    suspend fun sessions(userId: String): List<SessionSnapshot> = io {
        workout.selectSessionsForUser(userId, ::Workout_sessions).executeAsList().map(::snapshotOf)
    }

    suspend fun meal(mealId: String): MealLog? = io {
        nutrition.selectMealLogById(mealId).executeAsOneOrNull()?.toDomain()
    }

    suspend fun meals(userId: String): List<MealLog> = io {
        nutrition.selectMealsForUser(userId).executeAsList().map { it.toDomain() }
    }

    suspend fun measurement(id: String): BodyMeasurement? = io {
        progress.selectMeasurementById(id).executeAsOneOrNull()?.toDomain()
    }

    suspend fun measurements(userId: String): List<BodyMeasurement> = io {
        progress.selectMeasurementsForUser(userId).executeAsList().map { it.toDomain() }
    }

    /**
     * Inserts every cloud row missing on the device, in one transaction. With [keepLocalPlans] restored
     * plans are inactive; otherwise the newest active cloud plan (and diet) becomes the active one.
     */
    suspend fun restore(localUserId: String, cloud: CloudDataSnapshot, keepLocalPlans: Boolean): CloudRestoreSummary = io {
        database.transactionWithResult {
            val activeProgramId = if (keepLocalPlans) null else newestActiveId(
                cloud.workoutPlans.filter { it.daysPerWeek > 0 },
                isActive = { it.isActive },
                createdAt = { it.createdAt },
                id = { it.id },
            )
            val activeDietId = if (keepLocalPlans) null else newestActiveId(
                cloud.dietPlans,
                isActive = { it.isActive },
                createdAt = { it.createdAt },
                id = { it.id },
            )
            val restoresActiveProgram = activeProgramId != null && workout.selectPlanById(activeProgramId).executeAsOneOrNull() == null
            val restoresActiveDiet = activeDietId != null && nutrition.selectDietPlanById(activeDietId).executeAsOneOrNull() == null
            if (restoresActiveProgram) workout.deactivatePlansForUser(localUserId)
            if (restoresActiveDiet) nutrition.deactivateDietPlansForUser(localUserId)

            var plans = 0
            cloud.workoutPlans.filter { it.id.isNotBlank() }.forEach { doc ->
                if (workout.selectPlanById(doc.id).executeAsOneOrNull() == null) {
                    workout.insertPlan(
                        id = doc.id,
                        user_id = localUserId,
                        title = doc.title,
                        description = doc.description,
                        days_per_week = doc.daysPerWeek.toLong(),
                        created_at = doc.createdAt,
                        is_active = if (doc.id == activeProgramId) 1 else 0,
                    )
                    plans++
                }
                doc.exercises.filter { it.id.isNotBlank() }.forEach { insertExerciseIfMissing(it.toExercise(doc.id)) }
            }

            var diets = 0
            cloud.dietPlans.filter { it.id.isNotBlank() }.forEach { doc ->
                if (nutrition.selectDietPlanById(doc.id).executeAsOneOrNull() == null) {
                    nutrition.insertDietPlan(
                        id = doc.id,
                        user_id = localUserId,
                        daily_calories = doc.dailyCalories.toLong(),
                        protein_g = doc.proteinG.toLong(),
                        carbs_g = doc.carbsG.toLong(),
                        fat_g = doc.fatG.toLong(),
                        created_at = doc.createdAt,
                        is_active = if (doc.id == activeDietId) 1 else 0,
                    )
                    diets++
                }
            }

            var sessions = 0
            cloud.sessions.filter { it.id.isNotBlank() }.forEach { doc ->
                val known = workout.selectSessionById(doc.id).executeAsOneOrNull() != null
                // Sessions are listed through their plan, so one without a plan row can't be shown.
                val planExists = workout.selectPlanById(doc.planId).executeAsOneOrNull() != null
                if (known || !planExists) return@forEach
                workout.insertSession(
                    id = doc.id,
                    plan_id = doc.planId,
                    started_at = doc.startedAt,
                    completed_at = doc.completedAt,
                    duration_minutes = doc.durationMinutes?.toLong(),
                    notes = doc.notes,
                )
                doc.exercises.filter { it.sessionExerciseId.isNotBlank() }.forEach { item ->
                    insertExerciseIfMissing(
                        Exercise(
                            id = item.exerciseId,
                            planId = doc.planId,
                            dayOfWeek = ARCHIVED_DAY,
                            name = item.name,
                            sets = item.sets.size,
                            reps = item.sets.firstOrNull()?.targetReps.orEmpty(),
                            restSeconds = 0,
                            muscleGroup = item.muscleGroup,
                            notes = null,
                        ),
                    )
                    workout.insertSessionExercise(
                        id = item.sessionExerciseId,
                        session_id = doc.id,
                        exercise_id = item.exerciseId,
                        order_index = item.orderIndex.toLong(),
                    )
                    item.sets.filter { it.id.isNotBlank() }.forEach { set ->
                        workout.insertSessionSet(
                            id = set.id,
                            session_exercise_id = item.sessionExerciseId,
                            set_number = set.setNumber.toLong(),
                            target_reps = set.targetReps,
                            actual_reps = set.actualReps,
                            weight_kg = set.weightKg,
                            completed = if (set.completed) 1 else 0,
                            completed_at = set.completedAt,
                        )
                    }
                }
                sessions++
            }

            var meals = 0
            cloud.meals.filter { it.id.isNotBlank() }.forEach { doc ->
                if (nutrition.selectMealLogById(doc.id).executeAsOneOrNull() != null) return@forEach
                val meal = doc.toMealLog(localUserId)
                nutrition.insertMealLog(
                    id = meal.id,
                    user_id = localUserId,
                    meal_name = meal.mealName,
                    calories = meal.calories.toLong(),
                    protein_g = meal.proteinG,
                    carbs_g = meal.carbsG,
                    fat_g = meal.fatG,
                    photo_url = meal.photoUrl,
                    logged_at = meal.loggedAt,
                )
                meals++
            }

            var measurements = 0
            cloud.measurements.filter { it.id.isNotBlank() }.forEach { doc ->
                if (progress.selectMeasurementById(doc.id).executeAsOneOrNull() != null) return@forEach
                val m = doc.toMeasurement(localUserId)
                progress.insertMeasurement(
                    id = m.id,
                    user_id = localUserId,
                    weight_kg = m.weightKg,
                    body_fat_pct = m.bodyFatPct,
                    chest_cm = m.chestCm,
                    waist_cm = m.waistCm,
                    hip_cm = m.hipCm,
                    arm_cm = m.armCm,
                    photo_url = m.photoUrl,
                    measured_at = m.measuredAt,
                )
                measurements++
            }

            CloudRestoreSummary(
                workoutPlans = plans,
                dietPlans = diets,
                sessions = sessions,
                meals = meals,
                measurements = measurements,
                activeWorkoutPlanRestored = restoresActiveProgram,
                activeDietPlanRestored = restoresActiveDiet,
            )
        }
    }

    private fun exercisesOf(planId: String): List<Exercise> =
        workout.selectAllExercisesForPlan(planId).executeAsList().map { it.toDomain() }

    private fun snapshotOf(row: Workout_sessions): SessionSnapshot {
        val session = row.toDomain()
        val exercises = workout.selectSessionExercises(session.id).executeAsList().map { entryRow ->
            val entry = entryRow.toDomain()
            SessionExerciseSnapshot(
                entry = entry,
                exercise = workout.selectExerciseById(entry.exerciseId).executeAsOneOrNull()?.toDomain(),
                sets = workout.selectSessionSets(entry.id).executeAsList().map { it.toDomain() },
            )
        }
        return SessionSnapshot(session, exercises)
    }

    private fun insertExerciseIfMissing(exercise: Exercise) {
        if (exercise.id.isBlank() || workout.selectExerciseById(exercise.id).executeAsOneOrNull() != null) return
        workout.insertExercise(
            id = exercise.id,
            plan_id = exercise.planId,
            day_of_week = exercise.dayOfWeek.toLong(),
            name = exercise.name,
            sets = exercise.sets.toLong(),
            reps = exercise.reps,
            rest_seconds = exercise.restSeconds.toLong(),
            muscle_group = exercise.muscleGroup,
            notes = exercise.notes,
        )
    }

    private suspend fun <T> io(block: () -> T): T = withContext(Dispatchers.Default) { block() }

    private companion object {
        /** Same marker `archiveExercise` uses: hidden from plan days, kept for history. */
        const val ARCHIVED_DAY = -1
    }
}
