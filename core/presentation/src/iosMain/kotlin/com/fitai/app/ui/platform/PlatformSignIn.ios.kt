package com.fitai.app.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberGoogleSignInLauncher(onToken: (Result<String>) -> Unit): () -> Unit =
    remember(onToken) {
        {
            onToken(Result.failure(IllegalStateException("ios_google_sign_in_not_configured")))
        }
    }

@Composable
actual fun rememberAppleSignInLauncher(onResult: (Result<Pair<String, String>>) -> Unit): () -> Unit =
    remember(onResult) {
        {
            // Native Sign in with Apple + SHA256 nonce needs platform bindings; enable when shipping iOS with Firebase + capability.
            onResult(Result.failure(IllegalStateException("ios_apple_sign_in_not_configured")))
        }
    }
