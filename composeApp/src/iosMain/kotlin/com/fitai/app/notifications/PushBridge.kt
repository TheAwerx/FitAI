package com.fitai.app.notifications

import com.fitai.app.ui.notifications.PendingNotificationLinks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSBundle
import kotlin.time.Duration.Companion.seconds

/**
 * Called from Swift (`iosApp/iOSApp.swift`), where FirebaseMessaging lives: GitLive has no messaging
 * module, so the FCM token and notification taps cross over here. Swift sees it as `PushBridge.shared`.
 */
object PushBridge {
    private val token = MutableStateFlow<String?>(null)
    private val refreshes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    private var deleteTokenHandler: (() -> Unit)? = null

    internal val currentToken: StateFlow<String?> = token.asStateFlow()
    internal val tokenRefreshes: Flow<String> = refreshes.asSharedFlow()

    /** `MessagingDelegate.messaging(_:didReceiveRegistrationToken:)` */
    fun onToken(token: String) {
        if (this.token.value == token) return
        this.token.value = token
        // Also the first token of a launch: it may arrive after the launch sync gave up waiting.
        refreshes.tryEmit(token)
    }

    /** `UNUserNotificationCenterDelegate` tap; [link] is `userInfo["link"]`. */
    fun onNotificationOpened(link: String?) {
        PendingNotificationLinks.offer(link)
    }

    /** Swift registers how to drop the FCM token (`Messaging.messaging().deleteToken`). */
    fun setDeleteTokenHandler(handler: () -> Unit) {
        deleteTokenHandler = handler
    }

    internal fun deleteToken() {
        token.value = null
        deleteTokenHandler?.invoke()
    }
}

class IosPushTokenProvider : PushTokenProvider {
    override val platform: String = "ios"
    override val appVersion: String =
        (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String).orEmpty()
    override val tokenRefreshes: Flow<String> = PushBridge.tokenRefreshes

    /** The APNs → FCM token can take a moment after launch; wait briefly for it. */
    override suspend fun currentToken(): String? =
        PushBridge.currentToken.value ?: withTimeoutOrNull(TOKEN_WAIT) { PushBridge.currentToken.filterNotNull().first() }

    override suspend fun invalidateToken() {
        PushBridge.deleteToken()
    }

    private companion object {
        val TOKEN_WAIT = 5.seconds
    }
}
