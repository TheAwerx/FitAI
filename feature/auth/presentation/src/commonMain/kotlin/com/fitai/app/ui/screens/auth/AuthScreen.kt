package com.fitai.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.fitai.app.auth.SignInOutcome
import com.fitai.app.localization.LocalStrings
import com.fitai.app.localization.Strings
import com.fitai.app.ui.components.FitAIButton
import com.fitai.app.ui.components.FitAIButtonVariant
import com.fitai.app.ui.components.FitAITextField
import com.fitai.app.ui.platform.rememberAppleSignInLauncher
import com.fitai.app.ui.platform.rememberGoogleSignInLauncher
import com.fitai.app.ui.theme.FitAIColors
import com.fitai.app.ui.theme.FitAIIcons
import com.fitai.app.ui.theme.FitAIShapes
import com.fitai.app.ui.theme.FitAISpacing
import com.fitai.app.ui.theme.FitAITheme
import com.fitai.app.viewmodel.AuthViewModel
import com.fitai.app.viewmodel.auth.AuthEffect
import com.fitai.app.viewmodel.auth.AuthIntent
import com.fitai.app.viewmodel.auth.AuthMode
import com.fitai.app.viewmodel.auth.AuthState
import org.koin.compose.viewmodel.koinViewModel

/**
 * [funnel] is the mandatory sign-up after the paywall: there is no back, only
 * [onContinueOffline] when Firebase is not configured on this build.
 */
@Composable
fun AuthScreen(
    signIn: Boolean,
    funnel: Boolean,
    onBack: () -> Unit,
    onCompleted: (SignInOutcome) -> Unit,
    onContinueOffline: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onIntent(AuthIntent.Init(signIn = signIn, funnel = funnel))
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.Completed -> onCompleted(effect.outcome)
                AuthEffect.NavigateBack -> onBack()
            }
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = true,
        onBackCompleted = { viewModel.onIntent(AuthIntent.Back) },
    )

    val launchGoogle = rememberGoogleSignInLauncher { result ->
        result.onSuccess { viewModel.onIntent(AuthIntent.GoogleIdToken(it)) }
        result.onFailure { viewModel.onIntent(AuthIntent.ProviderError(it.message)) }
    }
    val launchApple = rememberAppleSignInLauncher { result ->
        result.onSuccess { (id, nonce) -> viewModel.onIntent(AuthIntent.AppleTokens(id, nonce)) }
        result.onFailure { viewModel.onIntent(AuthIntent.ProviderError(it.message)) }
    }

    AuthContent(
        state = state,
        onIntent = viewModel::onIntent,
        onGoogle = launchGoogle,
        onApple = launchApple,
        onContinueOffline = onContinueOffline,
        modifier = modifier,
    )
}

@Composable
private fun AuthContent(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    onContinueOffline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showBack = state.mode != AuthMode.SignUpOptions || !state.funnel
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FitAIColors.Background)
            .background(Brush.verticalGradient(0f to FitAIColors.Primary.copy(alpha = 0.1f), 0.5f to FitAIColors.Background)),
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .padding(horizontal = FitAISpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showBack) {
                    IconButton(
                        onClick = { onIntent(AuthIntent.Back) },
                        enabled = !state.busy,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = FitAIColors.Surface),
                    ) {
                        Icon(FitAIIcons.ChevronLeft, contentDescription = LocalStrings.current.actionBack, tint = FitAIColors.OnSurface)
                    }
                }
            }
            when (state.mode) {
                AuthMode.SignUpOptions -> SignUpOptions(state, onIntent, onGoogle, onApple, onContinueOffline)
                AuthMode.SignUpEmail -> EmailForm(state, onIntent, signIn = false, onGoogle = onGoogle, onApple = onApple)
                AuthMode.SignIn -> EmailForm(state, onIntent, signIn = true, onGoogle = onGoogle, onApple = onApple)
            }
        }
        if (state.busy) {
            CircularProgressIndicator(color = FitAIColors.Primary, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SignUpOptions(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
    onContinueOffline: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FitAISpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(96.dp).background(FitAIColors.Primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(FitAIIcons.VerifiedUser, contentDescription = null, tint = FitAIColors.Primary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(FitAISpacing.xl))
        Text(
            strings.authSignupTitle,
            style = MaterialTheme.typography.displayMedium,
            color = FitAIColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FitAISpacing.sm))
        Text(
            strings.authSignupSubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = FitAIColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = FitAISpacing.lg, vertical = FitAISpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        if (!state.isFirebaseAvailable) {
            Text(strings.firebaseUnavailable, style = MaterialTheme.typography.bodySmall, color = FitAIColors.Warning, textAlign = TextAlign.Center)
        }
        state.errorMessage?.let { ErrorText(mapAuthError(it, strings)) }
        ProviderButton(strings.profileApple, enabled = state.isFirebaseAvailable && !state.busy, onClick = onApple) {
            Icon(FitAIIcons.AppleLogo, contentDescription = null, tint = FitAIColors.OnSurface, modifier = Modifier.size(20.dp))
        }
        ProviderButton(strings.profileGoogle, enabled = state.isFirebaseAvailable && !state.busy, onClick = onGoogle) {
            Image(FitAIIcons.GoogleLogo, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        FitAIButton(
            text = strings.authContinueEmail,
            onClick = { onIntent(AuthIntent.ShowMode(AuthMode.SignUpEmail)) },
            leadingIcon = FitAIIcons.Mail,
            enabled = state.isFirebaseAvailable && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.funnel && !state.isFirebaseAvailable) {
            FitAIButton(
                text = strings.authOfflineContinue,
                onClick = onContinueOffline,
                variant = FitAIButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SwitchModeLink(
            prefix = strings.welcomeHaveAccountPrefix,
            link = strings.welcomeSignInLink,
            onClick = { onIntent(AuthIntent.ShowMode(AuthMode.SignIn)) },
        )
        Text(strings.authLegal, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.EmailForm(
    state: AuthState,
    onIntent: (AuthIntent) -> Unit,
    signIn: Boolean,
    onGoogle: () -> Unit,
    onApple: () -> Unit,
) {
    val strings = LocalStrings.current
    val enabled = state.isFirebaseAvailable && !state.busy
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FitAISpacing.lg)
            .padding(bottom = FitAISpacing.lg),
        verticalArrangement = Arrangement.spacedBy(FitAISpacing.md),
    ) {
        Text(
            if (signIn) strings.authSignInHeading else strings.authEmailHeading,
            style = MaterialTheme.typography.displayMedium,
            color = FitAIColors.OnSurface,
        )
        Spacer(Modifier.height(FitAISpacing.sm))
        if (signIn) {
            ProviderButton(strings.profileGoogle, enabled = enabled, onClick = onGoogle) {
                Image(FitAIIcons.GoogleLogo, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            ProviderButton(strings.profileApple, enabled = enabled, onClick = onApple) {
                Icon(FitAIIcons.AppleLogo, contentDescription = null, tint = FitAIColors.OnSurface, modifier = Modifier.size(20.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FitAISpacing.md)) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = FitAIColors.SurfaceVariant)
                Text(strings.authOr, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
                HorizontalDivider(modifier = Modifier.weight(1f), color = FitAIColors.SurfaceVariant)
            }
        }
        if (!state.isFirebaseAvailable) {
            Text(strings.firebaseUnavailable, style = MaterialTheme.typography.bodySmall, color = FitAIColors.Warning)
        }
        FitAITextField(
            value = state.email,
            onValueChange = { onIntent(AuthIntent.EmailChanged(it)) },
            label = strings.sampleEmailLabel,
            placeholder = strings.sampleEmailPlaceholder,
            leadingIcon = FitAIIcons.Mail,
            enabled = !state.busy,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        FitAITextField(
            value = state.password,
            onValueChange = { onIntent(AuthIntent.PasswordChanged(it)) },
            label = strings.authPassword,
            placeholder = if (signIn) null else strings.authPasswordPlaceholder,
            leadingIcon = FitAIIcons.Lock,
            enabled = !state.busy,
            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { onIntent(AuthIntent.TogglePasswordVisible) }) {
                    Icon(
                        imageVector = if (state.passwordVisible) FitAIIcons.VisibilityOff else FitAIIcons.Visibility,
                        contentDescription = if (state.passwordVisible) strings.authHidePassword else strings.authShowPassword,
                        tint = FitAIColors.OnSurfaceMuted,
                    )
                }
            },
        )
        if (signIn) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = { onIntent(AuthIntent.ForgotPassword) }, enabled = enabled) {
                    Text(strings.authForgotPassword, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
                }
            }
        }
        if (state.resetSent) {
            Text(strings.authResetSent, style = MaterialTheme.typography.bodySmall, color = FitAIColors.Success)
        }
        state.errorMessage?.let { ErrorText(mapAuthError(it, strings)) }
        FitAIButton(
            text = if (signIn) strings.authSignInCta else strings.authCreateAccount,
            onClick = { onIntent(AuthIntent.Submit) },
            enabled = enabled && state.email.isNotBlank() && state.password.isNotBlank(),
            trailingIcon = FitAIIcons.ArrowForward,
            modifier = Modifier.fillMaxWidth(),
        )
        if (signIn) {
            SwitchModeLink(
                prefix = strings.authNoAccount,
                link = strings.authSignUpLink,
                onClick = { onIntent(AuthIntent.ShowMode(AuthMode.SignUpOptions)) },
            )
        } else {
            Text(strings.authLegal, style = MaterialTheme.typography.labelSmall, color = FitAIColors.OnSurfaceMuted)
        }
    }
}

@Composable
private fun ProviderButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = FitAIShapes.button,
        color = FitAIColors.Surface,
        border = BorderStroke(1.dp, FitAIColors.SurfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(FitAISpacing.sm, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) FitAIColors.OnSurface else FitAIColors.OnSurfaceMuted.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun SwitchModeLink(prefix: String, link: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(prefix, style = MaterialTheme.typography.bodyMedium, color = FitAIColors.OnSurfaceMuted)
        TextButton(onClick = onClick) {
            Text(link, style = MaterialTheme.typography.titleMedium, color = FitAIColors.Primary)
        }
    }
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun mapAuthError(key: String, strings: Strings): String = when (key) {
    "firebase_unavailable" -> strings.firebaseUnavailable
    "validation_email_password_required" -> strings.validationEmailPasswordRequired
    "validation_password_short" -> strings.validationPasswordShort
    "validation_email_required" -> strings.validationEmailRequired
    "sync_failed" -> strings.syncFailed
    "apple_unsupported_android" -> strings.appleUnsupportedAndroid
    "error_unknown" -> strings.errorUnknown
    else -> key.ifBlank { strings.errorUnknown }
}

@Preview
@Composable
private fun AuthPreview() {
    FitAITheme {
        AuthContent(
            state = AuthState(funnel = true, isFirebaseAvailable = true),
            onIntent = {},
            onGoogle = {},
            onApple = {},
            onContinueOffline = {},
        )
    }
}
