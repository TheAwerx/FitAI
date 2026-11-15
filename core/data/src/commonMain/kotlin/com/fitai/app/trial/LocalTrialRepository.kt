package com.fitai.app.trial

import com.fitai.app.model.TrialDebugInfo
import com.fitai.app.model.TrialStartResult
import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.session.SessionPreferenceKeys
import com.fitai.app.util.epochMillis
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Dev flavor (and builds without Firebase): the server rules from [TrialPolicy] on a local ledger,
 * so the paywall → sign-up → trial flow runs without Cloud Functions. One trial per device key and
 * per account, like prod. The ledger survives sign-out (`SessionPreferenceKeys.DEV_TRIAL_LEDGER`) but
 * not a reinstall or "clear data"; only the prod server remembers across those.
 *
 * [accountId] is the signed-in account (null before sign-up), see [devTrialAccountId].
 */
class LocalTrialRepository(
    private val settings: Settings,
    private val deviceKeyProvider: DeviceKeyProvider,
    private val accountId: suspend () -> String?,
    private val clock: () -> Long = ::epochMillis,
) : TrialRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private val requestFlag = TrialStatusCache(settings)

    override val isFirebaseAvailable: Boolean get() = TrialAuth.available

    override suspend fun ensureAnonymousAuth(): Result<String> =
        if (TrialAuth.available) runCatching { TrialAuth.ensureAnonymousAuth() } else Result.success(LOCAL_GUEST)

    override suspend fun hasAccount(): Boolean = accountId() != null

    override var trialRequested: Boolean
        get() = requestFlag.trialRequested
        set(value) { requestFlag.trialRequested = value }

    override suspend fun getTrialStatus(forceRefresh: Boolean): Result<TrialStatus> = runCatching {
        val account = accountId()
        val device = deviceKeyProvider.deviceKey().value
        val ledger = readLedger()
        TrialPolicy.statusFor(
            accountId = account,
            accountTrial = account?.let { ledger.accounts[it]?.period() },
            deviceClaim = ledger.devices[device]?.claim(),
            nowMillis = clock(),
        )
    }

    override suspend fun startTrial(): Result<TrialStartResult> = runCatching {
        val account = accountId() ?: error(TrialErrors.ACCOUNT_REQUIRED)
        val device = deviceKeyProvider.deviceKey().value
        mutex.withLock {
            val ledger = readLedger()
            val now = clock()
            when (val decision = TrialPolicy.decideStart(account, ledger.accounts[account]?.period(), ledger.devices[device]?.claim(), now)) {
                is TrialStartDecision.Existing -> {
                    if (account !in ledger.accounts) {
                        writeLedger(ledger.copy(accounts = ledger.accounts + (account to AccountEntry.of(decision.period, device))))
                    }
                    TrialStartResult(started = false, status = TrialPolicy.statusOf(decision.period, now))
                }
                TrialStartDecision.DeviceUsed ->
                    TrialStartResult(started = false, status = TrialPolicy.noTrial(TrialState.UsedOnDevice))
                is TrialStartDecision.Start -> {
                    writeLedger(
                        ledger.copy(
                            accounts = ledger.accounts + (account to AccountEntry.of(decision.period, device)),
                            devices = ledger.devices + (device to DeviceEntry.of(account, decision.period)),
                        ),
                    )
                    TrialStartResult(started = true, status = TrialPolicy.statusOf(decision.period, now))
                }
            }
        }
    }

    override suspend fun canAccessAiFeatures(): Result<Boolean> = Result.success(true)

    override suspend fun debugInfo(): TrialDebugInfo? {
        if (!deviceKeyProvider.showsDiagnostics) return null
        val key = deviceKeyProvider.deviceKey()
        val status = getTrialStatus().getOrNull()
        return TrialDebugInfo(
            state = status?.state,
            daysRemaining = status?.daysRemaining ?: 0,
            deviceKeyPrefix = key.value.take(DEBUG_PREFIX_LENGTH),
            keySource = key.source.code,
            backend = "local",
        )
    }

    private fun readLedger(): Ledger = settings.getStringOrNull(SessionPreferenceKeys.DEV_TRIAL_LEDGER)
        ?.let { raw -> runCatching { json.decodeFromString(Ledger.serializer(), raw) }.getOrNull() }
        ?: Ledger()

    private fun writeLedger(ledger: Ledger) {
        settings[SessionPreferenceKeys.DEV_TRIAL_LEDGER] = json.encodeToString(Ledger.serializer(), ledger)
    }

    @Serializable
    private data class Ledger(
        val accounts: Map<String, AccountEntry> = emptyMap(),
        val devices: Map<String, DeviceEntry> = emptyMap(),
    )

    @Serializable
    private data class AccountEntry(val startedAtMillis: Long, val endsAtMillis: Long, val deviceKey: String) {
        fun period() = TrialPeriod(startedAtMillis, endsAtMillis)

        companion object {
            fun of(period: TrialPeriod, deviceKey: String) = AccountEntry(period.startedAtMillis, period.endsAtMillis, deviceKey)
        }
    }

    @Serializable
    private data class DeviceEntry(val firstAccountId: String, val startedAtMillis: Long, val endsAtMillis: Long) {
        fun claim() = DeviceTrialClaim(firstAccountId, TrialPeriod(startedAtMillis, endsAtMillis))

        companion object {
            fun of(accountId: String, period: TrialPeriod) = DeviceEntry(accountId, period.startedAtMillis, period.endsAtMillis)
        }
    }

    private companion object {
        const val LOCAL_GUEST = "local-guest"
        const val DEBUG_PREFIX_LENGTH = 8
    }
}

/**
 * The account the dev fake keys trials on: the signed-in Firebase uid; without Firebase at all, the
 * on-device profile (so the flow still runs offline). Null on the anonymous session before sign-up.
 */
suspend fun devTrialAccountId(localUserRepository: LocalUserRepository): String? =
    if (TrialAuth.available) {
        TrialAuth.accountUid()
    } else {
        localUserRepository.getLatestUser()?.let { user -> user.firebaseUid ?: "local-${user.id}" }
    }
