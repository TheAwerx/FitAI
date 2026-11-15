package com.fitai.app.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

fun todayLocalDate(): LocalDate =
    InstantFromEpoch(epochMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun InstantFromEpoch(ms: Long) = kotlinx.datetime.Instant.fromEpochMilliseconds(ms)

fun todayDateRangeIso(): Pair<String, String> {
    val today = todayLocalDate()
    val start = today.atStartOfDayIn(TimeZone.currentSystemDefault()).toString()
    val end = today.plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.currentSystemDefault()).toString()
    return start to end
}

fun currentDayOfWeek(): Int {
    val day = todayLocalDate().dayOfWeek
    return when (day) {
        kotlinx.datetime.DayOfWeek.MONDAY -> 1
        kotlinx.datetime.DayOfWeek.TUESDAY -> 2
        kotlinx.datetime.DayOfWeek.WEDNESDAY -> 3
        kotlinx.datetime.DayOfWeek.THURSDAY -> 4
        kotlinx.datetime.DayOfWeek.FRIDAY -> 5
        kotlinx.datetime.DayOfWeek.SATURDAY -> 6
        kotlinx.datetime.DayOfWeek.SUNDAY -> 7
    }
}

fun formatDayName(dayOfWeek: Int): String = when (dayOfWeek) {
    1 -> "Pazartesi"
    2 -> "Salı"
    3 -> "Çarşamba"
    4 -> "Perşembe"
    5 -> "Cuma"
    6 -> "Cumartesi"
    7 -> "Pazar"
    else -> "Gün $dayOfWeek"
}
