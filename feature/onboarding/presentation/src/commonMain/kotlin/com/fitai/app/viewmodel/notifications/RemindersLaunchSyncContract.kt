package com.fitai.app.viewmodel.notifications

import com.fitai.app.notifications.ReminderTexts

data class RemindersLaunchSyncState(val synced: Boolean = false)

sealed interface RemindersLaunchSyncIntent {
    data class Sync(val texts: ReminderTexts) : RemindersLaunchSyncIntent
}
