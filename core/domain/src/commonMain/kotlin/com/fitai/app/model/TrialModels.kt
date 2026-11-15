package com.fitai.app.model

import com.fitai.app.trial.TrialPolicy

/** Trial answer from the server (`getTrialStatus` / `startTrial` in firebase/functions/src/trial.ts). */
enum class TrialState(val code: String) {
    /** Neither this account nor this device has used the free trial. */
    Eligible("eligible"),
    Active("active"),
    Expired("expired"),
    /** This device already started a trial for another account. */
    UsedOnDevice("used"),
    ;

    companion object {
        fun fromCode(code: String?): TrialState? = entries.firstOrNull { it.code == code }
    }
}

data class TrialStatus(
    val state: TrialState,
    val trialDays: Int = TrialPolicy.TRIAL_DAYS,
    /** ISO-8601 instants; null while no trial was started. */
    val trialStartedAt: String? = null,
    val trialExpiresAt: String? = null,
    val daysRemaining: Int = 0,
    val hasSubscription: Boolean = false,
) {
    val isActive: Boolean get() = state == TrialState.Active

    /** The paywall may offer "start the free trial". */
    val canStart: Boolean get() = state == TrialState.Eligible
}

/** Result of `startTrial`; [started] is false when an earlier trial came back unchanged or the start was refused. */
data class TrialStartResult(
    val started: Boolean,
    val status: TrialStatus,
)

/** Debug builds only: lets a reinstall test check that the device key survived (Settings screen). */
data class TrialDebugInfo(
    val state: TrialState?,
    val daysRemaining: Int,
    /** First characters of the hashed device key; the full key is never shown. */
    val deviceKeyPrefix: String,
    /** Where the key came from, e.g. `android_id`, `keychain`. */
    val keySource: String,
    /** `server` (Cloud Functions) or `local` (dev fake). */
    val backend: String,
)
