package com.fitai.app.repository

import com.fitai.app.db.Diet_plans
import com.fitai.app.db.Exercises
import com.fitai.app.db.Meal_logs
import com.fitai.app.db.Session_exercises
import com.fitai.app.db.Session_sets
import com.fitai.app.db.Workout_plans
import com.fitai.app.db.Workout_sessions
import com.fitai.app.model.DietPlan
import com.fitai.app.model.Exercise
import com.fitai.app.model.MealLog
import com.fitai.app.model.SessionExercise
import com.fitai.app.model.SessionSet
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession

internal fun Workout_plans.toDomain(): WorkoutPlan = WorkoutPlan(
    id = id,
    userId = user_id,
    title = title,
    description = description,
    daysPerWeek = days_per_week.toInt(),
    createdAt = created_at,
    isActive = is_active == 1L,
)

internal fun Exercises.toDomain(): Exercise = Exercise(
    id = id,
    planId = plan_id,
    dayOfWeek = day_of_week.toInt(),
    name = name,
    sets = sets.toInt(),
    reps = reps,
    restSeconds = rest_seconds.toInt(),
    muscleGroup = muscle_group,
    notes = notes,
)

internal fun Workout_sessions.toDomain(): WorkoutSession = WorkoutSession(
    id = id,
    planId = plan_id,
    startedAt = started_at,
    completedAt = completed_at,
    durationMinutes = duration_minutes?.toInt(),
    notes = notes,
)

internal fun Session_exercises.toDomain(): SessionExercise = SessionExercise(
    id = id,
    sessionId = session_id,
    exerciseId = exercise_id,
    orderIndex = order_index.toInt(),
)

internal fun Session_sets.toDomain(): SessionSet = SessionSet(
    id = id,
    sessionExerciseId = session_exercise_id,
    setNumber = set_number.toInt(),
    targetReps = target_reps,
    actualReps = actual_reps,
    weightKg = weight_kg,
    completed = completed == 1L,
    completedAt = completed_at,
)

internal fun Diet_plans.toDomain(): DietPlan = DietPlan(
    id = id,
    userId = user_id,
    dailyCalories = daily_calories.toInt(),
    proteinG = protein_g.toInt(),
    carbsG = carbs_g.toInt(),
    fatG = fat_g.toInt(),
    createdAt = created_at,
    isActive = is_active == 1L,
)

internal fun Meal_logs.toDomain(): MealLog = MealLog(
    id = id,
    userId = user_id,
    mealName = meal_name,
    calories = calories.toInt(),
    proteinG = protein_g,
    carbsG = carbs_g,
    fatG = fat_g,
    photoUrl = photo_url,
    loggedAt = logged_at,
)
