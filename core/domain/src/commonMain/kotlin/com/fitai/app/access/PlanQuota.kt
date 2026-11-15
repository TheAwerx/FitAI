package com.fitai.app.access

/**
 * Workout plan regeneration rights ("Planı yenile"). The plan assigned in plan setup is free;
 * each successful regeneration from the Workout tab or Settings spends one right.
 *
 * Mirror of `PLAN_QUOTA` in `firebase/functions/src/planQuota.ts`. In prod the server owns the
 * count and the limit it stores in `plan_quotas/{uid}` wins over these values.
 */
object PlanQuotaPolicy {
    const val LIMIT = 3

    /** Days until the used count starts over; null means rights never come back. */
    val RESET_PERIOD_DAYS: Int? = null

    internal const val DAY_MILLIS = 86_400_000L
}

/** Why a workout plan is being generated. Sent to `generateWorkoutPlan` as `planPurpose`. */
enum class PlanGenerationPurpose(val code: String) {
    /** First plan for the account (plan setup, or the Workout tab when there is no plan). Free. */
    Initial("initial"),

    /** "Planı yenile": spends one right. */
    Regenerate("regenerate"),
}

data class PlanQuota(
    val limit: Int,
    val used: Int,
    /** Epoch millis when [used] starts over; null when it never does. */
    val resetsAtEpochMillis: Long? = null,
) {
    val remaining: Int get() = (limit - used).coerceAtLeast(0)
    val exhausted: Boolean get() = remaining == 0
}

/** Error codes shared by the client and the `generateWorkoutPlan` callable. */
object PlanQuotaErrors {
    const val EXHAUSTED = "plan_quota_exhausted"
    const val IN_PROGRESS = "plan_generation_in_progress"
    const val INITIAL_LIMIT = "plan_initial_limit"

    val all = listOf(EXHAUSTED, IN_PROGRESS, INITIAL_LIMIT)
}

interface PlanQuotaRepository {
    /** Prod: the server count when reachable, otherwise the local copy. Dev: the local count. */
    suspend fun getQuota(): PlanQuota

    /** Called after a successful regeneration. Prod: the server already counted it; this refreshes the copy. */
    suspend fun recordRegeneration(): PlanQuota

    /** The server said no rights are left; align the local copy with it. */
    suspend fun markExhausted(): PlanQuota
}

/**
 * Builds a [PlanQuota] from a stored count. With a reset period, a count whose period
 * ([periodStartMillis] + [resetPeriodDays]) has passed reads as zero.
 */
fun planQuotaOf(
    limit: Int,
    used: Int,
    periodStartMillis: Long?,
    resetPeriodDays: Int?,
    nowMillis: Long,
): PlanQuota {
    if (resetPeriodDays == null || periodStartMillis == null) return PlanQuota(limit, used.coerceAtLeast(0))
    val resetsAt = periodStartMillis + resetPeriodDays * PlanQuotaPolicy.DAY_MILLIS
    return if (nowMillis >= resetsAt) {
        PlanQuota(limit, used = 0)
    } else {
        PlanQuota(limit, used.coerceAtLeast(0), resetsAtEpochMillis = resetsAt)
    }
}
