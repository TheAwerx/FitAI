package com.fitai.app.auth

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.model.LocalUserProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable

@Serializable
private data class FirestoreUserProfile(
    val name: String,
    val email: String,
    val age: Long,
    val heightCm: Double,
    val weightKg: Double,
    val goal: String,
    val activityLevel: String,
    val equipment: String,
    val dietRestrictions: String,
    val createdAt: String,
    val updatedAt: String,
)

class FirestoreProfileSyncRepository : ProfileSyncRepository {

    private val initialized: Boolean by lazy { FirebaseBootstrap.ensureInitialized() }

    override val isFirebaseAvailable: Boolean get() = initialized

    override suspend fun pushLocalProfile(uid: String, local: LocalUserProfile): Result<Unit> =
        runCatching {
            check(initialized) { "Firebase is not configured" }
            val payload = FirestoreUserProfile(
                name = local.name,
                email = local.email,
                age = local.age,
                heightCm = local.heightCm,
                weightKg = local.weightKg,
                goal = local.goal,
                activityLevel = local.activityLevel,
                equipment = local.equipment,
                dietRestrictions = local.dietRestrictions,
                createdAt = local.createdAt,
                updatedAt = local.updatedAt,
            )
            Firebase.firestore.collection("user_profiles").document(uid)
                .set(FirestoreUserProfile.serializer(), payload, encodeDefaults = true)
        }

    override suspend fun pullProfile(uid: String): Result<LocalUserProfile?> = runCatching {
        check(initialized) { "Firebase is not configured" }
        val doc = Firebase.firestore.collection("user_profiles").document(uid).get()
        if (!doc.exists) return@runCatching null
        val remote = doc.data(FirestoreUserProfile.serializer())
        LocalUserProfile(
            id = uid,
            name = remote.name,
            email = remote.email,
            age = remote.age,
            heightCm = remote.heightCm,
            weightKg = remote.weightKg,
            goal = remote.goal,
            activityLevel = remote.activityLevel,
            equipment = remote.equipment,
            dietRestrictions = remote.dietRestrictions,
            firebaseUid = uid,
            createdAt = remote.createdAt,
            updatedAt = remote.updatedAt,
        )
    }
}
