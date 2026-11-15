package com.fitai.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fitai.app.MainActivity
import com.fitai.app.R
import com.fitai.app.localization.Strings
import com.fitai.app.localization.StringsEn
import com.fitai.app.localization.StringsTr
import com.fitai.app.ui.notifications.NotificationDeepLink
import com.fitai.app.util.appLocale

internal object ReminderNotifications {
    private const val CHANNEL_WORKOUT = "fitai_workout_reminders"
    private const val CHANNEL_MEALS = "fitai_meal_reminders"
    private const val CHANNEL_PROGRAM = "fitai_program"
    /** Server pushes; same id as `ANDROID_CHANNEL_ID` in functions/src/notifications/config.ts. */
    const val CHANNEL_SMART = "fitai_smart_nudges"
    /** Same as `ANDROID_CLICK_ACTION` in functions/src/notifications/config.ts. */
    const val ACTION_OPEN_LINK = "com.fitai.app.action.OPEN_LINK"
    private const val ACCENT_COLOR = 0xFF46F1C5.toInt()
    private const val PUSH_ID_BASE = 9000

    /** Same language rule as `ProvideFitAIStrings`; channels are created outside Compose. */
    private fun strings(): Strings = if (appLocale() == "en") StringsEn else StringsTr

    /** Idempotent; also refreshes channel names after a device language change. */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val strings = strings()
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_WORKOUT, strings.notificationsChannelWorkout, NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_MEALS, strings.notificationsChannelMeals, NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_PROGRAM, strings.notificationsChannelProgram, NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_SMART, strings.pushChannelSmart, NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun show(context: Context, kind: ReminderKind, notificationId: Int, title: String, body: String) {
        val channel = when (kind) {
            ReminderKind.Workout -> CHANNEL_WORKOUT
            ReminderKind.Meal -> CHANNEL_MEALS
            ReminderKind.Sequence, ReminderKind.Trial -> CHANNEL_PROGRAM
        }
        val link = when (kind) {
            ReminderKind.Workout -> NotificationDeepLink.Workout
            ReminderKind.Meal -> NotificationDeepLink.MealLog
            ReminderKind.Sequence, ReminderKind.Trial -> null
        }
        post(context, channel, notificationId, title, body, link?.code)
    }

    /** A server push received while FitAI is in the foreground. One notification per nudge type. */
    fun showPush(context: Context, tag: String, title: String, body: String, link: String?) {
        post(context, CHANNEL_SMART, PUSH_ID_BASE + (tag.hashCode() and 0x3FF), title, body, link)
    }

    private fun post(context: Context, channel: String, notificationId: Int, title: String, body: String, link: String?) {
        if (!canPost(context)) return
        ensureChannels(context)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_fitai)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent(context, notificationId, link))
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between canPost() and notify().
        }
    }

    /**
     * Opens MainActivity with [link] (see [NotificationDeepLink]). SINGLE_TOP + CLEAR_TOP hand it to the
     * running activity's `onNewIntent` instead of stacking a second one.
     */
    private fun contentIntent(context: Context, requestCode: Int, link: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_LINK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (link != null) intent.putExtra(NotificationDeepLink.EXTRA_KEY, link)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
