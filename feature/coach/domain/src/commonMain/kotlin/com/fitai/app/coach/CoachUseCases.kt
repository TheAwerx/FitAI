package com.fitai.app.coach

import com.fitai.app.access.CheckFeatureAccessUseCase
import com.fitai.app.access.GatedFeature
import com.fitai.app.ai.AIRepository
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.CoachChatRequest
import com.fitai.app.model.CoachMessage
import com.fitai.app.model.WeeklyCoachReport
import com.fitai.app.model.WeeklyReportRequest
import com.fitai.app.model.toProfilePayload
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.stats.ComputeWeeklyStatsUseCase
import com.fitai.app.stats.WeeklyStats
import com.fitai.app.util.appLocale

class GetCoachHistoryUseCase(
    private val localUserRepository: LocalUserRepository,
    private val coachHistoryRepository: CoachHistoryRepository,
) {
    suspend operator fun invoke(): UseCaseState<List<CoachMessage>> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: return@useCaseCatching emptyList()
        coachHistoryRepository.loadHistory(user.id)
    }
}

class ClearCoachHistoryUseCase(
    private val localUserRepository: LocalUserRepository,
    private val coachHistoryRepository: CoachHistoryRepository,
) {
    suspend operator fun invoke(): UseCaseState<Unit> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: return@useCaseCatching
        coachHistoryRepository.clearHistory(user.id)
    }
}

/** Both messages are persisted only after the coach replies, so a failed send can be retried as-is. */
class SendCoachMessageUseCase(
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val buildCoachContextUseCase: BuildCoachContextUseCase,
    private val localUserRepository: LocalUserRepository,
    private val aiRepository: AIRepository,
    private val coachHistoryRepository: CoachHistoryRepository,
    private val trackEventUseCase: TrackEventUseCase,
) {
    suspend operator fun invoke(message: String): UseCaseState<CoachSendResult> = useCaseCatching {
        val text = message.trim()
        if (text.isEmpty()) return@useCaseCatching CoachSendResult.Empty
        if (text.length > CoachLimits.MAX_MESSAGE_CHARS) return@useCaseCatching CoachSendResult.TooLong

        val access = checkFeatureAccessUseCase(GatedFeature.CoachChat)
        if (access !is UseCaseState.Success || !access.data.allowed) return@useCaseCatching CoachSendResult.Locked

        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val stored = coachHistoryRepository.loadHistory(user.id)
        val context = (buildCoachContextUseCase() as? UseCaseState.Success)?.data.orEmpty()
        val reply = aiRepository.coachChat(
            CoachChatRequest(
                profile = user.toProfilePayload(),
                contextSummary = context,
                history = stored.takeLast(CoachLimits.MAX_HISTORY_SENT),
                message = text,
                locale = appLocale(),
            ),
        ).getOrElse { error ->
            if (error.isDailyLimit()) return@useCaseCatching CoachSendResult.DailyLimit
            throw error
        }.reply.trim()
        check(reply.isNotEmpty()) { "empty_reply" }

        val updated = (stored + CoachMessage(CoachMessage.ROLE_USER, text) + CoachMessage(CoachMessage.ROLE_COACH, reply))
            .takeLast(CoachLimits.MAX_HISTORY_STORED)
        coachHistoryRepository.saveHistory(user.id, updated)
        trackEventUseCase(
            AnalyticsEvents.COACH_MESSAGE_SENT,
            mapOf("length" to text.length.toString(), "history_size" to stored.size.toString()),
        )
        CoachSendResult.Replied(updated)
    }
}

/**
 * Local stats for everyone; the AI report only when [GatedFeature.WeeklyReportFull] is allowed.
 * The AI report is cached per ISO week and locale, so reopening never calls the AI twice.
 */
class GetWeeklyReportUseCase(
    private val computeWeeklyStatsUseCase: ComputeWeeklyStatsUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val localUserRepository: LocalUserRepository,
    private val aiRepository: AIRepository,
    private val weeklyReportCacheRepository: WeeklyReportCacheRepository,
    private val trackEventUseCase: TrackEventUseCase,
) {
    suspend operator fun invoke(weekOffset: Int = 0): UseCaseState<WeeklyReport> = useCaseCatching {
        val stats = when (val result = computeWeeklyStatsUseCase(weekOffset)) {
            is UseCaseState.Success -> result.data
            is UseCaseState.Error -> error(result.message)
        }
        val access = (checkFeatureAccessUseCase(GatedFeature.WeeklyReportFull) as? UseCaseState.Success)?.data
        val allowed = access?.allowed == true
        val coachReport = if (allowed) loadCoachReport(stats) else null

        trackEventUseCase(
            AnalyticsEvents.WEEKLY_REPORT_OPENED,
            mapOf(
                "week_key" to stats.weekKey,
                "tier" to (access?.tier?.name?.lowercase() ?: "unknown"),
                "full" to (coachReport != null).toString(),
            ),
        )
        WeeklyReport(
            stats = stats,
            headline = WeeklyHeadline.from(stats),
            coachReport = coachReport,
            locked = !allowed,
            coachReportFailed = allowed && coachReport == null,
        )
    }

    private suspend fun loadCoachReport(stats: WeeklyStats): WeeklyCoachReport? {
        val user = localUserRepository.getLatestUser() ?: return null
        val locale = appLocale()
        weeklyReportCacheRepository.getReport(user.id, stats.weekKey, locale)?.let { return it }
        val report = aiRepository.weeklyCoachReport(
            WeeklyReportRequest(profile = user.toProfilePayload(), stats = stats, locale = locale),
        ).getOrNull() ?: return null
        weeklyReportCacheRepository.saveReport(user.id, stats.weekKey, locale, report)
        return report
    }
}
