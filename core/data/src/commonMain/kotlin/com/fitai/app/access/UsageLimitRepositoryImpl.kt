package com.fitai.app.access

import com.fitai.app.util.currentMonthStartEpochMillis
import com.fitai.app.util.currentWeekStartEpochMillis
import com.fitai.app.util.epochMillis
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/** Stores use timestamps (epoch millis, comma separated) per feature; weeks start on Monday. */
class UsageLimitRepositoryImpl(
    private val settings: Settings,
) : UsageLimitRepository {

    override suspend fun usedInCurrentPeriod(feature: GatedFeature): Int {
        val periodStart = periodStartMillis(feature.period)
        return timestamps(feature).count { it >= periodStart }
    }

    override suspend fun recordUse(feature: GatedFeature) {
        val oldestRelevant = minOf(currentWeekStartEpochMillis(), currentMonthStartEpochMillis())
        val kept = timestamps(feature).filter { it >= oldestRelevant } + epochMillis()
        settings[key(feature)] = kept.joinToString(",")
    }

    private fun timestamps(feature: GatedFeature): List<Long> =
        settings.getString(key(feature), "")
            .split(",")
            .mapNotNull { it.toLongOrNull() }

    private fun periodStartMillis(period: QuotaPeriod): Long = when (period) {
        QuotaPeriod.Week -> currentWeekStartEpochMillis()
        QuotaPeriod.Month -> currentMonthStartEpochMillis()
    }

    private fun key(feature: GatedFeature) = "usage_${feature.name}"
}
