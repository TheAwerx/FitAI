package com.fitai.app.trial

import com.fitai.app.model.TrialState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrialPolicyTest {

    private val day = TrialPolicy.DAY_MILLIS
    private val t0 = 1_763_193_600_000L // 2025-11-15T08:00:00Z
    private val period = TrialPeriod(t0, t0 + 7 * day)

    @Test
    fun firstAccountOnADeviceStartsSevenDays() {
        val decision = TrialPolicy.decideStart("alice", accountTrial = null, deviceClaim = null, nowMillis = t0)
        assertEquals(TrialStartDecision.Start(period), decision)
    }

    @Test
    fun secondAccountOnTheSameDeviceIsRefused() {
        val claim = DeviceTrialClaim("alice", period)
        assertEquals(TrialStartDecision.DeviceUsed, TrialPolicy.decideStart("bob", null, claim, t0 + day))
        assertEquals(TrialState.UsedOnDevice, TrialPolicy.statusFor("bob", null, claim, t0 + day).state)
    }

    @Test
    fun anAccountsTrialIsNeverRestartedOnAnotherDevice() {
        val decision = TrialPolicy.decideStart("alice", accountTrial = period, deviceClaim = null, nowMillis = t0 + 3 * day)
        assertEquals(TrialStartDecision.Existing(period), decision)
        val status = TrialPolicy.statusFor("alice", period, deviceClaim = null, nowMillis = t0 + 3 * day)
        assertEquals(TrialState.Active, status.state)
        assertEquals(4, status.daysRemaining)
        // Expired stays expired, wherever the account signs in.
        assertIs<TrialStartDecision.Existing>(TrialPolicy.decideStart("alice", period, null, t0 + 30 * day))
        assertEquals(TrialState.Expired, TrialPolicy.statusFor("alice", period, null, t0 + 30 * day).state)
    }

    @Test
    fun theDeviceClaimRestoresTheSameAccount() {
        val claim = DeviceTrialClaim("alice", period)
        assertEquals(TrialStartDecision.Existing(period), TrialPolicy.decideStart("alice", null, claim, t0 + day))
        assertEquals(TrialState.Active, TrialPolicy.statusFor("alice", null, claim, t0 + day).state)
    }

    @Test
    fun beforeSignUpOnlyTheDeviceCounts() {
        assertEquals(TrialState.Eligible, TrialPolicy.statusFor(null, null, null, t0).state)
        assertEquals(TrialState.UsedOnDevice, TrialPolicy.statusFor(null, null, DeviceTrialClaim("alice", period), t0).state)
        assertTrue(TrialPolicy.statusFor(null, null, null, t0).canStart)
        assertFalse(TrialPolicy.statusFor(null, null, DeviceTrialClaim("alice", period), t0).canStart)
    }

    @Test
    fun expiresExactlyAtTheEnd() {
        assertEquals(TrialState.Active, TrialPolicy.statusOf(period, t0 + 7 * day - 1).state)
        assertEquals(1, TrialPolicy.statusOf(period, t0 + 7 * day - 1).daysRemaining)
        val ended = TrialPolicy.statusOf(period, t0 + 7 * day)
        assertEquals(TrialState.Expired, ended.state)
        assertEquals(0, ended.daysRemaining)
        assertEquals("2025-11-22T08:00:00Z", ended.trialExpiresAt)
    }

    @Test
    fun daysRemainingRoundsUp() {
        assertEquals(7, TrialPolicy.daysRemaining(t0 + 7 * day, t0))
        assertEquals(7, TrialPolicy.daysRemaining(t0 + 7 * day, t0 + 1))
        assertEquals(1, TrialPolicy.daysRemaining(t0 + 1, t0))
        assertEquals(0, TrialPolicy.daysRemaining(t0, t0))
        assertEquals(0, TrialPolicy.daysRemaining(t0, t0 + day))
    }

    private fun activeSnapshot(fetchedAt: Long = t0 + day, serverTime: Long = t0 + day) = TrialSnapshot(
        accountId = "alice",
        state = TrialState.Active,
        startedAtMillis = period.startedAtMillis,
        endsAtMillis = period.endsAtMillis,
        serverTimeMillis = serverTime,
        fetchedAtMillis = fetchedAt,
    )

    @Test
    fun cachedTrialAdvancesWithTimeOnTheDevice() {
        val snapshot = activeSnapshot()
        assertEquals(6, TrialPolicy.resolve(snapshot, t0 + day)?.daysRemaining)
        assertEquals(5, TrialPolicy.resolve(snapshot, t0 + 2 * day + 1)?.daysRemaining)
    }

    @Test
    fun cachedTrialUsesServerTimeNotTheDeviceClock() {
        // Device clock is 3 days behind the server: the trial still ends on server time.
        val snapshot = activeSnapshot(fetchedAt = t0 - 2 * day, serverTime = t0 + day)
        assertEquals(6, TrialPolicy.resolve(snapshot, t0 - 2 * day)?.daysRemaining)
        assertEquals(TrialState.Expired, TrialPolicy.resolve(snapshot, t0 + 4 * day)?.state)
    }

    @Test
    fun offlineGraceEndsAnUnverifiedTrial() {
        val snapshot = activeSnapshot()
        assertEquals(TrialState.Active, TrialPolicy.resolve(snapshot, t0 + day + TrialPolicy.OFFLINE_GRACE_MILLIS)?.state)
        assertNull(TrialPolicy.resolve(snapshot, t0 + day + TrialPolicy.OFFLINE_GRACE_MILLIS + 1))
    }

    @Test
    fun aClockMovedBackwardsInvalidatesTheCache() {
        val snapshot = activeSnapshot()
        assertNull(TrialPolicy.resolve(snapshot, t0 + day - TrialPolicy.CLOCK_SKEW_MILLIS - 1))
        // Small NTP corrections are fine.
        assertEquals(6, TrialPolicy.resolve(snapshot, t0 + day - 1_000)?.daysRemaining)
    }

    @Test
    fun offlineTimeNeverReopensAFinishedOrRefusedTrial() {
        val expired = activeSnapshot().copy(state = TrialState.Expired, serverTimeMillis = t0 + 8 * day)
        assertEquals(TrialState.Expired, TrialPolicy.resolve(expired, t0 + 400 * day)?.state)
        val used = activeSnapshot().copy(state = TrialState.UsedOnDevice, startedAtMillis = null, endsAtMillis = null)
        assertEquals(TrialState.UsedOnDevice, TrialPolicy.resolve(used, t0 + 400 * day)?.state)
        val eligible = used.copy(state = TrialState.Eligible)
        assertEquals(TrialState.Eligible, TrialPolicy.resolve(eligible, t0 + 400 * day)?.state)
    }

    @Test
    fun freshnessWindow() {
        val snapshot = activeSnapshot()
        assertTrue(TrialPolicy.isFresh(snapshot, t0 + day))
        assertTrue(TrialPolicy.isFresh(snapshot, t0 + day + TrialPolicy.REFRESH_INTERVAL_MILLIS - 1))
        assertFalse(TrialPolicy.isFresh(snapshot, t0 + day + TrialPolicy.REFRESH_INTERVAL_MILLIS))
        assertFalse(TrialPolicy.isFresh(snapshot, t0 + day - TrialPolicy.CLOCK_SKEW_MILLIS - 1))
    }

    @Test
    fun parsesServerInstants() {
        assertEquals(t0, TrialPolicy.parseIsoMillis("2025-11-15T08:00:00.000Z"))
        assertNull(TrialPolicy.parseIsoMillis(""))
        assertNull(TrialPolicy.parseIsoMillis(null))
        assertNull(TrialPolicy.parseIsoMillis("yesterday"))
    }
}
