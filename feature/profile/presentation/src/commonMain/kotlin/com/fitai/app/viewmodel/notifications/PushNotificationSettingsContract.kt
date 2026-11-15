package com.fitai.app.viewmodel.notifications

import com.fitai.app.notifications.PushNotificationType
import com.fitai.app.notifications.PushNotificationTypes
import com.fitai.app.notifications.ReminderTexts

data class PushNotificationSettingsState(
    val types: PushNotificationTypes = PushNotificationTypes(),
    /** Null until checked; false shows the "not registered yet" hint. */
    val pushActive: Boolean? = null,
    val busy: Boolean = false,
)

sealed interface PushNotificationSettingsIntent {
    /** Re-reads the switches and whether this device is registered (after the reminder switch changed). */
    data object Refresh : PushNotificationSettingsIntent

    /** [texts] rebuild the local reminders, whose meal reminder follows the meal switch. */
    data class Toggle(
        val type: PushNotificationType,
        val enabled: Boolean,
        val texts: ReminderTexts,
    ) : PushNotificationSettingsIntent
}
