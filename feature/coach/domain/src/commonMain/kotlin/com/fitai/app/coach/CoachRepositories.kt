package com.fitai.app.coach

import com.fitai.app.model.CoachMessage
import com.fitai.app.model.WeeklyCoachReport

interface CoachHistoryRepository {
    /** Oldest first. */
    suspend fun loadHistory(userId: String): List<CoachMessage>
    /** Keeps only the newest [CoachLimits.MAX_HISTORY_STORED] messages. */
    suspend fun saveHistory(userId: String, messages: List<CoachMessage>)
    suspend fun clearHistory(userId: String)
}

interface WeeklyReportCacheRepository {
    suspend fun getReport(userId: String, weekKey: String, locale: String): WeeklyCoachReport?
    suspend fun saveReport(userId: String, weekKey: String, locale: String, report: WeeklyCoachReport)
}

object CoachLimits {
    /** Server rejects longer messages. */
    const val MAX_MESSAGE_CHARS = 1000
    /** Server keeps only this many history items. */
    const val MAX_HISTORY_SENT = 20
    const val MAX_HISTORY_STORED = 30
    const val MAX_CONTEXT_CHARS = 1200
    const val CONTEXT_DAYS = 14
}
