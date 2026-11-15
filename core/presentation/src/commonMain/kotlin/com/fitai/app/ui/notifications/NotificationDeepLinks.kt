package com.fitai.app.ui.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Screens a notification tap can open. [code] is the `link` value in the push data payload
 * (firebase/functions/src/notifications/types.ts `DeepLink`) and in local reminder intents.
 */
enum class NotificationDeepLink(val code: String) {
    Workout("workout"),
    MealLog("meal_log"),
    Home("home"),
    ;

    companion object {
        /** Key of the link in the push data payload, the Android intent extras and the iOS `userInfo`. */
        const val EXTRA_KEY = "link"

        fun fromCode(code: String?): NotificationDeepLink? = entries.firstOrNull { it.code == code }
    }
}

/**
 * The last tapped notification's link until the main tabs handle it. Platform entry points (Android
 * `MainActivity`, the iOS notification delegate) run before Compose, sometimes before Koin, so this is a
 * process-wide holder rather than an injected one.
 */
object PendingNotificationLinks {
    private val _pending = MutableStateFlow<NotificationDeepLink?>(null)
    val pending: StateFlow<NotificationDeepLink?> = _pending.asStateFlow()

    /** Unknown codes are ignored (a newer server may send links this build doesn't know). */
    fun offer(code: String?) {
        NotificationDeepLink.fromCode(code)?.let { _pending.value = it }
    }

    fun consume(): NotificationDeepLink? {
        val link = _pending.value
        _pending.value = null
        return link
    }
}
