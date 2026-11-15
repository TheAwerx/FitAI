package com.fitai.app.trial

import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Error messages shared with `TRIAL_ERRORS` in firebase/functions/src/trial.ts. */
object TrialErrors {
    const val ACCOUNT_REQUIRED = "trial_requires_account"
    const val INVALID_DEVICE = "trial_invalid_device"
    const val NOT_CONFIGURED = "trial_not_configured"
    const val STATUS_UNAVAILABLE = "trial_status_unavailable"
}

data class TrialPeriod(
    val startedAtMillis: Long,
    val endsAtMillis: Long,
)

/** The device a trial was started on, and for which account (`device_trials/{hash}`). */
data class DeviceTrialClaim(
    val firstAccountId: String,
    val period: TrialPeriod,
)

sealed interface TrialStartDecision {
    /** The account already had a trial; it comes back unchanged, never restarted. */
    data class Existing(val period: TrialPeriod) : TrialStartDecision
    /** Another account already started a trial on this device. */
    data object DeviceUsed : TrialStartDecision
    data class Start(val period: TrialPeriod) : TrialStartDecision
}

/** Last server answer, kept in app settings so the trial survives short offline periods. */
data class TrialSnapshot(
    /** Signed-in uid the answer belongs to; null for the anonymous session before sign-up. */
    val accountId: String?,
    val state: TrialState,
    val startedAtMillis: Long?,
    val endsAtMillis: Long?,
    val serverTimeMillis: Long,
    /** Device clock when the answer arrived. */
    val fetchedAtMillis: Long,
)

/**
 * One free trial per account and per device. Mirror of `firebase/functions/src/trial.ts`: the server
 * decides in prod, the dev fake (`LocalTrialRepository`) applies the same rules locally.
 */
object TrialPolicy {
    const val TRIAL_DAYS = 7
    const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    /** A cached answer younger than this is used without asking the server again. */
    const val REFRESH_INTERVAL_MILLIS = 10 * 60 * 1000L

    /** An active trial stays usable offline this long after the last server answer. */
    const val OFFLINE_GRACE_MILLIS = 3 * DAY_MILLIS

    /** Backward clock corrections up to this much (NTP) don't invalidate the cache. */
    const val CLOCK_SKEW_MILLIS = 5 * 60 * 1000L

    fun daysRemaining(endsAtMillis: Long, nowMillis: Long): Int {
        val left = endsAtMillis - nowMillis
        return if (left <= 0) 0 else ((left + DAY_MILLIS - 1) / DAY_MILLIS).toInt()
    }

    fun statusOf(period: TrialPeriod, nowMillis: Long): TrialStatus {
        val active = period.endsAtMillis > nowMillis
        return TrialStatus(
            state = if (active) TrialState.Active else TrialState.Expired,
            trialStartedAt = isoOf(period.startedAtMillis),
            trialExpiresAt = isoOf(period.endsAtMillis),
            daysRemaining = if (active) daysRemaining(period.endsAtMillis, nowMillis) else 0,
        )
    }

    fun noTrial(state: TrialState): TrialStatus = TrialStatus(state = state)

    /**
     * What the server answers to `getTrialStatus`: the account's trial if it has one, otherwise
     * whether this device can still start one. [accountId] is null before sign-up.
     */
    fun statusFor(
        accountId: String?,
        accountTrial: TrialPeriod?,
        deviceClaim: DeviceTrialClaim?,
        nowMillis: Long,
    ): TrialStatus = when {
        accountId != null && accountTrial != null -> statusOf(accountTrial, nowMillis)
        deviceClaim == null -> noTrial(TrialState.Eligible)
        accountId != null && deviceClaim.firstAccountId == accountId -> statusOf(deviceClaim.period, nowMillis)
        else -> noTrial(TrialState.UsedOnDevice)
    }

    /** What the server does on `startTrial` for a signed-in account. */
    fun decideStart(
        accountId: String,
        accountTrial: TrialPeriod?,
        deviceClaim: DeviceTrialClaim?,
        nowMillis: Long,
    ): TrialStartDecision = when {
        accountTrial != null -> TrialStartDecision.Existing(accountTrial)
        deviceClaim == null -> TrialStartDecision.Start(TrialPeriod(nowMillis, nowMillis + TRIAL_DAYS * DAY_MILLIS))
        deviceClaim.firstAccountId == accountId -> TrialStartDecision.Existing(deviceClaim.period)
        else -> TrialStartDecision.DeviceUsed
    }

    /** The cached answer is recent enough to skip the server. */
    fun isFresh(snapshot: TrialSnapshot, deviceNowMillis: Long): Boolean {
        val elapsed = deviceNowMillis - snapshot.fetchedAtMillis
        return elapsed >= -CLOCK_SKEW_MILLIS && elapsed < REFRESH_INTERVAL_MILLIS
    }

    /**
     * Status from a cached server answer, advanced by the time passed on this device. Null when it
     * can't be trusted: the device clock moved backwards, or an active trial wasn't re-checked within
     * [OFFLINE_GRACE_MILLIS]. Offline time never turns a finished or refused trial into an active one.
     */
    fun resolve(snapshot: TrialSnapshot, deviceNowMillis: Long): TrialStatus? {
        val elapsed = deviceNowMillis - snapshot.fetchedAtMillis
        if (elapsed < -CLOCK_SKEW_MILLIS) return null
        val serverNow = snapshot.serverTimeMillis + elapsed.coerceAtLeast(0)
        val started = snapshot.startedAtMillis
        val ends = snapshot.endsAtMillis
        return when (snapshot.state) {
            TrialState.Active, TrialState.Expired -> {
                if (started == null || ends == null) return null
                val status = statusOf(TrialPeriod(started, ends), serverNow)
                if (status.isActive && elapsed > OFFLINE_GRACE_MILLIS) null else status
            }
            TrialState.Eligible, TrialState.UsedOnDevice -> noTrial(snapshot.state)
        }
    }

    @OptIn(ExperimentalTime::class)
    fun parseIsoMillis(value: String?): Long? =
        value?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }

    @OptIn(ExperimentalTime::class)
    private fun isoOf(millis: Long): String = Instant.fromEpochMilliseconds(millis).toString()
}
