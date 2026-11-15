package com.fitai.app.auth

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.notifications.ReminderScheduler
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.session.LocalSessionRepository
import com.fitai.app.sync.PlanSyncRepository
import com.fitai.app.util.nowIsoTimestamp

/** What happened to the on-device profile after a successful sign-in. */
enum class SignInOutcome {
    /** The onboarding profile on this device is now linked to the account. */
    LinkedLocalProfile,
    /** No profile on this device; the account's cloud profile was restored and still needs plans. */
    RestoredFromCloud,
    /** Neither this device nor the account has a profile; the user must go through onboarding. */
    NoProfile,
}

class IsFirebaseAvailableUseCase(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Boolean = authRepository.isFirebaseAvailable
}

/** Shared post-sign-in step: link and push the local profile, or restore it from the cloud. */
class LinkSignedInAccountUseCase(
    private val authRepository: AuthRepository,
    private val profileSyncRepository: ProfileSyncRepository,
    private val localUserRepository: LocalUserRepository,
    private val planSyncRepository: PlanSyncRepository,
) {
    suspend operator fun invoke(): UseCaseState<SignInOutcome> = useCaseCatching {
        val uid = authRepository.currentUserOrNull()?.uid ?: error("auth_session_missing")
        val local = localUserRepository.getLatestUser()
        if (local != null) {
            localUserRepository.linkFirebaseUid(local.id, uid, nowIsoTimestamp())
            profileSyncRepository.pushLocalProfile(uid, local).getOrThrow()
            planSyncRepository.pushPlansToFirestore(uid, local.id).getOrThrow()
            return@useCaseCatching SignInOutcome.LinkedLocalProfile
        }
        val remote = profileSyncRepository.pullProfile(uid).getOrThrow() ?: return@useCaseCatching SignInOutcome.NoProfile
        localUserRepository.insertGuestProfile(
            id = remote.id,
            name = remote.name,
            email = remote.email,
            age = remote.age,
            heightCm = remote.heightCm,
            weightKg = remote.weightKg,
            goal = remote.goal,
            activityLevel = remote.activityLevel,
            equipment = remote.equipment,
            dietRestrictions = remote.dietRestrictions,
            createdAt = remote.createdAt,
            updatedAt = nowIsoTimestamp(),
        )
        localUserRepository.linkFirebaseUid(remote.id, uid, nowIsoTimestamp())
        SignInOutcome.RestoredFromCloud
    }
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

class SyncPlansToFirestoreUseCase(
    private val planSyncRepository: PlanSyncRepository,
    private val localUserRepository: LocalUserRepository,
) {
    suspend operator fun invoke(uid: String): UseCaseState<Unit> =
        useCaseCatching {
            val user = localUserRepository.getLatestUser() ?: error("no_user")
            planSyncRepository.pushPlansToFirestore(uid, user.id).getOrThrow()
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
) {
    suspend operator fun invoke(): UseCaseState<Unit> = useCaseCatching {
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
