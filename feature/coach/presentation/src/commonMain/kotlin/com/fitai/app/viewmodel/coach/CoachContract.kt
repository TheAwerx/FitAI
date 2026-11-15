package com.fitai.app.viewmodel.coach

import com.fitai.app.coach.WeeklyReport
import com.fitai.app.model.CoachMessage

enum class CoachSendError { Failed, DailyLimit, TooLong }

data class CoachState(
    val reportLoading: Boolean = true,
    val report: WeeklyReport? = null,
    val reportError: Boolean = false,
    val chatLocked: Boolean = false,
    val accessResolved: Boolean = false,
    val messages: List<CoachMessage> = emptyList(),
    val input: String = "",
    val sending: Boolean = false,
    /** User message shown while waiting for the reply, kept after a failure for retry. */
    val pendingMessage: String? = null,
    val sendError: CoachSendError? = null,
)

sealed interface CoachIntent {
    data object Load : CoachIntent
    data object RetryReport : CoachIntent
    data class InputChanged(val value: String) : CoachIntent
    data object Send : CoachIntent
    data class SendPrompt(val text: String) : CoachIntent
    data object RetrySend : CoachIntent
    data object ClearHistory : CoachIntent
    data object Upgrade : CoachIntent
}

sealed interface CoachEffect {
    data object NavigateToPaywall : CoachEffect
    data object ScrollToLatest : CoachEffect
}
