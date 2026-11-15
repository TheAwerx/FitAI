package com.fitai.app.engagement

import com.fitai.app.util.isoWeekKey
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Plan-aware streak. A day is active when it has a completed session or a logged meal.
 * - Active day: streak +1.
 * - Planned rest day without activity: keeps the streak and counts (+1) once a streak has started.
 * - Missed planned training day: uses the ISO week's single freeze (no +1) if still unused,
 *   otherwise the streak resets. A freeze is only consumed while a streak is running.
 * - Today never breaks the streak; an inactive training day today simply adds nothing yet.
 */
object Streaks {

    const val LOOKBACK_DAYS = 120

    data class Result(
        val status: StreakStatus,
        /** ISO week keys whose freeze was consumed inside the window. */
        val frozenWeeks: Set<String>,
        /** First date the running streak reached each requested milestone length. */
        val milestoneDates: Map<Int, LocalDate>,
    )

    /** [trainingDaysOfWeek] uses ISO numbers (1 = Monday); empty means every day needs activity. */
    fun compute(
        today: LocalDate,
        activeDates: Set<LocalDate>,
        trainingDaysOfWeek: Set<Int>,
        milestones: Collection<Int> = emptyList(),
    ): Result {
        val frozenWeeks = mutableSetOf<String>()
        val milestoneDates = mutableMapOf<Int, LocalDate>()
        var run = 0
        var longest = 0
        var day = today.minus(DatePeriod(days = LOOKBACK_DAYS - 1))
        while (day <= today) {
            val required = trainingDaysOfWeek.isEmpty() || day.dayOfWeek.isoDayNumber in trainingDaysOfWeek
            when {
                day in activeDates -> run++
                !required -> if (run > 0) run++
                day == today || run == 0 -> Unit
                frozenWeeks.add(isoWeekKey(day)) -> Unit
                else -> run = 0
            }
            if (run > longest) longest = run
            milestones.forEach { length -> if (run >= length && length !in milestoneDates) milestoneDates[length] = day }
            day = day.plus(DatePeriod(days = 1))
        }
        val freezeUsedThisWeek = isoWeekKey(today) in frozenWeeks
        return Result(
            status = StreakStatus(
                currentDays = run,
                longestDays = longest.takeIf { it > 0 },
                todayActive = today in activeDates,
                freezeAvailableThisWeek = !freezeUsedThisWeek,
                freezeUsedThisWeek = freezeUsedThisWeek,
            ),
            frozenWeeks = frozenWeeks,
            milestoneDates = milestoneDates,
        )
    }
}
