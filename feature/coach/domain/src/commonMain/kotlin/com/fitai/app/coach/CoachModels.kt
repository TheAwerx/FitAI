package com.fitai.app.coach

import com.fitai.app.model.CoachMessage
import com.fitai.app.model.WeeklyCoachReport
import com.fitai.app.stats.WeeklyStats
import kotlin.math.roundToInt

sealed interface CoachSendResult {
    /** [history] is the persisted conversation, oldest first, ending with the coach reply. */
    data class Replied(val history: List<CoachMessage>) : CoachSendResult
    data object Locked : CoachSendResult
    data object Empty : CoachSendResult
    data object TooLong : CoachSendResult
    data object DailyLimit : CoachSendResult
}

/** Locally computed one-liner; Free users see only this, Pro users see it when the AI report fails. */
sealed interface WeeklyHeadline {
    data object NoActivity : WeeklyHeadline
    data class PlanCompleted(val sessions: Int) : WeeklyHeadline
    data class VolumeUp(val percent: Int) : WeeklyHeadline
    data class ProteinLow(val avgProteinG: Int, val targetG: Int) : WeeklyHeadline
    data class InProgress(val done: Int, val planned: Int) : WeeklyHeadline

    companion object {
        fun from(stats: WeeklyStats): WeeklyHeadline {
            val volumeChange = stats.volumeChangePercent()
            return when {
                stats.sessionsCompleted == 0 && stats.mealsLogged == 0 -> NoActivity
                stats.sessionsPlanned > 0 && stats.sessionsCompleted >= stats.sessionsPlanned ->
                    PlanCompleted(stats.sessionsCompleted)
                volumeChange != null && volumeChange >= 5 -> VolumeUp(volumeChange)
                stats.proteinTargetG > 0 && stats.mealsLogged > 0 &&
                    stats.avgProteinG < stats.proteinTargetG * 0.85 ->
                    ProteinLow(stats.avgProteinG.roundToInt(), stats.proteinTargetG)
                else -> InProgress(stats.sessionsCompleted, stats.sessionsPlanned)
            }
        }
    }
}

data class WeeklyReport(
    val stats: WeeklyStats,
    val headline: WeeklyHeadline,
    /** Null for Free users ([locked]) or when the AI call failed ([coachReportFailed]). */
    val coachReport: WeeklyCoachReport?,
    val locked: Boolean,
    val coachReportFailed: Boolean = false,
)

/** Null when there is no previous-week volume to compare against. */
fun WeeklyStats.volumeChangePercent(): Int? =
    if (previousWeekVolumeKg <= 0.0) null
    else (((totalVolumeKg - previousWeekVolumeKg) / previousWeekVolumeKg) * 100).roundToInt()

internal fun Throwable.isDailyLimit(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        val text = (current.message.orEmpty() + " " + current.toString()).lowercase()
        if ("daily_limit" in text || "resource-exhausted" in text || "resource_exhausted" in text) return true
        current = current.cause?.takeIf { it !== current }
    }
    return false
}
