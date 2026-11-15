package com.fitai.app.notifications

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Local wall-clock math shared by the planner and both platform schedulers. */
@OptIn(ExperimentalTime::class)
object ReminderTimes {
    private const val DEFAULT_HORIZON_DAYS = 60

    fun localDateOf(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date

    fun localDateTimeOf(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
        Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)

    fun epochMillisAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long = LocalDateTime(date, LocalTime(hour, minute)).toInstant(timeZone).toEpochMilliseconds()

    /**
     * Fire times strictly after [nowMillis], oldest first. [days] null means every day.
     * Dates listed in [skipDates] are left out.
     */
    fun occurrences(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        days: Set<Int>?,
        skipDates: Set<String>,
        limit: Int,
        horizonDays: Int = DEFAULT_HORIZON_DAYS,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<Long> {
        if (days != null && days.isEmpty()) return emptyList()
        val today = localDateOf(nowMillis, timeZone)
        val result = mutableListOf<Long>()
        for (offset in 0..horizonDays) {
            if (result.size >= limit) break
            val date = today.plus(DatePeriod(days = offset))
            if (days != null && date.dayOfWeek.isoDayNumber !in days) continue
            if (date.toString() in skipDates) continue
            val at = epochMillisAt(date, hour, minute, timeZone)
            if (at > nowMillis) result += at
        }
        return result
    }

    fun nextOccurrence(
        nowMillis: Long,
        hour: Int,
        minute: Int,
        days: Set<Int>?,
        skipDates: Set<String> = emptySet(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long? = occurrences(nowMillis, hour, minute, days, skipDates, limit = 1, timeZone = timeZone).firstOrNull()

    /** True while a skip date is today or later, i.e. a plain repeating trigger would still double up. */
    fun hasUpcomingSkip(
        nowMillis: Long,
        skipDates: Set<String>,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Boolean {
        val today = localDateOf(nowMillis, timeZone)
        return skipDates.any { iso -> runCatching { LocalDate.parse(iso) >= today }.getOrDefault(false) }
    }

    fun sequenceFireAt(
        anchorEpochMillis: Long,
        item: SequenceReminder,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long {
        val date = localDateOf(anchorEpochMillis, timeZone).plus(DatePeriod(days = item.dayOffset))
        return epochMillisAt(date, item.hour, item.minute, timeZone)
    }
}
