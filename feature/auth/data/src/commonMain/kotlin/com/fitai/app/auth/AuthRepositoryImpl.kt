package com.fitai.app.auth

import kotlinx.coroutines.flow.Flow

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
) : AuthRepository {

    override val isFirebaseAvailable: Boolean get() = remote.isFirebaseAvailable

    override fun authState(): Flow<AuthUser?> = remote.authState()

    override fun currentUserOrNull(): AuthUser? = remote.currentUserOrNull()

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching { remote.signInWithEmail(email, password) }

    override suspend fun registerWithEmail(email: String, password: String): Result<Unit> =
        runCatching { remote.registerWithEmail(email, password) }

    override suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        runCatching { remote.signInWithGoogleIdToken(idToken) }

    override suspend fun signInWithApple(idToken: String, rawNonce: String): Result<Unit> =
        runCatching { remote.signInWithApple(idToken, rawNonce) }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching { remote.sendPasswordReset(email) }

    override suspend fun signOut() {
        remote.signOut()
    }
}
