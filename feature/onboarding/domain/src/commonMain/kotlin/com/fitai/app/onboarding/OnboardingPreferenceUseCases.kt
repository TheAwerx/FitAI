package com.fitai.app.onboarding

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.preferences.AttributionSources
import com.fitai.app.preferences.UserPreferencesRepository
import com.fitai.app.util.nowIsoTimestamp

class SaveAttributionUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(source: String): UseCaseState<Unit> = useCaseCatching {
        require(source in AttributionSources.all) { "unknown_attribution_source" }
        userPreferencesRepository.attributionSource = source
    }
}

class ConfirmCommitmentUseCase(
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    operator fun invoke(daysPerWeek: Int): UseCaseState<Unit> = useCaseCatching {
        require(daysPerWeek in MIN_DAYS..MAX_DAYS) { "invalid_commitment_days" }
        userPreferencesRepository.commitmentDaysPerWeek = daysPerWeek
        userPreferencesRepository.commitmentConfirmedAt = nowIsoTimestamp()
    }

    companion object {
        const val MIN_DAYS = 2
        const val MAX_DAYS = 6
    }
}
