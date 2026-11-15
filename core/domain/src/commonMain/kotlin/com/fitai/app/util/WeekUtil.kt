package com.fitai.app.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Monday of the ISO week containing [date]. */
fun weekStart(date: LocalDate): LocalDate =
    date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))

/** ISO-8601 week key such as "2026-W45". */
fun isoWeekKey(date: LocalDate): String {
    val thursday = weekStart(date).plus(DatePeriod(days = 3))
    val week = (thursday.dayOfYear - 1) / 7 + 1
    return "${thursday.year}-W${week.toString().padStart(2, '0')}"
}

/** [weekOffset] 0 = this week, -1 = last week. */
fun currentIsoWeekKey(weekOffset: Int = 0): String =
    isoWeekKey(todayLocalDate().plus(DatePeriod(days = 7 * weekOffset)))

/** Monday 00:00 (inclusive) to next Monday 00:00 (exclusive) as UTC ISO timestamps in the device time zone. */
fun weekRangeIso(weekOffset: Int = 0): Pair<String, String> {
    val zone = TimeZone.currentSystemDefault()
    val monday = weekStart(todayLocalDate()).plus(DatePeriod(days = 7 * weekOffset))
    val nextMonday = monday.plus(DatePeriod(days = 7))
    return monday.atStartOfDayIn(zone).toString() to nextMonday.atStartOfDayIn(zone).toString()
}

/** Local calendar date of an ISO instant string, or null when it cannot be parsed. */
@OptIn(ExperimentalTime::class)
fun isoTimestampToLocalDate(iso: String): LocalDate? =
    runCatching { Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault()).date }.getOrNull()

fun currentWeekStartEpochMillis(): Long =
    weekStart(todayLocalDate()).atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

fun currentMonthStartEpochMillis(): Long {
    val today = todayLocalDate()
    return LocalDate(today.year, today.month, 1)
        .atStartOfDayIn(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()
}
