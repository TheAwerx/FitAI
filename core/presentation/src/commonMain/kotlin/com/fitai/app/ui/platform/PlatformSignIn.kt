package com.fitai.app.ui.platform

import androidx.compose.runtime.Composable

/** Invoke returned lambda to start Google sign-in; calls [onToken] with ID token or failure. */
@Composable
expect fun rememberGoogleSignInLauncher(onToken: (Result<String>) -> Unit): () -> Unit

/** Invoke returned lambda to start Sign in with Apple; id token and raw nonce for Firebase. */
@Composable
expect fun rememberAppleSignInLauncher(onResult: (Result<Pair<String, String>>) -> Unit): () -> Unit
