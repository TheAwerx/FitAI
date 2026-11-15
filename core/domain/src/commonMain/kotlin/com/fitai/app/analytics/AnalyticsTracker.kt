package com.fitai.app.analytics

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching

interface AnalyticsTracker {
    fun track(event: String, params: Map<String, String> = emptyMap())
}

object AnalyticsEvents {
    const val ONBOARDING_STEP_VIEWED = "onboarding_step_viewed"
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    const val ATTRIBUTION_SELECTED = "attribution_selected"
    const val COMMITMENT_CONFIRMED = "commitment_confirmed"
    const val PAYWALL_VIEWED = "paywall_viewed"
    const val PURCHASE_STARTED = "purchase_started"
    const val PURCHASE_COMPLETED = "purchase_completed"
    const val TRIAL_STARTED = "trial_started"
    const val WORKOUT_STARTED = "workout_started"
    const val WORKOUT_COMPLETED = "workout_completed"
    const val SET_COMPLETED = "set_completed"
    const val MEAL_PHOTO_ANALYZED = "meal_photo_analyzed"
    const val NEXT_MEAL_SUGGESTED = "next_meal_suggested"
    const val TODAY_MODE_USED = "today_mode_used"
    const val COACH_MESSAGE_SENT = "coach_message_sent"
    const val WEEKLY_REPORT_OPENED = "weekly_report_opened"
    const val NOTIFICATION_PERMISSION_RESULT = "notification_permission_result"
    const val ONBOARDING_STATEMENT_ANSWERED = "onboarding_statement_answered"
    const val PAYWALL_EXIT_INTENT = "paywall_exit_intent"
    const val PAYWALL_DECLINED = "paywall_declined"
    const val SIGNUP_COMPLETED = "signup_completed"
    const val PLAN_ASSIGNED = "plan_assigned"
    const val STARTER_CHALLENGE_OPENED = "starter_challenge_opened"
}

class TrackEventUseCase(
    private val analyticsTracker: AnalyticsTracker,
) {
    operator fun invoke(event: String, params: Map<String, String> = emptyMap()): UseCaseState<Unit> =
        useCaseCatching { analyticsTracker.track(event, params) }
}
