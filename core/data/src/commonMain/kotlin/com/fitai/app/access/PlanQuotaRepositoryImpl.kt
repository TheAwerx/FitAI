package com.fitai.app.access

import com.fitai.app.util.epochMillis
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.firestore
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Regeneration count kept in app settings. It is the only count in dev (no Cloud Functions) and the
 * offline copy of the server count in prod. Sign-out clears it with the other settings.
 */
class LocalPlanQuotaRepository(
    private val settings: Settings,
    private val clock: () -> Long = ::epochMillis,
) : PlanQuotaRepository {

    override suspend fun getQuota(): PlanQuota = current()

    override suspend fun recordRegeneration(): PlanQuota {
        val quota = current()
        val periodStart = if (quota.resetsAtEpochMillis != null) storedPeriodStart() else clock()
        store(used = quota.used + 1, periodStartMillis = periodStart)
        return current()
    }

    override suspend fun markExhausted(): PlanQuota {
        val quota = current()
        store(used = quota.limit, periodStartMillis = storedPeriodStart() ?: clock())
        return current()
    }

    /** Copies the server document so the count survives going offline. */
    fun cache(limit: Int, used: Int, periodStartMillis: Long?, resetPeriodDays: Int?) {
        settings[KEY_LIMIT] = limit
        if (resetPeriodDays == null) settings.remove(KEY_RESET_DAYS) else settings[KEY_RESET_DAYS] = resetPeriodDays
        store(used, periodStartMillis)
    }

    private fun current(): PlanQuota = planQuotaOf(
        limit = settings.getInt(KEY_LIMIT, PlanQuotaPolicy.LIMIT),
        used = settings.getInt(KEY_USED, 0),
        periodStartMillis = storedPeriodStart(),
        resetPeriodDays = settings.getIntOrNull(KEY_RESET_DAYS) ?: PlanQuotaPolicy.RESET_PERIOD_DAYS,
        nowMillis = clock(),
    )

    private fun storedPeriodStart(): Long? = settings.getLongOrNull(KEY_PERIOD_START)

    private fun store(used: Int, periodStartMillis: Long?) {
        settings[KEY_USED] = used.coerceAtLeast(0)
        if (periodStartMillis == null) settings.remove(KEY_PERIOD_START) else settings[KEY_PERIOD_START] = periodStartMillis
    }

    private companion object {
        const val KEY_USED = "plan_quota_used"
        const val KEY_LIMIT = "plan_quota_limit"
        const val KEY_RESET_DAYS = "plan_quota_reset_days"
        const val KEY_PERIOD_START = "plan_quota_period_start"
    }
}

/**
 * Prod: reads `plan_quotas/{uid}`, which only the `generateWorkoutPlan` callable writes (Admin SDK).
 * The server counts a regeneration once it succeeds, so reinstalling the app does not give rights back.
 * Falls back to [local] when signed out, offline, the read is slow, or the server has no document yet
 * (nothing counted so far, or a callable deployed before the quota existed).
 */
class FirestorePlanQuotaRepository(
    private val local: LocalPlanQuotaRepository,
) : PlanQuotaRepository {

    override suspend fun getQuota(): PlanQuota = fetchRemote() ?: local.getQuota()

    override suspend fun recordRegeneration(): PlanQuota = fetchRemote() ?: local.recordRegeneration()

    override suspend fun markExhausted(): PlanQuota =
        fetchRemote()?.takeIf { it.exhausted } ?: local.markExhausted()

    private suspend fun fetchRemote(): PlanQuota? = runCatching {
        val uid = Firebase.auth.currentUser?.uid ?: return@runCatching null
        withTimeoutOrNull(REMOTE_TIMEOUT) {
            val doc = Firebase.firestore.collection(COLLECTION).document(uid).get()
            if (!doc.exists) return@withTimeoutOrNull null
            local.cache(
                limit = doc.intOrNull(FIELD_LIMIT) ?: PlanQuotaPolicy.LIMIT,
                used = doc.intOrNull(FIELD_USED) ?: 0,
                periodStartMillis = doc.longOrNull(FIELD_PERIOD_START),
                resetPeriodDays = if (doc.contains(FIELD_RESET_DAYS)) doc.intOrNull(FIELD_RESET_DAYS) else PlanQuotaPolicy.RESET_PERIOD_DAYS,
            )
            local.getQuota()
        }
    }.onFailure { Napier.w("plan quota read failed; using local copy", it) }.getOrNull()

    private fun DocumentSnapshot.longOrNull(field: String): Long? =
        runCatching { get<Long?>(field) }.getOrNull()
            ?: runCatching { get<Double?>(field)?.toLong() }.getOrNull()

    private fun DocumentSnapshot.intOrNull(field: String): Int? = longOrNull(field)?.toInt()

    private companion object {
        const val COLLECTION = "plan_quotas"
        const val FIELD_USED = "used"
        const val FIELD_LIMIT = "limit"
        const val FIELD_RESET_DAYS = "resetPeriodDays"
        const val FIELD_PERIOD_START = "periodStartedAtMs"
        val REMOTE_TIMEOUT = 6.seconds
    }
}
