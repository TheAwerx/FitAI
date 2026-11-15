package com.fitai.app.access

import com.fitai.app.domain.GenerateWorkoutPlanUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.model.GeneratedWorkoutPlan

class GetPlanQuotaUseCase(
    private val planQuotaRepository: PlanQuotaRepository,
) {
    suspend operator fun invoke(): UseCaseState<PlanQuota> = useCaseCatching { planQuotaRepository.getQuota() }
}

/** A generation that spent one regeneration right, with the quota left afterwards. */
data class PlanRegeneration<T>(
    val value: T,
    val quota: PlanQuota,
)

/**
 * Spends one plan regeneration right around [invoke]'s `generate`. Refuses with
 * [PlanQuotaErrors.EXHAUSTED] when none is left. A failed generation keeps the right.
 */
class SpendPlanRegenerationUseCase(
    private val planQuotaRepository: PlanQuotaRepository,
) {
    suspend operator fun <T> invoke(generate: suspend () -> UseCaseState<T>): UseCaseState<PlanRegeneration<T>> {
        val quota = when (val current = useCaseCatching { planQuotaRepository.getQuota() }) {
            is UseCaseState.Error -> return current
            is UseCaseState.Success -> current.data
        }
        if (quota.exhausted) return UseCaseState.Error(PlanQuotaErrors.EXHAUSTED)
        return when (val result = generate()) {
            is UseCaseState.Error -> {
                if (result.message == PlanQuotaErrors.EXHAUSTED) {
                    useCaseCatching { planQuotaRepository.markExhausted() }
                }
                result
            }
            is UseCaseState.Success -> {
                val updated = when (val recorded = useCaseCatching { planQuotaRepository.recordRegeneration() }) {
                    is UseCaseState.Success -> recorded.data
                    is UseCaseState.Error -> quota.copy(used = quota.used + 1)
                }
                UseCaseState.Success(PlanRegeneration(result.data, updated))
            }
        }
    }
}

/** "Planı yenile" for the workout plan. Shared by the Workout tab and Settings so both spend the same rights. */
class RegenerateWorkoutPlanUseCase(
    private val spendPlanRegenerationUseCase: SpendPlanRegenerationUseCase,
    private val generateWorkoutPlanUseCase: GenerateWorkoutPlanUseCase,
) {
    suspend operator fun invoke(): UseCaseState<PlanRegeneration<GeneratedWorkoutPlan>> =
        spendPlanRegenerationUseCase { generateWorkoutPlanUseCase(PlanGenerationPurpose.Regenerate) }
}
