package com.fitai.app.notifications

import com.fitai.app.ui.notifications.NotificationDeepLink
import com.fitai.app.util.epochMillis
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * Repeating reminders use repeating calendar triggers. While a skip date is still ahead (first week of
 * onboarding), they are expanded into dated one-off requests instead, because a repeating trigger
 * cannot skip a day; the next launch-time reschedule switches them back once the skips have passed.
 */
class IosReminderScheduler : ReminderScheduler {
    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    private class Pending(
        val firstFireAtMillis: Long,
        val identifier: String,
        val title: String,
        val body: String,
        val dateComponents: NSDateComponents,
        val repeats: Boolean,
        /** Screen a tap opens (`userInfo["link"]`, read by the Swift notification delegate). */
        val link: NotificationDeepLink? = null,
    )

    override suspend fun schedule(schedule: ReminderSchedule) {
        center.removeAllPendingNotificationRequests()
        val now = epochMillis()
        buildPending(schedule, now)
            .sortedBy { it.firstFireAtMillis }
            .take(MAX_PENDING_REQUESTS)
            .forEach { pending ->
                val content = UNMutableNotificationContent().apply {
                    setTitle(pending.title)
                    setBody(pending.body)
                    setSound(UNNotificationSound.defaultSound)
                    pending.link?.let { setUserInfo(mapOf<Any?, Any?>(NotificationDeepLink.EXTRA_KEY to it.code)) }
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    pending.dateComponents,
                    repeats = pending.repeats,
                )
                val request = UNNotificationRequest.requestWithIdentifier(pending.identifier, content, trigger)
                center.addNotificationRequest(request, withCompletionHandler = null)
            }
    }

    override suspend fun cancelAll() {
        center.removeAllPendingNotificationRequests()
    }

    override suspend fun isPermissionGranted(): Boolean = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            continuation.resume(
                status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional ||
                    status == UNAuthorizationStatusEphemeral,
            )
        }
    }

    private fun buildPending(schedule: ReminderSchedule, now: Long): List<Pending> = buildList {
        schedule.workoutReminder?.let { w ->
            addAll(repeating("$ID_PREFIX.workout", w.days, w.hour, w.minute, w.skipDates, w.title, w.body, now, NotificationDeepLink.Workout))
        }
        schedule.mealNudge?.let { m ->
            addAll(repeating("$ID_PREFIX.meal", null, m.hour, m.minute, m.skipDates, m.title, m.body, now, NotificationDeepLink.MealLog))
        }
        schedule.onboardingSequence?.let { sequence ->
            sequence.items.forEachIndexed { index, item ->
                val at = ReminderTimes.sequenceFireAt(sequence.anchorEpochMillis, item)
                if (at > now) add(oneOff("$ID_PREFIX.sequence.$index", at, item.title, item.body))
            }
        }
        schedule.trialEndReminder?.takeIf { it.atEpochMillis > now }?.let { trial ->
            add(oneOff("$ID_PREFIX.trial", trial.atEpochMillis, trial.title, trial.body))
        }
    }

    private fun repeating(
        prefix: String,
        days: Set<Int>?,
        hour: Int,
        minute: Int,
        skipDates: Set<String>,
        title: String,
        body: String,
        now: Long,
        link: NotificationDeepLink,
    ): List<Pending> {
        if (ReminderTimes.hasUpcomingSkip(now, skipDates)) {
            return ReminderTimes.occurrences(
                nowMillis = now,
                hour = hour,
                minute = minute,
                days = days,
                skipDates = skipDates,
                limit = ONE_OFF_EXPANSION_LIMIT,
                horizonDays = ONE_OFF_EXPANSION_DAYS,
            ).map { at -> oneOff("$prefix.$at", at, title, body, link) }
        }
        val weekdays = days ?: run {
            val next = ReminderTimes.nextOccurrence(now, hour, minute, days = null) ?: return emptyList()
            return listOf(Pending(next, "$prefix.daily", title, body, timeComponents(null, hour, minute), repeats = true, link = link))
        }
        return weekdays.mapNotNull { isoDay ->
            val next = ReminderTimes.nextOccurrence(now, hour, minute, setOf(isoDay)) ?: return@mapNotNull null
            Pending(next, "$prefix.$isoDay", title, body, timeComponents(isoDay, hour, minute), repeats = true, link = link)
        }
    }

    private fun oneOff(
        identifier: String,
        atMillis: Long,
        title: String,
        body: String,
        link: NotificationDeepLink? = null,
    ): Pending {
        val date = NSDate.dateWithTimeIntervalSince1970(atMillis / 1000.0)
        val components = NSCalendar.currentCalendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = date,
        )
        return Pending(atMillis, identifier, title, body, components, repeats = false, link = link)
    }

    private fun timeComponents(isoDay: Int?, hour: Int, minute: Int): NSDateComponents =
        NSDateComponents().apply {
            isoDay?.let { weekday = iosWeekday(it).toLong() }
            this.hour = hour.toLong()
            this.minute = minute.toLong()
        }

    private companion object {
        const val ID_PREFIX = "fitai"
        /** iOS keeps only the 64 soonest pending requests per app. */
        const val MAX_PENDING_REQUESTS = 64
        const val ONE_OFF_EXPANSION_LIMIT = 21
        const val ONE_OFF_EXPANSION_DAYS = 21

        /** ISO 1 = Monday … 7 = Sunday → NSCalendar 1 = Sunday … 7 = Saturday. */
        fun iosWeekday(isoDay: Int): Int = isoDay % 7 + 1
    }
}
