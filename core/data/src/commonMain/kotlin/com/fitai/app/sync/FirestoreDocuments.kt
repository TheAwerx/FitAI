package com.fitai.app.sync

import com.fitai.app.model.SessionLength
import kotlinx.serialization.Serializable

/**
 * Firestore document shapes, one to one with `firebase/SCHEMA.md` and the checks in `firestore.rules`.
 * Every field has a default so older or partial documents still decode.
 */
internal object FirestorePaths {
    const val USERS = "users"
    const val LEGACY_USER_PROFILES = "user_profiles"
    const val WORKOUT_PLANS = "workout_plans"
    const val DIET_PLANS = "diet_plans"
    const val WORKOUT_SESSIONS = "workout_sessions"
    const val MEAL_LOGS = "meal_logs"
    const val MEASUREMENTS = "measurements"
}

internal const val CLOUD_SCHEMA_VERSION = 1

@Serializable
internal data class UserDocument(
    val schemaVersion: Int = CLOUD_SCHEMA_VERSION,
    val localUserId: String = "",
    val name: String = "",
    val email: String = "",
    val age: Long = 0,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val gender: String? = null,
    val goal: String = "",
    val activityLevel: String = "",
    val equipment: List<String> = emptyList(),
    val dietRestrictions: List<String> = emptyList(),
    val experience: String? = null,
    val obstacle: String? = null,
    val focusAreas: List<String> = emptyList(),
    val daysPerWeek: Int = DEFAULT_DAYS_PER_WEEK,
    val sessionMinutes: Int = SessionLength.DEFAULT,
    val attributionSource: String? = null,
    val commitmentConfirmedAt: String? = null,
    val notificationsEnabled: Boolean = false,
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
    val reminderMinute: Int = 0,
    val language: String = "tr",
    val timeZone: String = "UTC",
    val createdAt: String = "",
    val updatedAt: String = "",
) {
    companion object {
        const val DEFAULT_DAYS_PER_WEEK = 3
        const val DEFAULT_REMINDER_HOUR = 18
    }
}

/** Pre-schema `user_profiles/{uid}`; read once on sign-in and moved into [UserDocument]. */
@Serializable
internal data class LegacyUserProfileDocument(
    val name: String = "",
    val email: String = "",
    val age: Long = 0,
    val heightCm: Double = 0.0,
    val weightKg: Double = 0.0,
    val goal: String = "",
    val activityLevel: String = "",
    val equipment: String = "",
    val dietRestrictions: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
internal data class WorkoutPlanDocument(
    val schemaVersion: Int = CLOUD_SCHEMA_VERSION,
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val daysPerWeek: Int = 0,
    val kind: String = KIND_PROGRAM,
    val isActive: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
    val exercises: List<ExerciseEntry> = emptyList(),
) {
    companion object {
        const val KIND_PROGRAM = "program"
        const val KIND_ADHOC = "adhoc"
    }
}

@Serializable
internal data class ExerciseEntry(
    val id: String = "",
    /** ISO weekday 1..7; `<= 0` marks an exercise archived by "Replace" that history still points to. */
    val dayOfWeek: Int = 0,
    val name: String = "",
    val sets: Int = 0,
    val reps: String = "",
    val restSeconds: Int = 0,
    val muscleGroup: String = "",
    val notes: String? = null,
)

@Serializable
internal data class DietPlanDocument(
    val schemaVersion: Int = CLOUD_SCHEMA_VERSION,
    val id: String = "",
    val dailyCalories: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val isActive: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
internal data class WorkoutSessionDocument(
    val schemaVersion: Int = CLOUD_SCHEMA_VERSION,
    val id: String = "",
    val planId: String = "",
    val startedAt: String = "",
    val completedAt: String? = null,
    val completed: Boolean = false,
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val localDate: String = "",
    val totalSets: Int = 0,
    val completedSets: Int = 0,
    val volumeKg: Double = 0.0,
    val exercises: List<SessionExerciseEntry> = emptyList(),
    val updatedAt: String = "",
)

@Serializable
internal data class SessionExerciseEntry(
    val sessionExerciseId: String = "",
    val exerciseId: String = "",
    val orderIndex: Int = 0,
    val name: String = "",
    val muscleGroup: String = "",
    val sets: List<SessionSetEntry> = emptyList(),
)

@Serializable
internal data class SessionSetEntry(
    val id: String = "",
    val setNumber: Int = 0,
    val targetReps: String = "",
    val actualReps: String? = null,
    val weightKg: Double? = null,
    val completed: Boolean = false,
    val completedAt: String? = null,
)

@Serializable
internal data class MealLogDocument(
    val schemaVersion: Int = CLOUD_SCHEMA_VERSION,
    val id: String = "",
    val mealName: String = "",
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val source: String = SOURCE_MANUAL,
    val photoUrl: String? = null,
    val loggedAt: String = "",
    val localDate: String = "",
    val updatedAt: String = "",
) {
    companion object {
        const val SOURCE_MANUAL = "manual"
        const val SOURCE_PHOTO = "photo"
    }
}

@Serializable
internal data class MeasurementDocument(
    val schemaVersion: Int = CLOUD_SCHEMA_VERSION,
    val id: String = "",
    val weightKg: Double = 0.0,
    val bodyFatPct: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val hipCm: Double? = null,
    val armCm: Double? = null,
    val photoUrl: String? = null,
    val measuredAt: String = "",
    val localDate: String = "",
    val updatedAt: String = "",
)

/** Everything under `users/{uid}/…` except the account document itself. */
internal data class CloudDataSnapshot(
    val workoutPlans: List<WorkoutPlanDocument> = emptyList(),
    val dietPlans: List<DietPlanDocument> = emptyList(),
    val sessions: List<WorkoutSessionDocument> = emptyList(),
    val meals: List<MealLogDocument> = emptyList(),
    val measurements: List<MeasurementDocument> = emptyList(),
)
