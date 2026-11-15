package com.fitai.app.access

import com.fitai.app.billing.BillingSettingsKeys
import com.fitai.app.trial.TrialRepository
import com.russhwolf.settings.Settings

/**
 * Pro from the (mock) subscription, Trial while the account's free trial runs, otherwise Free.
 * The trial repository caches the server answer for offline use; without a trustworthy answer the
 * tier is Free (an unverified trial does not stay unlocked).
 */
class EntitlementRepositoryImpl(
    private val trialRepository: TrialRepository,
    private val settings: Settings,
) : EntitlementRepository {

    override suspend fun currentTier(): AccessTier {
        if (settings.getBoolean(BillingSettingsKeys.MOCK_SUBSCRIPTION_ACTIVE, false)) return AccessTier.Pro
        val status = trialRepository.getTrialStatus().getOrNull() ?: return AccessTier.Free
        return when {
            status.hasSubscription -> AccessTier.Pro
            status.isActive -> AccessTier.Trial
            else -> AccessTier.Free
        }
    }
}
