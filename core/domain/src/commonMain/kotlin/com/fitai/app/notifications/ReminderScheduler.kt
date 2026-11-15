package com.fitai.app.notifications

/** Platform local-notification scheduler. [schedule] replaces everything previously scheduled. */
interface ReminderScheduler {
    suspend fun schedule(schedule: ReminderSchedule)
    suspend fun cancelAll()
    suspend fun isPermissionGranted(): Boolean
}

/** Persists when reminders were first scheduled so the D0–D7 sequence keeps its original days. */
interface ReminderStateRepository {
    var sequenceAnchorEpochMillis: Long?
}
