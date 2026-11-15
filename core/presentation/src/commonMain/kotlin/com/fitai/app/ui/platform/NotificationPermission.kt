package com.fitai.app.ui.platform

import androidx.compose.runtime.Composable

/**
 * Invoke the returned lambda to ask for notification permission; [onResult] gets whether
 * notifications may be shown. Already-granted or already-denied states resolve without a prompt.
 */
@Composable
expect fun rememberNotificationPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit
