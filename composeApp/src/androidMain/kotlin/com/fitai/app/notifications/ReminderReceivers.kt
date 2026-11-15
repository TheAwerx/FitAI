package com.fitai.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderAlarms.ACTION_FIRE) return
        val kind = ReminderKind.entries.getOrNull(intent.getIntExtra(ReminderAlarms.EXTRA_KIND, -1)) ?: return
        val index = intent.getIntExtra(ReminderAlarms.EXTRA_INDEX, 0)
        val schedule = ReminderScheduleStore(context).load() ?: return
        ReminderAlarms.message(schedule, kind, index)?.let { (title, body) ->
            ReminderNotifications.show(context, kind, kind.requestBase + index, title, body)
        }
        if (kind.repeating) {
            ReminderAlarms.armNext(context, schedule, kind, System.currentTimeMillis() + RE_ARM_GAP_MILLIS)
        }
    }

    private companion object {
        const val RE_ARM_GAP_MILLIS = 60_000L
    }
}

/**
 * Alarms are dropped on reboot, and their absolute times go stale when the clock or time zone changes;
 * re-arm from the stored schedule so reminders stay at the user's local time.
 */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESTORE_ACTIONS) return
        val schedule = ReminderScheduleStore(context).load() ?: return
        ReminderNotifications.ensureChannels(context)
        ReminderAlarms.cancelAll(context)
        ReminderAlarms.armAll(context, schedule, System.currentTimeMillis())
    }

    private companion object {
        val RESTORE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
