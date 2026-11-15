package com.fitai.app.sync

import com.fitai.app.model.AdaptedSession
import com.fitai.app.model.BodyMeasurement
import com.fitai.app.model.CustomFoodDraft
import com.fitai.app.model.DietPlan
import com.fitai.app.model.Exercise
import com.fitai.app.model.FoodItem
import com.fitai.app.model.GeneratedDietPlan
import com.fitai.app.model.GeneratedWorkoutPlan
import com.fitai.app.model.MealEntryDraft
import com.fitai.app.model.MealLog
import com.fitai.app.model.PlanMeal
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.model.WorkoutSession
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.repository.FoodRepository
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.ProgressRepository
import com.fitai.app.repository.WorkoutRepository

/*
 * Decorators that keep the device repositories as the source of truth and tell [CloudSyncScheduler]
 * which row changed after each successful local write. Reads go straight to the wrapped repository.
 */

internal class SyncingWorkoutRepository(
    private val delegate: WorkoutRepository,
    private val sync: CloudSyncScheduler,
) : WorkoutRepository by delegate {

    override suspend fun saveGeneratedPlan(userId: String, generated: GeneratedWorkoutPlan): WorkoutPlan {
        val previous = delegate.getActivePlan(userId)
        val plan = delegate.saveGeneratedPlan(userId, generated)
        previous?.let { sync.workoutPlanChanged(it.id) }
        sync.workoutPlanChanged(plan.id)
        return plan
    }

    override suspend fun saveAdHocPlan(userId: String, adapted: AdaptedSession, dayOfWeek: Int): WorkoutPlan =
        delegate.saveAdHocPlan(userId, adapted, dayOfWeek).also { sync.workoutPlanChanged(it.id) }

    override suspend fun startSession(planId: String, exerciseIds: List<String>, startedAt: String): WorkoutSession =
        delegate.startSession(planId, exerciseIds, startedAt).also { sync.sessionChanged(it.id) }

    override suspend fun completeSet(setId: String, actualReps: String?, weightKg: Double?, completedAt: String) {
        delegate.completeSet(setId, actualReps, weightKg, completedAt)
        sync.sessionSetChanged()
    }

    override suspend fun completeSession(sessionId: String, completedAt: String, durationMinutes: Int, notes: String?) {
        delegate.completeSession(sessionId, completedAt, durationMinutes, notes)
        sync.sessionChanged(sessionId)
    }

    override suspend fun replaceExercise(
        exerciseId: String,
        newName: String,
        newNotes: String?,
        sessionExerciseId: String?,
    ): Exercise = delegate.replaceExercise(exerciseId, newName, newNotes, sessionExerciseId).also {
        sync.workoutPlanChanged(it.planId)
        if (sessionExerciseId != null) sync.sessionSetChanged()
    }
}

internal class SyncingNutritionRepository(
    private val delegate: NutritionRepository,
    private val sync: CloudSyncScheduler,
) : NutritionRepository by delegate {

    override suspend fun saveGeneratedDiet(userId: String, generated: GeneratedDietPlan): DietPlan {
        val previous = delegate.getActiveDiet(userId)
        val plan = delegate.saveGeneratedDiet(userId, generated)
        previous?.let { sync.dietPlanChanged(it.id) }
        sync.dietPlanChanged(plan.id)
        return plan
    }

    override suspend fun logMeal(
        userId: String,
        mealName: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        loggedAt: String,
        photoUrl: String?,
        slot: String?,
    ): MealLog = delegate.logMeal(userId, mealName, calories, proteinG, carbsG, fatG, loggedAt, photoUrl, slot)
        .also { sync.mealChanged(it.id) }

    override suspend fun logEntries(userId: String, entries: List<MealEntryDraft>, loggedAt: String): List<MealLog> =
        delegate.logEntries(userId, entries, loggedAt).onEach { sync.mealChanged(it.id) }

    override suspend fun updateMeal(meal: MealLog) {
        delegate.updateMeal(meal)
        sync.mealChanged(meal.id)
    }

    override suspend fun updateDietPlanMeals(planId: String, meals: List<PlanMeal>) {
        delegate.updateDietPlanMeals(planId, meals)
        sync.dietPlanChanged(planId)
    }

    override suspend fun deleteMeal(mealId: String) {
        delegate.deleteMeal(mealId)
        sync.mealDeleted(mealId)
    }
}

internal class SyncingFoodRepository(
    private val delegate: FoodRepository,
    private val sync: CloudSyncScheduler,
) : FoodRepository by delegate {

    override suspend fun saveCustomFood(userId: String, draft: CustomFoodDraft, id: String?): FoodItem =
        delegate.saveCustomFood(userId, draft, id).also { sync.customFoodChanged(it.id) }

    override suspend fun deleteCustomFood(id: String) {
        delegate.deleteCustomFood(id)
        sync.customFoodDeleted(id)
    }
}

internal class SyncingProgressRepository(
    private val delegate: ProgressRepository,
    private val sync: CloudSyncScheduler,
) : ProgressRepository by delegate {

    override suspend fun addMeasurement(measurement: BodyMeasurement): BodyMeasurement =
        delegate.addMeasurement(measurement).also { sync.measurementChanged(it.id) }

    override suspend fun updateMeasurement(measurement: BodyMeasurement) {
        delegate.updateMeasurement(measurement)
        sync.measurementChanged(measurement.id)
    }

    override suspend fun deleteMeasurement(id: String) {
        delegate.deleteMeasurement(id)
        sync.measurementDeleted(id)
    }
}

/**
 * Profile edits go to `users/{uid}`. New profiles (onboarding, cloud restore) and linking are uploaded by
 * the sign-in use case itself, so a push here could only race it.
 */
internal class SyncingLocalUserRepository(
    private val delegate: LocalUserRepository,
    private val sync: CloudSyncScheduler,
) : LocalUserRepository by delegate {

    override suspend fun updateProfile(
        id: String,
        name: String,
        email: String,
        age: Long,
        heightCm: Double,
        weightKg: Double,
        goal: String,
        activityLevel: String,
        equipment: String,
        dietRestrictions: String,
        updatedAt: String,
    ) {
        delegate.updateProfile(id, name, email, age, heightCm, weightKg, goal, activityLevel, equipment, dietRestrictions, updatedAt)
        sync.profileChanged()
    }
}

/** Onboarding answers and reminder settings live in preferences; they are part of `users/{uid}`. */
internal class SyncingUserPreferencesRepository(
    private val delegate: UserPreferencesRepository,
    private val sync: CloudSyncScheduler,
) : UserPreferencesRepository by delegate {

    override var attributionSource: String?
        get() = delegate.attributionSource
        set(value) = update(delegate.attributionSource, value) { delegate.attributionSource = it }

    override var commitmentDaysPerWeek: Int?
        get() = delegate.commitmentDaysPerWeek
        set(value) = update(delegate.commitmentDaysPerWeek, value) { delegate.commitmentDaysPerWeek = it }

    override var commitmentConfirmedAt: String?
        get() = delegate.commitmentConfirmedAt
        set(value) = update(delegate.commitmentConfirmedAt, value) { delegate.commitmentConfirmedAt = it }

    override var reminderHour: Int
        get() = delegate.reminderHour
        set(value) = update(delegate.reminderHour, value) { delegate.reminderHour = it }

    override var reminderMinute: Int
        get() = delegate.reminderMinute
        set(value) = update(delegate.reminderMinute, value) { delegate.reminderMinute = it }

    override var notificationsEnabled: Boolean
        get() = delegate.notificationsEnabled
        set(value) = update(delegate.notificationsEnabled, value) { delegate.notificationsEnabled = it }

    override var gender: String?
        get() = delegate.gender
        set(value) = update(delegate.gender, value) { delegate.gender = it }

    override var trainingExperience: String?
        get() = delegate.trainingExperience
        set(value) = update(delegate.trainingExperience, value) { delegate.trainingExperience = it }

    override var trainingObstacle: String?
        get() = delegate.trainingObstacle
        set(value) = update(delegate.trainingObstacle, value) { delegate.trainingObstacle = it }

    override var focusAreas: String?
        get() = delegate.focusAreas
        set(value) = update(delegate.focusAreas, value) { delegate.focusAreas = it }

    override var sessionMinutes: Int?
        get() = delegate.sessionMinutes
        set(value) = update(delegate.sessionMinutes, value) { delegate.sessionMinutes = it }

    private inline fun <T> update(current: T, value: T, write: (T) -> Unit) {
        write(value)
        if (current != value) sync.profileChanged()
    }
}
