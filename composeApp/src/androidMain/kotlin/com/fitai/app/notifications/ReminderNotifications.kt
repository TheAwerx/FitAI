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
import com.fitai.app.R
import com.fitai.app.localization.Strings
import com.fitai.app.localization.StringsEn
import com.fitai.app.localization.StringsTr
import com.fitai.app.util.appLocale

internal object ReminderNotifications {
    private const val CHANNEL_WORKOUT = "fitai_workout_reminders"
    private const val CHANNEL_MEALS = "fitai_meal_reminders"
    private const val CHANNEL_PROGRAM = "fitai_program"
    private const val ACCENT_COLOR = 0xFF46F1C5.toInt()

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
        if (!canPost(context)) return
        ensureChannels(context)
        val channel = when (kind) {
            ReminderKind.Workout -> CHANNEL_WORKOUT
            ReminderKind.Meal -> CHANNEL_MEALS
            ReminderKind.Sequence, ReminderKind.Trial -> CHANNEL_PROGRAM
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ?: return
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_fitai)
            .setColor(ACCENT_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between canPost() and notify().
        }
    }
}
