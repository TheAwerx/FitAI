package com.fitai.app.notifications

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Builds the full schedule from plain inputs. Onboarding messages fire instead of (not in addition to)
 * the repeating workout / meal reminder on the same day; that is what the skip dates encode.
 * [includeMealNudge] is false when the meal reminder is switched off, or when server pushes own it
 * (they only fire when nothing was logged; see firebase/SCHEMA.md, "Push notifications").
 */
object ReminderPlanner {
    const val MEAL_NUDGE_HOUR = 12
    const val MEAL_NUDGE_MINUTE = 30
    private const val MORNING_HOUR = 10
    private const val FIRST_SESSION_DELAY_MILLIS = 2 * 60 * 60 * 1000L
    private val DAYTIME_HOURS = 8..21
    private const val TRIAL_REMINDER_LEAD_MILLIS = 24 * 60 * 60 * 1000L
    private const val TRIAL_REMINDER_EVENING_HOUR = 20
    private const val TRIAL_REMINDER_EARLIEST_HOUR = 9
    private const val TRIAL_REMINDER_LATEST_HOUR = 21

    private enum class Slot { WorkoutTime, Lunch, Morning }

    fun plan(
        nowMillis: Long,
        reminderHour: Int,
        reminderMinute: Int,
        trainingDays: Set<Int>,
        sequenceAnchorMillis: Long,
        trialExpiresAtMillis: Long?,
        texts: ReminderTexts,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        includeMealNudge: Boolean = true,
    ): ReminderSchedule {
        val workoutSkips = mutableSetOf<String>()
        val mealSkips = mutableSetOf<String>()
        val sequenceItems = mutableListOf<SequenceReminder>()

        firstSessionItem(sequenceAnchorMillis, texts.day0FirstSession, timeZone)?.let { sequenceItems += it }
        listOf(
            Triple(1, Slot.Lunch, texts.day1MealPhoto),
            Triple(2, Slot.Morning, texts.day2RestDay),
            Triple(3, Slot.WorkoutTime, texts.day3Overload),
            Triple(5, Slot.WorkoutTime, texts.day5TodayMode),
            Triple(7, Slot.Morning, texts.day7WeeklyReport),
        ).forEach { (offset, slot, message) ->
            val (hour, minute) = when (slot) {
                Slot.WorkoutTime -> reminderHour to reminderMinute
                Slot.Lunch -> MEAL_NUDGE_HOUR to MEAL_NUDGE_MINUTE
                Slot.Morning -> MORNING_HOUR to 0
            }
            sequenceItems += SequenceReminder(offset, hour, minute, message.title, message.body)
        }

        val upcoming = sequenceItems.filter { ReminderTimes.sequenceFireAt(sequenceAnchorMillis, it, timeZone) > nowMillis }
        val anchorDate = ReminderTimes.localDateOf(sequenceAnchorMillis, timeZone)
        upcoming.forEach { item ->
            val date = anchorDate.plus(DatePeriod(days = item.dayOffset))
            val isWorkoutSlot = item.dayOffset == 0 || (item.hour == reminderHour && item.minute == reminderMinute)
            if (isWorkoutSlot && date.dayOfWeek.isoDayNumber in trainingDays) workoutSkips += date.toString()
            if (item.hour == MEAL_NUDGE_HOUR && item.minute == MEAL_NUDGE_MINUTE) mealSkips += date.toString()
        }

        return ReminderSchedule(
            workoutReminder = trainingDays.takeIf { it.isNotEmpty() }?.let { days ->
                WorkoutReminder(days, reminderHour, reminderMinute, texts.workout.title, texts.workout.body, workoutSkips)
            },
            mealNudge = if (includeMealNudge) {
                MealNudge(MEAL_NUDGE_HOUR, MEAL_NUDGE_MINUTE, texts.meal.title, texts.meal.body, mealSkips)
            } else {
                null
            },
            onboardingSequence = upcoming.takeIf { it.isNotEmpty() }?.let { OnboardingSequence(sequenceAnchorMillis, it) },
            trialEndReminder = trialExpiresAtMillis
                ?.let { trialReminderAt(it, nowMillis, timeZone) }
                ?.let { TrialEndReminder(it, texts.trialEnd.title, texts.trialEnd.body) },
        )
    }

    /** "Plan ready, session not started": two hours after the anchor, dropped outside daytime. */
    private fun firstSessionItem(anchorMillis: Long, message: ReminderMessage, timeZone: TimeZone): SequenceReminder? {
        val anchorDate = ReminderTimes.localDateOf(anchorMillis, timeZone)
        val fireAt = ReminderTimes.localDateTimeOf(anchorMillis + FIRST_SESSION_DELAY_MILLIS, timeZone)
        if (fireAt.date != anchorDate || fireAt.hour !in DAYTIME_HOURS) return null
        return SequenceReminder(0, fireAt.hour, fireAt.minute, message.title, message.body)
    }

    /** 24 h before expiry, moved earlier (never later) when that lands at night. */
    private fun trialReminderAt(expiresAtMillis: Long, nowMillis: Long, timeZone: TimeZone): Long? {
        val raw = expiresAtMillis - TRIAL_REMINDER_LEAD_MILLIS
        val local = ReminderTimes.localDateTimeOf(raw, timeZone)
        val adjusted = when {
            local.hour < TRIAL_REMINDER_EARLIEST_HOUR ->
                ReminderTimes.epochMillisAt(local.date.minus(DatePeriod(days = 1)), TRIAL_REMINDER_EVENING_HOUR, 0, timeZone)
            local.hour > TRIAL_REMINDER_LATEST_HOUR ->
                ReminderTimes.epochMillisAt(local.date, TRIAL_REMINDER_EVENING_HOUR, 0, timeZone)
            else -> raw
        }
        return when {
            adjusted > nowMillis -> adjusted
            raw > nowMillis -> raw
            else -> null
        }
    }

    /** Spread used when there is no active plan yet, keyed by the onboarding commitment. */
    fun defaultTrainingDays(daysPerWeek: Int?): Set<Int> = when (daysPerWeek?.coerceIn(1, 7)) {
        1 -> setOf(3)
        2 -> setOf(2, 5)
        3, null -> setOf(1, 3, 5)
        4 -> setOf(1, 2, 4, 5)
        5 -> setOf(1, 2, 3, 4, 5)
        6 -> setOf(1, 2, 3, 4, 5, 6)
        else -> (1..7).toSet()
    }
}
