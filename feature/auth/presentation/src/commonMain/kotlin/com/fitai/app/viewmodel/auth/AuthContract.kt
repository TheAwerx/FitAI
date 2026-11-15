package com.fitai.app.viewmodel.auth

import com.fitai.app.auth.SignInOutcome

enum class AuthMode { SignUpOptions, SignUpEmail, SignIn }

data class AuthState(
    val mode: AuthMode = AuthMode.SignUpOptions,
    /** Mandatory sign-up right after the paywall: no way back into the app without an account. */
    val funnel: Boolean = false,
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val busy: Boolean = false,
    val errorMessage: String? = null,
    val resetSent: Boolean = false,
    val isFirebaseAvailable: Boolean = false,
)

sealed interface AuthIntent {
    data class Init(val signIn: Boolean, val funnel: Boolean) : AuthIntent
    data class EmailChanged(val value: String) : AuthIntent
    data class PasswordChanged(val value: String) : AuthIntent
    data object TogglePasswordVisible : AuthIntent
    data class ShowMode(val mode: AuthMode) : AuthIntent
    data object Submit : AuthIntent
    data object ForgotPassword : AuthIntent
    data class GoogleIdToken(val token: String) : AuthIntent
    data class AppleTokens(val idToken: String, val rawNonce: String) : AuthIntent
    data class ProviderError(val message: String?) : AuthIntent
    data object Back : AuthIntent
}

sealed interface AuthEffect {
    data class Completed(val outcome: SignInOutcome) : AuthEffect
    data object NavigateBack : AuthEffect
}
