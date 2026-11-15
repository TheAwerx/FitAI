package com.fitai.app.coach

import com.fitai.app.model.WeeklyCoachReport
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class WeeklyReportCacheRepositoryImpl(
    private val settings: Settings,
    private val json: Json = CoachJson,
) : WeeklyReportCacheRepository {

    private val serializer = ListSerializer(CachedWeeklyReport.serializer())

    override suspend fun getReport(userId: String, weekKey: String, locale: String): WeeklyCoachReport? =
        read(userId).firstOrNull { it.weekKey == weekKey && it.locale == locale }?.report

    override suspend fun saveReport(userId: String, weekKey: String, locale: String, report: WeeklyCoachReport) {
        val entries = read(userId).filterNot { it.weekKey == weekKey && it.locale == locale } +
            CachedWeeklyReport(weekKey, locale, report)
        settings[key(userId)] = json.encodeToString(serializer, entries.takeLast(MAX_ENTRIES))
    }

    private fun read(userId: String): List<CachedWeeklyReport> {
        val raw = settings.getStringOrNull(key(userId)) ?: return emptyList()
        return runCatching { json.decodeFromString(serializer, raw) }.getOrElse {
            settings.remove(key(userId))
            emptyList()
        }
    }

    private fun key(userId: String) = "$KEY_PREFIX$userId"

    private companion object {
        const val KEY_PREFIX = "coach_weekly_report_"
        const val MAX_ENTRIES = 8
    }
}

@Serializable
private data class CachedWeeklyReport(
    val weekKey: String,
    val locale: String,
    val report: WeeklyCoachReport,
)
