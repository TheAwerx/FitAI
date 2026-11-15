package com.fitai.app.sync

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.model.DietPlan
import com.fitai.app.model.Exercise
import com.fitai.app.model.WorkoutPlan
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.WorkoutRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

@Serializable
private data class FirestoreWorkoutPlan(
    val id: String,
    val title: String,
    val description: String,
    val daysPerWeek: Int,
    val createdAt: String,
    val isActive: Boolean,
    val exercises: List<FirestoreExercise>,
)

@Serializable
private data class FirestoreExercise(
    val id: String,
    val dayOfWeek: Int,
    val name: String,
    val sets: Int,
    val reps: String,
    val restSeconds: Int,
    val muscleGroup: String,
    val notes: String? = null,
)

@Serializable
private data class FirestoreDietPlan(
    val id: String,
    val dailyCalories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val createdAt: String,
    val isActive: Boolean,
)

class FirestorePlanSyncRepository(
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
) : PlanSyncRepository {

    private val initialized: Boolean by lazy { FirebaseBootstrap.ensureInitialized() }

    override val isFirebaseAvailable: Boolean get() = initialized

    override suspend fun pushPlansToFirestore(uid: String, userId: String): Result<Unit> = runCatching {
        check(initialized) { "Firebase is not configured" }
        val activeWorkout = workoutRepository.getActivePlan(userId)
        if (activeWorkout != null) {
            pushWorkoutPlan(uid, activeWorkout)
        }
        val activeDiet = nutritionRepository.getActiveDiet(userId)
        if (activeDiet != null) {
            pushDietPlan(uid, activeDiet)
        }
    }

    private suspend fun pushWorkoutPlan(uid: String, plan: WorkoutPlan) {
        val exercises = workoutRepository.getExercisesForPlan(plan.id)
        val payload = FirestoreWorkoutPlan(
            id = plan.id,
            title = plan.title,
            description = plan.description,
            daysPerWeek = plan.daysPerWeek,
            createdAt = plan.createdAt,
            isActive = plan.isActive,
            exercises = exercises.map { it.toFirestore() },
        )
        Firebase.firestore
            .collection("users")
            .document(uid)
            .collection("workout_plans")
            .document(plan.id)
            .set(FirestoreWorkoutPlan.serializer(), payload, encodeDefaults = true)
    }

    private suspend fun pushDietPlan(uid: String, plan: DietPlan) {
        val payload = FirestoreDietPlan(
            id = plan.id,
            dailyCalories = plan.dailyCalories,
            proteinG = plan.proteinG,
            carbsG = plan.carbsG,
            fatG = plan.fatG,
            createdAt = plan.createdAt,
            isActive = plan.isActive,
        )
        Firebase.firestore
            .collection("users")
            .document(uid)
            .collection("diet_plans")
            .document(plan.id)
            .set(FirestoreDietPlan.serializer(), payload, encodeDefaults = true)
    }

    private fun Exercise.toFirestore() = FirestoreExercise(
        id = id,
        dayOfWeek = dayOfWeek,
        name = name,
        sets = sets,
        reps = reps,
        restSeconds = restSeconds,
        muscleGroup = muscleGroup,
        notes = notes,
    )
}

class NoOpPlanSyncRepository : PlanSyncRepository {
    override val isFirebaseAvailable: Boolean = false
    override suspend fun pushPlansToFirestore(uid: String, userId: String): Result<Unit> =
        Result.success(Unit)
}
