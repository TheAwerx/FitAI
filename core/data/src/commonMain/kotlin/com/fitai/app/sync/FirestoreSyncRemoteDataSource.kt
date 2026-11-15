package com.fitai.app.sync

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.sync.FirestorePaths.DIET_PLANS
import com.fitai.app.sync.FirestorePaths.LEGACY_USER_PROFILES
import com.fitai.app.sync.FirestorePaths.MEAL_LOGS
import com.fitai.app.sync.FirestorePaths.MEASUREMENTS
import com.fitai.app.sync.FirestorePaths.USERS
import com.fitai.app.sync.FirestorePaths.WORKOUT_PLANS
import com.fitai.app.sync.FirestorePaths.WORKOUT_SESSIONS
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.DocumentReference
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.WriteBatch
import dev.gitlive.firebase.firestore.firestore
import io.github.aakira.napier.Napier
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer

internal data class CloudAccount(val uid: String, val email: String?)

/** Firestore reads and writes for `users/{uid}` and its subcollections (see `firebase/SCHEMA.md`). */
internal class FirestoreSyncRemoteDataSource {
    private val initialized: Boolean by lazy { FirebaseBootstrap.ensureInitialized() }

    val isAvailable: Boolean get() = initialized

    /** The signed-in, non-anonymous account; anonymous sessions have no cloud copy. */
    fun currentAccount(): CloudAccount? {
        if (!initialized) return null
        val user = Firebase.auth.currentUser ?: return null
        return if (user.isAnonymous) null else CloudAccount(user.uid, user.email)
    }

    suspend fun getUser(uid: String): UserDocument? =
        userRef(uid).get().decodeOrNull(UserDocument.serializer())

    suspend fun getLegacyProfile(uid: String): LegacyUserProfileDocument? =
        Firebase.firestore.collection(LEGACY_USER_PROFILES).document(uid).get()
            .decodeOrNull(LegacyUserProfileDocument.serializer())

    suspend fun setUser(uid: String, doc: UserDocument) = userRef(uid).write(UserDocument.serializer(), doc)

    suspend fun setWorkoutPlan(uid: String, doc: WorkoutPlanDocument) =
        sub(uid, WORKOUT_PLANS).document(doc.id).write(WorkoutPlanDocument.serializer(), doc)

    suspend fun setDietPlan(uid: String, doc: DietPlanDocument) =
        sub(uid, DIET_PLANS).document(doc.id).write(DietPlanDocument.serializer(), doc)

    suspend fun setSession(uid: String, doc: WorkoutSessionDocument) =
        sub(uid, WORKOUT_SESSIONS).document(doc.id).write(WorkoutSessionDocument.serializer(), doc)

    suspend fun setMeal(uid: String, doc: MealLogDocument) =
        sub(uid, MEAL_LOGS).document(doc.id).write(MealLogDocument.serializer(), doc)

    suspend fun deleteMeal(uid: String, mealId: String) = sub(uid, MEAL_LOGS).document(mealId).delete()

    suspend fun setMeasurement(uid: String, doc: MeasurementDocument) =
        sub(uid, MEASUREMENTS).document(doc.id).write(MeasurementDocument.serializer(), doc)

    suspend fun deleteMeasurement(uid: String, id: String) = sub(uid, MEASUREMENTS).document(id).delete()

    /** Writes every document in batches (Firestore allows 500 writes per batch). */
    suspend fun writeAll(uid: String, data: CloudDataSnapshot) {
        val writes = buildList<(WriteBatch) -> WriteBatch> {
            data.workoutPlans.forEach { doc -> add { it.put(sub(uid, WORKOUT_PLANS).document(doc.id), WorkoutPlanDocument.serializer(), doc) } }
            data.dietPlans.forEach { doc -> add { it.put(sub(uid, DIET_PLANS).document(doc.id), DietPlanDocument.serializer(), doc) } }
            data.sessions.forEach { doc -> add { it.put(sub(uid, WORKOUT_SESSIONS).document(doc.id), WorkoutSessionDocument.serializer(), doc) } }
            data.meals.forEach { doc -> add { it.put(sub(uid, MEAL_LOGS).document(doc.id), MealLogDocument.serializer(), doc) } }
            data.measurements.forEach { doc -> add { it.put(sub(uid, MEASUREMENTS).document(doc.id), MeasurementDocument.serializer(), doc) } }
        }
        writes.chunked(BATCH_SIZE).forEach { chunk ->
            chunk.fold(Firebase.firestore.batch()) { batch, write -> write(batch) }.commit()
        }
    }

    suspend fun readAll(uid: String): CloudDataSnapshot = CloudDataSnapshot(
        workoutPlans = readCollection(sub(uid, WORKOUT_PLANS), WorkoutPlanDocument.serializer()) { doc, id ->
            if (doc.id.isBlank()) doc.copy(id = id) else doc
        },
        dietPlans = readCollection(sub(uid, DIET_PLANS), DietPlanDocument.serializer()) { doc, id ->
            if (doc.id.isBlank()) doc.copy(id = id) else doc
        },
        sessions = readCollection(sub(uid, WORKOUT_SESSIONS), WorkoutSessionDocument.serializer()) { doc, id ->
            if (doc.id.isBlank()) doc.copy(id = id) else doc
        },
        meals = readCollection(sub(uid, MEAL_LOGS), MealLogDocument.serializer()) { doc, id ->
            if (doc.id.isBlank()) doc.copy(id = id) else doc
        },
        measurements = readCollection(sub(uid, MEASUREMENTS), MeasurementDocument.serializer()) { doc, id ->
            if (doc.id.isBlank()) doc.copy(id = id) else doc
        },
    )

    private fun userRef(uid: String): DocumentReference = Firebase.firestore.collection(USERS).document(uid)

    private fun sub(uid: String, name: String): CollectionReference = userRef(uid).collection(name)

    private suspend fun <T : Any> DocumentReference.write(serializer: KSerializer<T>, doc: T) {
        set(serializer, doc) { encodeDefaults = true }
    }

    private fun <T : Any> WriteBatch.put(ref: DocumentReference, serializer: KSerializer<T>, doc: T): WriteBatch =
        set(ref, serializer, doc) { encodeDefaults = true }

    private suspend fun <T> readCollection(
        collection: CollectionReference,
        serializer: DeserializationStrategy<T>,
        withId: (T, String) -> T,
    ): List<T> = collection.get().documents.mapNotNull { snapshot ->
        runCatching { withId(snapshot.data(serializer), snapshot.id) }
            .onFailure { Napier.w("Skipping unreadable ${collection.path}/${snapshot.id}", it) }
            .getOrNull()
    }

    private fun <T> DocumentSnapshot.decodeOrNull(serializer: DeserializationStrategy<T>): T? =
        if (exists) data(serializer) else null

    private companion object {
        const val BATCH_SIZE = 400
    }
}
