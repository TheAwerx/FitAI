package com.fitai.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.serialization.json.Json

internal enum class ReminderKind(val requestBase: Int, val repeating: Boolean) {
    Workout(1000, repeating = true),
    Meal(2000, repeating = true),
    Sequence(3000, repeating = false),
    Trial(4000, repeating = false),
}

/** Last scheduled [ReminderSchedule], kept outside Koin so receivers work before the app starts. */
internal class ReminderScheduleStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ReminderSchedule? = prefs.getString(KEY_SCHEDULE, null)?.let { raw ->
        runCatching { json.decodeFromString<ReminderSchedule>(raw) }.getOrNull()
    }

    fun save(schedule: ReminderSchedule) {
        prefs.edit().putString(KEY_SCHEDULE, json.encodeToString(schedule)).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_SCHEDULE).apply()
    }

    private companion object {
        const val PREFS_NAME = "fitai_reminders"
        const val KEY_SCHEDULE = "schedule_json"
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * Inexact `setAndAllowWhileIdle` alarms, so no SCHEDULE_EXACT_ALARM permission is needed. Repeating
 * reminders hold one alarm for their next occurrence and are re-armed by [ReminderAlarmReceiver].
 */
internal object ReminderAlarms {
    const val ACTION_FIRE = "com.fitai.app.action.REMINDER_FIRE"
    const val EXTRA_KIND = "fitai.reminder.kind"
    const val EXTRA_INDEX = "fitai.reminder.index"
    private const val MAX_SEQUENCE_ITEMS = 16

    fun armAll(context: Context, schedule: ReminderSchedule, nowMillis: Long) {
        ReminderKind.entries.filter { it.repeating }.forEach { armNext(context, schedule, it, nowMillis) }
        schedule.onboardingSequence?.let { sequence ->
            sequence.items.take(MAX_SEQUENCE_ITEMS).forEachIndexed { index, item ->
                val at = ReminderTimes.sequenceFireAt(sequence.anchorEpochMillis, item)
                if (at > nowMillis) arm(context, ReminderKind.Sequence, index, at)
            }
        }
        schedule.trialEndReminder
            ?.takeIf { it.atEpochMillis > nowMillis }
            ?.let { arm(context, ReminderKind.Trial, 0, it.atEpochMillis) }
    }

    fun armNext(context: Context, schedule: ReminderSchedule, kind: ReminderKind, nowMillis: Long) {
        val next = when (kind) {
            ReminderKind.Workout -> schedule.workoutReminder?.let {
                ReminderTimes.nextOccurrence(nowMillis, it.hour, it.minute, it.days, it.skipDates)
            }
            ReminderKind.Meal -> schedule.mealNudge?.let {
                ReminderTimes.nextOccurrence(nowMillis, it.hour, it.minute, days = null, skipDates = it.skipDates)
            }
            ReminderKind.Sequence, ReminderKind.Trial -> null
        }
        if (next != null) arm(context, kind, 0, next)
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        ReminderKind.entries.forEach { kind ->
            val count = if (kind == ReminderKind.Sequence) MAX_SEQUENCE_ITEMS else 1
            repeat(count) { index ->
                pendingIntent(context, kind, index, PendingIntent.FLAG_NO_CREATE)?.let {
                    alarmManager.cancel(it)
                    it.cancel()
                }
            }
        }
    }

    fun message(schedule: ReminderSchedule, kind: ReminderKind, index: Int): Pair<String, String>? = when (kind) {
        ReminderKind.Workout -> schedule.workoutReminder?.let { it.title to it.body }
        ReminderKind.Meal -> schedule.mealNudge?.let { it.title to it.body }
        ReminderKind.Sequence -> schedule.onboardingSequence?.items?.getOrNull(index)?.let { it.title to it.body }
        ReminderKind.Trial -> schedule.trialEndReminder?.let { it.title to it.body }
    }

    private fun arm(context: Context, kind: ReminderKind, index: Int, atMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, kind, index, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
    }

    private fun pendingIntent(context: Context, kind: ReminderKind, index: Int, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_KIND, kind.ordinal)
            .putExtra(EXTRA_INDEX, index)
        return PendingIntent.getBroadcast(
            context,
            kind.requestBase + index,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
