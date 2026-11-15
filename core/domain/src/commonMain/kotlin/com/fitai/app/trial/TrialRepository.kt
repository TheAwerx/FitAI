package com.fitai.app.trial

import com.fitai.app.model.TrialDebugInfo
import com.fitai.app.model.TrialStartResult
import com.fitai.app.model.TrialStatus

/**
 * Free trial (7 days, once per account and once per device). Prod asks Cloud Functions
 * (`startTrial` / `getTrialStatus`), dev uses a local fake with the same rules ([TrialPolicy]).
 */
interface TrialRepository {
    val isFirebaseAvailable: Boolean

    suspend fun ensureAnonymousAuth(): Result<String>

    /** Signed in with a real account (trials need one; the paywall runs on an anonymous session). */
    suspend fun hasAccount(): Boolean

    /**
     * The account's trial, or before sign-up whether this device can still start one. Uses a recent
     * cached answer unless [forceRefresh]; falls back to the cache when offline (see [TrialPolicy.resolve]).
     */
    suspend fun getTrialStatus(forceRefresh: Boolean = false): Result<TrialStatus>

    /** Server-verified. Fails with [TrialErrors.ACCOUNT_REQUIRED] without an account. */
    suspend fun startTrial(): Result<TrialStartResult>

    /** The paywall asked for the trial before sign-up; it starts once an account exists. */
    var trialRequested: Boolean

    suspend fun canAccessAiFeatures(): Result<Boolean>

    /** Null outside debug builds. */
    suspend fun debugInfo(): TrialDebugInfo?
}
