package com.fitai.app.viewmodel.plansetup

import com.fitai.app.notifications.ReminderTexts

/** Why the free trial asked for on the paywall didn't start after sign-up. */
enum class PlanSetupTrialNotice { UsedOnDevice, UsedByAccount, Pending }

data class PlanSetupState(
    /** True when the account is Pro or in its trial and the plan is built by AI. */
    val personal: Boolean = false,
    val trialNotice: PlanSetupTrialNotice? = null,
    val progress: Float = 0f,
    val failed: Boolean = false,
    val running: Boolean = false,
)

sealed interface PlanSetupIntent {
    data class Start(val texts: ReminderTexts) : PlanSetupIntent
    data object Retry : PlanSetupIntent
    data object UseStarterPlan : PlanSetupIntent
}

sealed interface PlanSetupEffect {
    data object Done : PlanSetupEffect
}
