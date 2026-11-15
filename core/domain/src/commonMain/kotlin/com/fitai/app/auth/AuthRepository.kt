package com.fitai.app.auth

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean = false,
    val displayName: String? = null,
)

interface AuthRepository {
    /** Firebase init succeeded (google-services / plist present). */
    val isFirebaseAvailable: Boolean

    fun authState(): kotlinx.coroutines.flow.Flow<AuthUser?>

    /** Immediate session after sign-in; avoids racing [authState]. */
    fun currentUserOrNull(): AuthUser?

    suspend fun signInWithEmail(email: String, password: String): Result<Unit>

    suspend fun registerWithEmail(email: String, password: String): Result<Unit>

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit>

    suspend fun signInWithApple(idToken: String, rawNonce: String): Result<Unit>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun signOut()
}
