package com.fitai.app.notifications

import com.fitai.app.ui.notifications.NotificationDeepLink
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Server pushes (firebase/functions/src/notifications). In the background the system shows them on the
 * `fitai_smart_nudges` channel and a tap opens MainActivity through the payload's click action; while
 * FitAI is in the foreground they arrive here and are shown the same way.
 */
class FitAiMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        PushTokenEvents.emit(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: return
        val body = message.notification?.body.orEmpty()
        ReminderNotifications.showPush(
            context = this,
            tag = message.data["type"] ?: message.messageId ?: title,
            title = title,
            body = body,
            link = message.data[NotificationDeepLink.EXTRA_KEY],
        )
    }
}
