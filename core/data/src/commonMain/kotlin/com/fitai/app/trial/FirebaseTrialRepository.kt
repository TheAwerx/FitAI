package com.fitai.app.trial

import com.fitai.app.firebase.FirebaseBootstrap
import com.fitai.app.firebase.FitAiFunctions
import com.fitai.app.model.TrialDebugInfo
import com.fitai.app.model.TrialStartResult
import com.fitai.app.model.TrialStatus
import com.fitai.app.util.epochMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** Firebase Auth helpers shared by both trial repositories. */
internal object TrialAuth {
    val available: Boolean get() = FirebaseBootstrap.ensureInitialized()

    suspend fun ensureAnonymousAuth(): String {
        check(available) { "Firebase is not configured" }
        val auth = Firebase.auth
        if (auth.currentUser == null) auth.signInAnonymously()
        return auth.currentUser?.uid ?: error("Anonymous auth failed")
    }

    /** Uid of a real (non-anonymous) account, or null. */
    fun accountUid(): String? =
        if (available) Firebase.auth.currentUser?.takeUnless { it.isAnonymous }?.uid else null
}

/**
 * Prod: `getTrialStatus` / `startTrial` callables decide with server time (firebase/functions/src/trial.ts).
 * The last answer is cached ([TrialStatusCache]) and reused for [TrialPolicy.REFRESH_INTERVAL_MILLIS];
 * offline, an active trial stays valid for [TrialPolicy.OFFLINE_GRACE_MILLIS]. Starting always needs the server.
 */
class FirebaseTrialRepository(
    private val cache: TrialStatusCache,
    private val deviceKeyProvider: DeviceKeyProvider,
    private val clock: () -> Long = ::epochMillis,
) : TrialRepository {

    /** Home asks for the tier and the banner at once; the second caller reuses the first answer. */
    private val statusMutex = Mutex()

    override val isFirebaseAvailable: Boolean get() = TrialAuth.available

    override suspend fun ensureAnonymousAuth(): Result<String> = runCatching { TrialAuth.ensureAnonymousAuth() }

    override suspend fun hasAccount(): Boolean = TrialAuth.accountUid() != null

    override var trialRequested: Boolean
        get() = cache.trialRequested
        set(value) { cache.trialRequested = value }

    override suspend fun getTrialStatus(forceRefresh: Boolean): Result<TrialStatus> = statusMutex.withLock {
        loadStatus(forceRefresh)
    }

    private suspend fun loadStatus(forceRefresh: Boolean): Result<TrialStatus> = runCatching {
        check(TrialAuth.available) { "Firebase is not configured" }
        val accountId = TrialAuth.accountUid()
        val cached = cache.read()?.takeIf { it.accountId == accountId }
        if (!forceRefresh && cached != null && TrialPolicy.isFresh(cached, clock())) {
            TrialPolicy.resolve(cached, clock())?.let { return@runCatching it }
        }
        val fetched = runCatching { fetchStatus(accountId) }
            .onFailure { Napier.w("trial status fetch failed; using cached answer", it) }
            .getOrNull()
        fetched ?: cached?.let { TrialPolicy.resolve(it, clock()) } ?: error(TrialErrors.STATUS_UNAVAILABLE)
    }

    override suspend fun startTrial(): Result<TrialStartResult> = runCatching {
        check(TrialAuth.available) { "Firebase is not configured" }
        val accountId = TrialAuth.accountUid() ?: error(TrialErrors.ACCOUNT_REQUIRED)
        val key = deviceKeyProvider.deviceKey()
        val dto = withTimeout(CALL_TIMEOUT) {
            FitAiFunctions.startTrial(TrialDeviceRequest(key.value, key.platform.code))
        }
        TrialStartResult(started = dto.started, status = store(dto, accountId))
    }

    override suspend fun canAccessAiFeatures(): Result<Boolean> = runCatching {
        // Signed-in accounts reach the AI callables; per-feature limits are `GatedFeature` quotas.
        hasAccount() || getTrialStatus().getOrNull()?.isActive == true
    }

    override suspend fun debugInfo(): TrialDebugInfo? {
        if (!deviceKeyProvider.showsDiagnostics) return null
        val key = deviceKeyProvider.deviceKey()
        val status = getTrialStatus().getOrNull()
        return TrialDebugInfo(
            state = status?.state,
            daysRemaining = status?.daysRemaining ?: 0,
            deviceKeyPrefix = key.value.take(DEBUG_PREFIX_LENGTH),
            keySource = key.source.code,
            backend = "server",
        )
    }

    private suspend fun fetchStatus(accountId: String?): TrialStatus {
        if (!TrialAuth.available) error("Firebase is not configured")
        TrialAuth.ensureAnonymousAuth()
        val key = deviceKeyProvider.deviceKey()
        val dto = withTimeout(CALL_TIMEOUT) {
            FitAiFunctions.getTrialStatus(TrialDeviceRequest(key.value, key.platform.code))
        }
        return store(dto, accountId)
    }

    private fun store(dto: TrialStatusDto, accountId: String?): TrialStatus {
        val now = clock()
        val snapshot = dto.toSnapshot(accountId, now)
        cache.write(snapshot)
        return TrialPolicy.resolve(snapshot, now) ?: TrialPolicy.noTrial(snapshot.state)
    }

    private companion object {
        val CALL_TIMEOUT = 15.seconds
        const val DEBUG_PREFIX_LENGTH = 8
    }
}
