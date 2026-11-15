package com.fitai.app.trial

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.firebase.FitAiFunctions
import com.fitai.app.model.TrialStatus
import com.fitai.app.model.TrialStatusDto
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

class FirestoreTrialRepository : TrialRepository {

    private val initialized: Boolean by lazy { FirebaseBootstrap.ensureInitialized() }

    override val isFirebaseAvailable: Boolean get() = initialized

    override suspend fun ensureAnonymousAuth(): Result<String> = runCatching {
        check(initialized) { "Firebase is not configured" }
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
        auth.currentUser?.uid ?: error("Anonymous auth failed")
    }

    override suspend fun initGuestTrial(): Result<TrialStatus> = runCatching {
        check(initialized) { "Firebase is not configured" }
        ensureAnonymousAuth().getOrThrow()
        FitAiFunctions.initGuestTrial().toDomain()
    }

    override suspend fun getTrialStatus(): Result<TrialStatus> = runCatching {
        check(initialized) { "Firebase is not configured" }
        val uid = Firebase.auth.currentUser?.uid ?: ensureAnonymousAuth().getOrThrow()
        val doc = Firebase.firestore.collection("guest_trials").document(uid).get()
        if (doc.exists) {
            TrialStatus(
                trialStartedAt = doc.get("trialStartedAt") ?: "",
                trialExpiresAt = doc.get("trialExpiresAt") ?: "",
                daysRemaining = (doc.get<Long?>("daysRemaining") ?: 0L).toInt(),
                isActive = doc.get<Boolean?>("isActive") ?: false,
                hasSubscription = doc.get<Boolean?>("hasSubscription") ?: false,
            )
        } else {
            FitAiFunctions.getTrialStatus().toDomain()
        }
    }

    override suspend fun canAccessAiFeatures(): Result<Boolean> = runCatching {
        val status = getTrialStatus().getOrThrow()
        status.isActive || status.hasSubscription || Firebase.auth.currentUser?.isAnonymous == false
    }

    private fun TrialStatusDto.toDomain() = TrialStatus(
        trialStartedAt = trialStartedAt,
        trialExpiresAt = trialExpiresAt,
        daysRemaining = daysRemaining,
        isActive = isActive,
        hasSubscription = hasSubscription,
    )
}

class LocalTrialRepository : TrialRepository {
    private var trialStartedAt: String? = null

    override val isFirebaseAvailable: Boolean = false

    override suspend fun ensureAnonymousAuth(): Result<String> =
        Result.success("local-guest")

    override suspend fun initGuestTrial(): Result<TrialStatus> {
        val now = com.fitai.app.util.nowIsoTimestamp()
        trialStartedAt = now
        return Result.success(
            TrialStatus(
                trialStartedAt = now,
                trialExpiresAt = now,
                daysRemaining = 3,
                isActive = true,
                hasSubscription = false,
            ),
        )
    }

    override suspend fun getTrialStatus(): Result<TrialStatus> {
        val now = com.fitai.app.util.nowIsoTimestamp()
        return Result.success(
            TrialStatus(
                trialStartedAt = trialStartedAt ?: now,
                trialExpiresAt = trialStartedAt ?: now,
                daysRemaining = 3,
                isActive = true,
                hasSubscription = false,
            ),
        )
    }

    override suspend fun canAccessAiFeatures(): Result<Boolean> = Result.success(true)
}
