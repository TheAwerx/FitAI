package com.fitai.app.notifications

import android.content.Context

class AndroidReminderScheduler(context: Context) : ReminderScheduler {
    private val appContext = context.applicationContext
    private val store = ReminderScheduleStore(appContext)

    override suspend fun schedule(schedule: ReminderSchedule) {
        ReminderAlarms.cancelAll(appContext)
        store.save(schedule)
        ReminderNotifications.ensureChannels(appContext)
        ReminderAlarms.armAll(appContext, schedule, System.currentTimeMillis())
    }

    override suspend fun cancelAll() {
        ReminderAlarms.cancelAll(appContext)
        store.clear()
    }

    override suspend fun isPermissionGranted(): Boolean = ReminderNotifications.canPost(appContext)
}
