package com.fitai.app.auth

import com.fitai.app.firebase.FirebaseBootstrap
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AuthRemoteDataSource {
    private val initialized: Boolean by lazy { FirebaseBootstrap.ensureInitialized() }

    val isFirebaseAvailable: Boolean get() = initialized

    fun authState(): Flow<AuthUser?> {
        if (!initialized) return flowOf(null)
        return Firebase.auth.authStateChanged.map { user: FirebaseUser? -> user?.toAuthUser() }
    }

    fun currentUserOrNull(): AuthUser? {
        if (!initialized) return null
        return Firebase.auth.currentUser?.toAuthUser()
    }

    suspend fun sendPasswordReset(email: String) {
        requireFirebase()
        Firebase.auth.sendPasswordResetEmail(email)
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email,
        isAnonymous = isAnonymous,
        displayName = displayName,
    )

    suspend fun signInWithEmail(email: String, password: String) {
        requireFirebase()
        Firebase.auth.signInWithEmailAndPassword(email, password)
    }

    suspend fun registerWithEmail(email: String, password: String) {
        requireFirebase()
        Firebase.auth.createUserWithEmailAndPassword(email, password)
    }

    suspend fun signInWithGoogleIdToken(idToken: String) {
        requireFirebase()
        val credential = GoogleAuthProvider.credential(idToken, null)
        Firebase.auth.signInWithCredential(credential)
    }

    suspend fun signInWithApple(idToken: String, rawNonce: String) {
        requireFirebase()
        val credential = OAuthProvider.credential(
            providerId = "apple.com",
            idToken = idToken,
            rawNonce = rawNonce,
        )
        Firebase.auth.signInWithCredential(credential)
    }

    suspend fun signOut() {
        if (!initialized) return
        runCatching { Firebase.auth.signOut() }
    }

    private fun requireFirebase() {
        check(initialized) { "Firebase is not configured" }
    }
}
