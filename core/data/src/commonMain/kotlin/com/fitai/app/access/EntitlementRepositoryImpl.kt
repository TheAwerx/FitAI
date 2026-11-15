package com.fitai.app.access

import com.fitai.app.billing.BillingSettingsKeys
import com.fitai.app.trial.TrialRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/** Falls back to the last resolved tier when the trial status cannot be fetched (e.g. offline). */
class EntitlementRepositoryImpl(
    private val trialRepository: TrialRepository,
    private val settings: Settings,
) : EntitlementRepository {

    override suspend fun currentTier(): AccessTier {
        if (settings.getBoolean(BillingSettingsKeys.MOCK_SUBSCRIPTION_ACTIVE, false)) return AccessTier.Pro
        val status = trialRepository.getTrialStatus().getOrNull() ?: return lastKnownTier()
        val tier = when {
            status.hasSubscription -> AccessTier.Pro
            status.isActive -> AccessTier.Trial
            else -> AccessTier.Free
        }
        settings[KEY_LAST_TIER] = tier.name
        return tier
    }

    private fun lastKnownTier(): AccessTier =
        settings.getStringOrNull(KEY_LAST_TIER)
            ?.let { name -> AccessTier.entries.find { it.name == name } }
            ?: AccessTier.Free

    private companion object {
        const val KEY_LAST_TIER = "entitlement_last_tier"
    }
}
