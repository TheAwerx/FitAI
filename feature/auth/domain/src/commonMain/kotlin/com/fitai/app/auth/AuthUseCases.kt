package com.fitai.app.auth

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.notifications.ReminderScheduler
import com.fitai.app.notifications.SyncPushRegistrationUseCase
import com.fitai.app.notifications.UnregisterPushDeviceUseCase
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.session.LocalSessionRepository
import com.fitai.app.sync.UserDataSyncRepository
import com.fitai.app.sync.applyCloudProfile
import com.fitai.app.sync.buildCloudUserProfile
import com.fitai.app.util.nowIsoTimestamp
import kotlin.random.Random

/** What happened to the on-device profile after a successful sign-in. */
enum class SignInOutcome {
    /** The onboarding profile on this device is now linked to the account. */
    LinkedLocalProfile,
    /** No profile on this device; the account's cloud profile was restored and still needs plans. */
    RestoredFromCloud,
    /** No profile on this device; profile, answers, active plans and history all came back from the cloud. */
    RestoredWithPlans,
    /** Neither this device nor the account has a profile; the user must go through onboarding. */
    NoProfile,
}

class IsFirebaseAvailableUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Boolean = authRepository.isFirebaseAvailable
}

/**
 * Shared post-sign-in step (see `firebase/SCHEMA.md`, "Sign-in sync flow"): link and upload the device
 * profile, or restore the account's cloud copy onto an empty device.
 */
class LinkSignedInAccountUseCase(
    private val authRepository: AuthRepository,
    private val profileSyncRepository: ProfileSyncRepository,
    private val localUserRepository: LocalUserRepository,
    private val userDataSyncRepository: UserDataSyncRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val syncPushRegistrationUseCase: SyncPushRegistrationUseCase,
) {
    suspend operator fun invoke(): UseCaseState<SignInOutcome> = useCaseCatching { link() }.also { result ->
        // Server pushes follow the account: register this installation's token for it (best effort).
        if (result is UseCaseState.Success) syncPushRegistrationUseCase()
    }

    private suspend fun link(): SignInOutcome {
        val account = authRepository.currentUserOrNull() ?: error("auth_session_missing")
        val uid = account.uid
        val local = localUserRepository.getLatestUser()
        if (local != null) {
            val now = nowIsoTimestamp()
            // Older history is merged only into the same (or a not yet linked) account, never across accounts.
            val sameAccount = local.firebaseUid == null || local.firebaseUid == uid
            localUserRepository.linkFirebaseUid(local.id, uid, now)
            val linked = local.copy(firebaseUid = uid, updatedAt = now)
            profileSyncRepository.pushProfile(uid, buildCloudUserProfile(linked, userPreferencesRepository, account.email))
                .getOrThrow()
            // Plans and history are best effort: sign-in must not fail on them, later writes sync one by one.
            if (sameAccount) userDataSyncRepository.restoreAll(uid, local.id, keepLocalPlans = true)
            userDataSyncRepository.pushAll(uid, local.id)
            return SignInOutcome.LinkedLocalProfile
        }
        val remote = profileSyncRepository.pullProfile(uid).getOrThrow() ?: return SignInOutcome.NoProfile
        val restored = remote.profile
        val localId = restored.id.ifBlank { newLocalUserId() }
        val now = nowIsoTimestamp()
        localUserRepository.insertGuestProfile(
            id = localId,
            name = restored.name,
            email = restored.email.ifBlank { account.email.orEmpty() },
            age = restored.age,
            heightCm = restored.heightCm,
            weightKg = restored.weightKg,
            goal = restored.goal,
            activityLevel = restored.activityLevel,
            equipment = restored.equipment,
            dietRestrictions = restored.dietRestrictions,
            createdAt = restored.createdAt.ifBlank { now },
            updatedAt = now,
        )
        localUserRepository.linkFirebaseUid(localId, uid, now)
        userPreferencesRepository.applyCloudProfile(remote)
        if (remote.legacy) {
            // One-time move of the old `user_profiles/{uid}` document into `users/{uid}`.
            localUserRepository.getLatestUser()?.let { migrated ->
                profileSyncRepository.pushProfile(uid, buildCloudUserProfile(migrated, userPreferencesRepository, account.email))
            }
        }
        val summary = userDataSyncRepository.restoreAll(uid, localId, keepLocalPlans = false).getOrNull()
        return if (summary?.hasActivePlans == true) SignInOutcome.RestoredWithPlans else SignInOutcome.RestoredFromCloud
    }

    private fun newLocalUserId(): String = (1..32).joinToString("") { Random.nextInt(0, 16).toString(16) }
}

class AuthenticateWithEmailUseCase(
    private val authRepository: AuthRepository,
    private val linkSignedInAccountUseCase: LinkSignedInAccountUseCase,
) {
    suspend operator fun invoke(email: String, password: String, register: Boolean): UseCaseState<SignInOutcome> {
        if (!authRepository.isFirebaseAvailable) {
            return UseCaseState.Error("firebase_unavailable")
        }
        if (email.isBlank() || password.isBlank()) {
            return UseCaseState.Error("validation_email_password_required")
        }
        if (register && password.length < MIN_PASSWORD_LENGTH) {
            return UseCaseState.Error("validation_password_short")
        }
        val authResult =
            if (register) authRepository.registerWithEmail(email, password)
            else authRepository.signInWithEmail(email, password)
        authResult.exceptionOrNull()?.let {
            return UseCaseState.Error(it.message ?: "error_unknown")
        }
        return linkSignedInAccountUseCase()
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}

/** Already signed in with a real (non-anonymous) account, e.g. sign-in from Welcome before onboarding. */
class LinkExistingSessionUseCase(
    private val authRepository: AuthRepository,
    private val linkSignedInAccountUseCase: LinkSignedInAccountUseCase,
) {
    suspend operator fun invoke(): UseCaseState<SignInOutcome>? {
        val user = authRepository.currentUserOrNull() ?: return null
        if (user.isAnonymous) return null
        return linkSignedInAccountUseCase()
    }
}

class SendPasswordResetUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): UseCaseState<Unit> {
        if (email.isBlank()) return UseCaseState.Error("validation_email_required")
        return authRepository.sendPasswordReset(email.trim()).toUseCaseStateCompat()
    }
}

/** Full upload of the device account (profile, answers, plans, history) to `users/{uid}`. */
class SyncUserDataToCloudUseCase(
    private val authRepository: AuthRepository,
    private val profileSyncRepository: ProfileSyncRepository,
    private val userDataSyncRepository: UserDataSyncRepository,
    private val localUserRepository: LocalUserRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(): UseCaseState<Unit> =
        useCaseCatching {
            val account = authRepository.currentUserOrNull()?.takeUnless { it.isAnonymous } ?: error("auth_session_missing")
            val user = localUserRepository.getLatestUser() ?: error("no_user")
            check(user.firebaseUid == account.uid) { "account_not_linked" }
            profileSyncRepository.pushProfile(account.uid, buildCloudUserProfile(user, userPreferencesRepository, account.email))
                .getOrThrow()
            userDataSyncRepository.pushAll(account.uid, user.id).getOrThrow()
        }
}

class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository,
    private val linkSignedInAccountUseCase: LinkSignedInAccountUseCase,
) {
    suspend operator fun invoke(idToken: String): UseCaseState<SignInOutcome> {
        authRepository.signInWithGoogleIdToken(idToken).exceptionOrNull()?.let {
            return UseCaseState.Error(it.message ?: "error_unknown")
        }
        return linkSignedInAccountUseCase()
    }
}

class SignInWithAppleUseCase(
    private val authRepository: AuthRepository,
    private val linkSignedInAccountUseCase: LinkSignedInAccountUseCase,
) {
    suspend operator fun invoke(idToken: String, rawNonce: String): UseCaseState<SignInOutcome> {
        authRepository.signInWithApple(idToken, rawNonce).exceptionOrNull()?.let {
            return UseCaseState.Error(it.message ?: "error_unknown")
        }
        return linkSignedInAccountUseCase()
    }
}

class SignOutUseCase(
    private val authRepository: AuthRepository,
    private val localSessionRepository: LocalSessionRepository,
    private val reminderScheduler: ReminderScheduler,
    private val platformAuthSession: PlatformAuthSession,
    private val unregisterPushDeviceUseCase: UnregisterPushDeviceUseCase,
) {
    suspend operator fun invoke(): UseCaseState<Unit> = useCaseCatching {
        // While still signed in: the owner-only rules allow deleting the device document only now.
        unregisterPushDeviceUseCase()
        authRepository.signOut()
        localSessionRepository.clear()
        reminderScheduler.cancelAll()
        platformAuthSession.clear()
    }
}

class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke() = authRepository.authState()
}

private fun Result<Unit>.toUseCaseStateCompat(): UseCaseState<Unit> =
    fold(
        onSuccess = { UseCaseState.Success(Unit) },
        onFailure = { UseCaseState.Error(it.message ?: "error_unknown") },
    )
