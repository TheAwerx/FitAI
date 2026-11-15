package com.fitai.app.notifications

import android.content.Context
import com.fitai.app.BuildConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/** New tokens from [FitAiMessagingService.onNewToken]; nobody listens while the app UI isn't running. */
internal object PushTokenEvents {
    private val refreshes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val tokens: Flow<String> = refreshes.asSharedFlow()

    fun emit(token: String) {
        refreshes.tryEmit(token)
    }
}

/** FCM through the native SDK (GitLive has no messaging module). */
class AndroidPushTokenProvider(context: Context) : PushTokenProvider {
    private val appContext = context.applicationContext

    override val platform: String = "android"
    override val appVersion: String = BuildConfig.VERSION_NAME
    override val tokenRefreshes: Flow<String> = PushTokenEvents.tokens

    private fun messagingOrNull(): FirebaseMessaging? =
        if (FirebaseApp.getApps(appContext).isEmpty()) null else runCatching { FirebaseMessaging.getInstance() }.getOrNull()

    override suspend fun currentToken(): String? {
        val messaging = messagingOrNull() ?: return null
        return withTimeoutOrNull(TOKEN_TIMEOUT) { messaging.token.awaitOrNull() }
    }

    override suspend fun invalidateToken() {
        val messaging = messagingOrNull() ?: return
        withTimeoutOrNull(TOKEN_TIMEOUT) { messaging.deleteToken().awaitOrNull() }
    }

    private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            continuation.resume(if (task.isSuccessful) task.result else null)
        }
    }

    private companion object {
        val TOKEN_TIMEOUT = 5.seconds
    }
}
