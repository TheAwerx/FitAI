package com.fitai.app.trial

import com.fitai.app.access.AccessTier
import com.fitai.app.access.EntitlementRepositoryImpl
import com.fitai.app.billing.BillingSettingsKeys
import com.fitai.app.model.TrialState
import com.fitai.app.session.SessionPreferenceKeys
import com.russhwolf.settings.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private class FakeDeviceKeys(var key: String = "a".repeat(64), override val showsDiagnostics: Boolean = true) : DeviceKeyProvider {
    override suspend fun deviceKey() = DeviceKey(key, DevicePlatform.Android, DeviceKeySource.AndroidId)
}

/** The dev fake must follow the server rules: one trial per device key and per account. */
class LocalTrialRepositoryTest {

    private val day = TrialPolicy.DAY_MILLIS
    private val t0 = 1_763_193_600_000L

    private class Device(val settings: InMemorySettings = InMemorySettings(), val keys: FakeDeviceKeys = FakeDeviceKeys()) {
        var account: String? = null
        var now: Long = 0
        fun repository() = LocalTrialRepository(settings, keys, accountId = { account }, clock = { now })
    }

    private fun device(key: String = "a".repeat(64)) = Device(keys = FakeDeviceKeys(key)).also { it.now = t0 }

    @Test
    fun firstAccountOnTheDeviceGetsSevenDays() = runTest {
        val phone = device()
        val repo = phone.repository()
        assertEquals(TrialState.Eligible, repo.getTrialStatus().getOrThrow().state)
        phone.account = "alice"
        val result = repo.startTrial().getOrThrow()
        assertTrue(result.started)
        assertEquals(TrialState.Active, result.status.state)
        assertEquals(7, result.status.daysRemaining)
        assertEquals(TrialState.Active, repo.getTrialStatus().getOrThrow().state)
    }

    @Test
    fun secondAccountOnTheSameDeviceIsRefused() = runTest {
        val phone = device()
        val repo = phone.repository()
        phone.account = "alice"
        repo.startTrial().getOrThrow()

        phone.account = "bob"
        val refused = repo.startTrial().getOrThrow()
        assertFalse(refused.started)
        assertEquals(TrialState.UsedOnDevice, refused.status.state)
        assertEquals(TrialState.UsedOnDevice, repo.getTrialStatus().getOrThrow().state)
        // The paywall before sign-up sees it too.
        phone.account = null
        assertEquals(TrialState.UsedOnDevice, repo.getTrialStatus().getOrThrow().state)
    }

    @Test
    fun sameAccountKeepsItsTrialWhenTheDeviceKeyChanges() = runTest {
        val phone = device()
        val repo = phone.repository()
        phone.account = "alice"
        repo.startTrial().getOrThrow()

        phone.now = t0 + 3 * day
        phone.keys.key = "b".repeat(64)
        val again = repo.startTrial().getOrThrow()
        assertFalse(again.started)
        assertEquals(TrialState.Active, again.status.state)
        assertEquals(4, again.status.daysRemaining)
    }

    @Test
    fun expiresByTheClockAndIsNotRestarted() = runTest {
        val phone = device()
        val repo = phone.repository()
        phone.account = "alice"
        repo.startTrial().getOrThrow()
        phone.now = t0 + 7 * day
        assertEquals(TrialState.Expired, repo.getTrialStatus().getOrThrow().state)
        val again = repo.startTrial().getOrThrow()
        assertFalse(again.started)
        assertEquals(TrialState.Expired, again.status.state)
    }

    @Test
    fun needsAnAccountToStart() = runTest {
        val repo = device().repository()
        val failure = repo.startTrial().exceptionOrNull()
        assertEquals(TrialErrors.ACCOUNT_REQUIRED, failure?.message)
        assertFalse(repo.hasAccount())
    }

    @Test
    fun ledgerOutlivesTheRepositoryAndUsesTheKeptSettingsKey() = runTest {
        val phone = device()
        phone.account = "alice"
        phone.repository().startTrial().getOrThrow()
        assertNotNull(phone.settings.getStringOrNull(SessionPreferenceKeys.DEV_TRIAL_LEDGER))

        phone.account = "bob"
        assertEquals(TrialState.UsedOnDevice, phone.repository().startTrial().getOrThrow().status.state)
    }

    @Test
    fun requestFlagIsStoredInSettings() = runTest {
        val phone = device()
        val repo = phone.repository()
        assertFalse(repo.trialRequested)
        repo.trialRequested = true
        assertTrue(phone.repository().trialRequested)
        repo.trialRequested = false
        assertFalse(phone.repository().trialRequested)
    }

    @Test
    fun debugInfoShowsOnlyAKeyPrefix() = runTest {
        val phone = device()
        val info = phone.repository().debugInfo()
        assertEquals("aaaaaaaa", info?.deviceKeyPrefix)
        assertEquals("android_id", info?.keySource)
        assertEquals("local", info?.backend)
        val release = Device(keys = FakeDeviceKeys(showsDiagnostics = false))
        assertNull(release.repository().debugInfo())
    }

    @Test
    fun entitlementFollowsTheTrial() = runTest {
        val phone = device()
        val repo = phone.repository()
        val entitlement = EntitlementRepositoryImpl(repo, phone.settings)
        phone.account = "alice"
        assertEquals(AccessTier.Free, entitlement.currentTier())
        repo.startTrial().getOrThrow()
        assertEquals(AccessTier.Trial, entitlement.currentTier())
        phone.now = t0 + 8 * day
        assertEquals(AccessTier.Free, entitlement.currentTier())
        phone.settings[BillingSettingsKeys.MOCK_SUBSCRIPTION_ACTIVE] = true
        assertEquals(AccessTier.Pro, entitlement.currentTier())
    }
}

class TrialStatusCacheTest {

    @Test
    fun snapshotRoundTrips() {
        val cache = TrialStatusCache(InMemorySettings())
        assertNull(cache.read())
        val snapshot = TrialSnapshot("alice", TrialState.Active, 1L, 2L, 3L, 4L)
        cache.write(snapshot)
        assertEquals(snapshot, cache.read())
    }

    @Test
    fun serverAnswerMapsToASnapshot() {
        val dto = TrialStatusDto(
            state = "used",
            startedAt = null,
            endsAt = null,
            serverTime = "2025-11-15T08:00:00.000Z",
        )
        val snapshot = dto.toSnapshot(accountId = null, deviceNowMillis = 99L)
        assertEquals(TrialState.UsedOnDevice, snapshot.state)
        assertEquals(1_763_193_600_000L, snapshot.serverTimeMillis)
        assertEquals(99L, snapshot.fetchedAtMillis)
        val active = TrialStatusDto(state = "active", startedAt = "2025-11-15T08:00:00.000Z", endsAt = "2025-11-22T08:00:00.000Z")
            .toSnapshot("alice", 5L)
        assertEquals(1_763_193_600_000L + 7 * TrialPolicy.DAY_MILLIS, active.endsAtMillis)
        // No server time: fall back to the device clock.
        assertEquals(5L, active.serverTimeMillis)
    }
}
