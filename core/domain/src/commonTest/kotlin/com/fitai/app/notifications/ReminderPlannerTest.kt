package com.fitai.app.notifications

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderPlannerTest {
    private val utc = TimeZone.UTC
    private val message = ReminderMessage("t", "b")
    private val texts = ReminderTexts(message, message, message, message, message, message, message, message, message)

    // Monday 2026-11-09 09:00 UTC.
    private val mondayMorning = ReminderTimes.epochMillisAt(LocalDate(2026, 11, 9), 9, 0, utc)

    private fun plan(now: Long = mondayMorning, anchor: Long = mondayMorning, trialEnd: Long? = null) =
        ReminderPlanner.plan(now, 18, 0, setOf(1, 3, 5), anchor, trialEnd, texts, utc)

    @Test
    fun sequenceStartsTwoHoursAfterAnchorAndSkipsOverlappingRepeats() {
        val schedule = plan()
        val items = schedule.onboardingSequence!!.items
        assertEquals(listOf(0, 1, 2, 3, 5, 7), items.map { it.dayOffset })
        assertEquals(11, items.first().hour)
        // Day 0 (Mon) is a training day and day 3 (Thu) is not; day 1 lunch replaces the meal nudge.
        assertEquals(setOf("2026-11-09"), schedule.workoutReminder!!.skipDates)
        assertEquals(setOf("2026-11-10"), schedule.mealNudge!!.skipDates)
    }

    @Test
    fun pastSequenceItemsAreDropped() {
        val tenDaysLater = mondayMorning + 10 * 24 * 60 * 60 * 1000L
        val schedule = plan(now = tenDaysLater)
        assertNull(schedule.onboardingSequence)
        assertTrue(schedule.workoutReminder!!.skipDates.isEmpty())
    }

    @Test
    fun nightTrialReminderMovesToPreviousEvening() {
        val expires = ReminderTimes.epochMillisAt(LocalDate(2026, 11, 12), 3, 0, utc)
        val reminder = plan(trialEnd = expires).trialEndReminder!!
        assertEquals(ReminderTimes.epochMillisAt(LocalDate(2026, 11, 10), 20, 0, utc), reminder.atEpochMillis)
    }

    @Test
    fun weeklyOccurrencesHonourSkipDates() {
        val next = ReminderTimes.nextOccurrence(mondayMorning, 18, 0, setOf(1, 3), setOf("2026-11-09"), utc)
        assertEquals(ReminderTimes.epochMillisAt(LocalDate(2026, 11, 11), 18, 0, utc), next)
    }
}
