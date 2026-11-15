package com.fitai.app.trial

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.toUseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.TrialDebugInfo
import com.fitai.app.model.TrialStartResult
import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus

enum class TrialRequestOutcome {
    Started,
    /** The account's trial is already running (e.g. it started on another device). */
    AlreadyActive,
    /** No account yet (paywall before sign-up); the trial starts after sign-up. */
    PendingSignup,
    UsedOnDevice,
    /** This account's trial already ended. */
    UsedByAccount,
}

data class TrialRequestResult(
    val outcome: TrialRequestOutcome,
    val status: TrialStatus?,
)

internal fun TrialStartResult.toRequestResult(): TrialRequestResult {
    val outcome = when {
        started -> TrialRequestOutcome.Started
        status.state == TrialState.Active -> TrialRequestOutcome.AlreadyActive
        status.state == TrialState.Expired -> TrialRequestOutcome.UsedByAccount
        else -> TrialRequestOutcome.UsedOnDevice
    }
    return TrialRequestResult(outcome, status)
}

class GetTrialStatusUseCase(
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): UseCaseState<TrialStatus> =
        trialRepository.getTrialStatus(forceRefresh).toUseCaseState()
}

/** Paywall "start free trial": starts now when signed in, otherwise remembers it for after sign-up. */
class RequestTrialUseCase(
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(): UseCaseState<TrialRequestResult> = useCaseCatching {
        if (!trialRepository.hasAccount()) {
            trialRepository.trialRequested = true
            return@useCaseCatching TrialRequestResult(TrialRequestOutcome.PendingSignup, status = null)
        }
        val result = trialRepository.startTrial().getOrThrow()
        trialRepository.trialRequested = false
        result.toRequestResult()
    }
}

/**
 * Starts the trial asked for on the paywall before sign-up (plan setup, and Home as a retry when the
 * first attempt was offline). Success(null) when nothing is pending or there is still no account.
 * A failed call keeps the request pending.
 */
class StartRequestedTrialUseCase(
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(): UseCaseState<TrialRequestResult?> = useCaseCatching {
        if (!trialRepository.trialRequested || !trialRepository.hasAccount()) return@useCaseCatching null
        val result = trialRepository.startTrial().getOrThrow()
        trialRepository.trialRequested = false
        result.toRequestResult()
    }
}

class GetTrialDebugInfoUseCase(
    private val trialRepository: TrialRepository,
) {
    suspend operator fun invoke(): UseCaseState<TrialDebugInfo?> = useCaseCatching { trialRepository.debugInfo() }
}
