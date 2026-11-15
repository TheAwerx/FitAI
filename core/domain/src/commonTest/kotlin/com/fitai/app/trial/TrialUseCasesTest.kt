package com.fitai.app.trial

import com.fitai.app.domain.UseCaseState
import com.fitai.app.model.TrialDebugInfo
import com.fitai.app.model.TrialStartResult
import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private class FakeTrialRepository(
    var account: Boolean = false,
    var startResult: Result<TrialStartResult> = Result.success(
        TrialStartResult(started = true, status = TrialStatus(TrialState.Active, daysRemaining = 7)),
    ),
) : TrialRepository {
    var startCalls = 0

    override val isFirebaseAvailable: Boolean = false
    override suspend fun ensureAnonymousAuth(): Result<String> = Result.success("anon")
    override suspend fun hasAccount(): Boolean = account
    override suspend fun getTrialStatus(forceRefresh: Boolean): Result<TrialStatus> = Result.success(TrialStatus(TrialState.Eligible))
    override suspend fun startTrial(): Result<TrialStartResult> {
        startCalls++
        return startResult
    }
    override var trialRequested: Boolean = false
    override suspend fun canAccessAiFeatures(): Result<Boolean> = Result.success(account)
    override suspend fun debugInfo(): TrialDebugInfo? = null
}

class TrialUseCasesTest {

    @Test
    fun beforeSignUpTheTrialIsOnlyRequested() = runTest {
        val repository = FakeTrialRepository(account = false)
        val result = RequestTrialUseCase(repository)()
        assertIs<UseCaseState.Success<TrialRequestResult>>(result)
        assertEquals(TrialRequestOutcome.PendingSignup, result.data.outcome)
        assertTrue(repository.trialRequested)
        assertEquals(0, repository.startCalls)
    }

    @Test
    fun signedInStartsRightAway() = runTest {
        val repository = FakeTrialRepository(account = true)
        val result = RequestTrialUseCase(repository)()
        assertEquals(TrialRequestOutcome.Started, (result as UseCaseState.Success).data.outcome)
        assertFalse(repository.trialRequested)
    }

    @Test
    fun refusalsMapToTheReason() = runTest {
        fun outcomeOf(state: TrialState, started: Boolean = false) =
            TrialStartResult(started, TrialStatus(state)).toRequestResult().outcome
        assertEquals(TrialRequestOutcome.UsedOnDevice, outcomeOf(TrialState.UsedOnDevice))
        assertEquals(TrialRequestOutcome.UsedByAccount, outcomeOf(TrialState.Expired))
        assertEquals(TrialRequestOutcome.AlreadyActive, outcomeOf(TrialState.Active))
        assertEquals(TrialRequestOutcome.Started, outcomeOf(TrialState.Active, started = true))
    }

    @Test
    fun pendingTrialStartsAfterSignUp() = runTest {
        val repository = FakeTrialRepository(account = false)
        RequestTrialUseCase(repository)()
        val useCase = StartRequestedTrialUseCase(repository)

        // Still no account (e.g. "continue offline"): nothing happens, the request stays.
        assertNull((useCase() as UseCaseState.Success).data)
        assertTrue(repository.trialRequested)

        repository.account = true
        assertEquals(TrialRequestOutcome.Started, (useCase() as UseCaseState.Success).data?.outcome)
        assertFalse(repository.trialRequested)
        // Only once.
        assertNull((useCase() as UseCaseState.Success).data)
        assertEquals(1, repository.startCalls)
    }

    @Test
    fun aFailedStartKeepsTheRequestForARetry() = runTest {
        val repository = FakeTrialRepository(account = true, startResult = Result.failure(IllegalStateException("offline")))
        repository.trialRequested = true
        val result = StartRequestedTrialUseCase(repository)()
        assertIs<UseCaseState.Error>(result)
        assertTrue(repository.trialRequested)
    }

    @Test
    fun nothingPendingDoesNothing() = runTest {
        val repository = FakeTrialRepository(account = true)
        assertNull((StartRequestedTrialUseCase(repository)() as UseCaseState.Success).data)
        assertEquals(0, repository.startCalls)
    }
}
