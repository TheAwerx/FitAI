package com.fitai.app.access

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching

class GetAccessTierUseCase(
    private val entitlementRepository: EntitlementRepository,
) {
    suspend operator fun invoke(): UseCaseState<AccessTier> = useCaseCatching {
        entitlementRepository.currentTier()
    }
}

class CheckFeatureAccessUseCase(
    private val entitlementRepository: EntitlementRepository,
    private val usageLimitRepository: UsageLimitRepository,
) {
    suspend operator fun invoke(feature: GatedFeature): UseCaseState<FeatureAccess> = useCaseCatching {
        when (val tier = entitlementRepository.currentTier()) {
            AccessTier.Trial, AccessTier.Pro -> FeatureAccess(feature, tier, allowed = true, remaining = null)
            AccessTier.Free -> {
                val used = usageLimitRepository.usedInCurrentPeriod(feature)
                val remaining = (feature.freeQuota - used).coerceAtLeast(0)
                FeatureAccess(feature, tier, allowed = remaining > 0, remaining = remaining)
            }
        }
    }
}

class RecordFeatureUseUseCase(
    private val entitlementRepository: EntitlementRepository,
    private val usageLimitRepository: UsageLimitRepository,
) {
    suspend operator fun invoke(feature: GatedFeature): UseCaseState<Unit> = useCaseCatching {
        if (entitlementRepository.currentTier() == AccessTier.Free) {
            usageLimitRepository.recordUse(feature)
        }
    }
}
