package com.fitai.app.engagement

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreaksTest {

    // 2026-11-02 is a Monday (ISO week 2026-W45).
    private fun nov(day: Int) = LocalDate(2026, 11, day)
    private val monWedFri = setOf(1, 3, 5)

    @Test
    fun noActivityMeansNoStreak() {
        val result = Streaks.compute(nov(11), emptySet(), monWedFri)
        assertEquals(0, result.status.currentDays)
        assertNull(result.status.longestDays)
        assertTrue(result.status.freezeAvailableThisWeek)
    }

    @Test
    fun restDaysKeepAndCountTheStreak() {
        // Mon, Wed, Fri active; Tue, Thu, Sat, Sun are rest days.
        val active = setOf(nov(2), nov(4), nov(6))
        val result = Streaks.compute(nov(8), active, monWedFri, milestones = listOf(7))
        assertEquals(7, result.status.currentDays)
        assertEquals(nov(8), result.milestoneDates[7])
        assertTrue(result.frozenWeeks.isEmpty())
    }

    @Test
    fun inactiveTrainingDayTodayDoesNotBreak() {
        val active = setOf(nov(2), nov(4), nov(6))
        val result = Streaks.compute(nov(9), active, monWedFri)
        assertEquals(7, result.status.currentDays)
        assertFalse(result.status.todayActive)
    }

    @Test
    fun firstMissedTrainingDayInWeekUsesFreeze() {
        // Wed 4th missed; freeze keeps the streak but the frozen day adds nothing.
        val active = setOf(nov(2), nov(3), nov(5), nov(6))
        val result = Streaks.compute(nov(6), active, monWedFri)
        assertEquals(4, result.status.currentDays)
        assertTrue(result.status.freezeUsedThisWeek)
        assertFalse(result.status.freezeAvailableThisWeek)
        assertEquals(setOf("2026-W45"), result.frozenWeeks)
    }

    @Test
    fun secondMissInSameWeekBreaksStreak() {
        // Mon 2nd active, Wed 4th missed (frozen), Fri 6th missed (breaks), Sat 7th active.
        val active = setOf(nov(2), nov(7))
        val result = Streaks.compute(nov(7), active, monWedFri)
        assertEquals(1, result.status.currentDays)
        assertEquals(3, result.status.longestDays)
    }

    @Test
    fun freezeResetsEachIsoWeek() {
        // Daily plan: Fri 6th (W45) and Tue 10th (W46) missed, both forgiven.
        val everyDay = (1..7).toSet()
        val active = setOf(nov(5), nov(7), nov(8), nov(9), nov(11))
        val result = Streaks.compute(nov(11), active, everyDay)
        assertEquals(5, result.status.currentDays)
        assertEquals(setOf("2026-W45", "2026-W46"), result.frozenWeeks)
    }

    @Test
    fun freezeIsNotConsumedWithoutRunningStreak() {
        val result = Streaks.compute(nov(6), setOf(nov(6)), monWedFri)
        assertEquals(1, result.status.currentDays)
        assertFalse(result.status.freezeUsedThisWeek)
    }

    @Test
    fun withoutPlanEveryDayNeedsActivity() {
        val active = setOf(nov(2), nov(3), nov(6))
        // Wed 4th frozen, Thu 5th breaks.
        val result = Streaks.compute(nov(6), active, emptySet())
        assertEquals(1, result.status.currentDays)
    }
}
