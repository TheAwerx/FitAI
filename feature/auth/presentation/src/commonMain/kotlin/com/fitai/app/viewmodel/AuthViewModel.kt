package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.auth.AuthenticateWithEmailUseCase
import com.fitai.app.auth.IsFirebaseAvailableUseCase
import com.fitai.app.auth.LinkExistingSessionUseCase
import com.fitai.app.auth.SendPasswordResetUseCase
import com.fitai.app.auth.SignInOutcome
import com.fitai.app.auth.SignInWithAppleUseCase
import com.fitai.app.auth.SignInWithGoogleUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.viewmodel.auth.AuthEffect
import com.fitai.app.viewmodel.auth.AuthIntent
import com.fitai.app.viewmodel.auth.AuthMode
import com.fitai.app.viewmodel.auth.AuthState
import kotlinx.coroutines.launch

class AuthViewModel(
    isFirebaseAvailableUseCase: IsFirebaseAvailableUseCase,
    private val authenticateWithEmailUseCase: AuthenticateWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithAppleUseCase: SignInWithAppleUseCase,
    private val linkExistingSessionUseCase: LinkExistingSessionUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<AuthState, AuthIntent, AuthEffect>(
    AuthState(isFirebaseAvailable = isFirebaseAvailableUseCase()),
) {
    private var initialized = false
    private var initialMode = AuthMode.SignUpOptions

    override fun handleIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.Init -> init(intent.signIn, intent.funnel)
            is AuthIntent.EmailChanged -> setState { copy(email = intent.value, errorMessage = null, resetSent = false) }
            is AuthIntent.PasswordChanged -> setState { copy(password = intent.value, errorMessage = null) }
            AuthIntent.TogglePasswordVisible -> setState { copy(passwordVisible = !passwordVisible) }
            is AuthIntent.ShowMode -> setState { copy(mode = intent.mode, errorMessage = null, resetSent = false) }
            AuthIntent.Submit -> submitEmail()
            AuthIntent.ForgotPassword -> forgotPassword()
            is AuthIntent.GoogleIdToken -> runSignIn { signInWithGoogleUseCase(intent.token) }
            is AuthIntent.AppleTokens -> runSignIn { signInWithAppleUseCase(intent.idToken, intent.rawNonce) }
            is AuthIntent.ProviderError ->
                setState { copy(errorMessage = intent.message?.takeIf { it.isNotBlank() } ?: ERROR_UNKNOWN) }
            AuthIntent.Back -> back()
        }
    }

    private fun init(signIn: Boolean, funnel: Boolean) {
        if (initialized) return
        initialized = true
        initialMode = if (signIn) AuthMode.SignIn else AuthMode.SignUpOptions
        setState { copy(mode = initialMode, funnel = funnel) }
        if (funnel) {
            viewModelScope.launch {
                val linked = linkExistingSessionUseCase() ?: return@launch
                handleResult(linked)
            }
        }
    }

    /** Email sign-up goes back to the provider list; the funnel's first screen has no back. */
    private fun back() {
        val s = currentState()
        if (s.busy) return
        when {
            s.mode == AuthMode.SignUpEmail -> setState { copy(mode = AuthMode.SignUpOptions, errorMessage = null) }
            s.mode != initialMode -> setState { copy(mode = initialMode, errorMessage = null) }
            !s.funnel -> setEffect(AuthEffect.NavigateBack)
        }
    }

    private fun submitEmail() {
        val s = currentState()
        val register = s.mode != AuthMode.SignIn
        runSignIn { authenticateWithEmailUseCase(s.email.trim(), s.password, register) }
    }

    private fun runSignIn(block: suspend () -> UseCaseState<SignInOutcome>) {
        if (currentState().busy) return
        viewModelScope.launch {
            setState { copy(busy = true, errorMessage = null) }
            handleResult(block())
        }
    }

    private fun handleResult(result: UseCaseState<SignInOutcome>) {
        when (result) {
            is UseCaseState.Success -> {
                setState { copy(busy = false) }
                trackEventUseCase(AnalyticsEvents.SIGNUP_COMPLETED, mapOf(PARAM_OUTCOME to result.data.name.lowercase()))
                setEffect(AuthEffect.Completed(result.data))
            }
            is UseCaseState.Error -> setState { copy(busy = false, errorMessage = result.message) }
        }
    }

    private fun forgotPassword() {
        val email = currentState().email
        viewModelScope.launch {
            setState { copy(busy = true, errorMessage = null, resetSent = false) }
            when (val result = sendPasswordResetUseCase(email)) {
                is UseCaseState.Success -> setState { copy(busy = false, resetSent = true) }
                is UseCaseState.Error -> setState { copy(busy = false, errorMessage = result.message) }
            }
        }
    }

    private companion object {
        const val PARAM_OUTCOME = "outcome"
        const val ERROR_UNKNOWN = "error_unknown"
    }
}
