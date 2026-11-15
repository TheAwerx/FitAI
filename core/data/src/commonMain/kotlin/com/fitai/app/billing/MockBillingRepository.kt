package com.fitai.app.billing

import com.fitai.app.util.AppLanguage
import com.fitai.app.util.deviceLanguageCode
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

internal object BillingSettingsKeys {
    const val MOCK_SUBSCRIPTION_ACTIVE = "billing_mock_subscription_active"
    const val MOCK_SUBSCRIPTION_PRODUCT = "billing_mock_subscription_product"
}

/** Stand-in until RevenueCat is wired; a purchase flips a local flag that `EntitlementRepositoryImpl` reads as Pro. */
class MockBillingRepository(
    private val settings: Settings,
) : BillingRepository {

    override suspend fun getProducts(): Result<List<SubscriptionProduct>> = runCatching {
        // The labels carry "/yıl", "/ay": follow the language picked in the app, not only the device.
        val turkish = (AppLanguage.override ?: deviceLanguageCode()) == "tr"
        listOf(
            SubscriptionProduct(
                id = PRODUCT_YEARLY,
                period = SubscriptionPeriod.Yearly,
                priceLabel = if (turkish) "999,99 ₺/yıl" else "$44.99/yr",
                pricePerMonthLabel = if (turkish) "83,33 ₺/ay" else "$3.75/mo",
                trialDays = TRIAL_DAYS,
                savingsPercent = if (turkish) 54 else 71,
                originalPriceLabel = if (turkish) "2.159,88 ₺" else "$155.88",
                pricePerWeekLabel = if (turkish) "19,23 ₺" else "$0.87",
            ),
            SubscriptionProduct(
                id = PRODUCT_MONTHLY,
                period = SubscriptionPeriod.Monthly,
                priceLabel = if (turkish) "179,99 ₺/ay" else "$12.99/mo",
                pricePerMonthLabel = if (turkish) "179,99 ₺/ay" else "$12.99/mo",
                trialDays = 0,
                savingsPercent = null,
                pricePerWeekLabel = if (turkish) "41,54 ₺" else "$3.00",
            ),
        )
    }

    override suspend fun purchase(productId: String): Result<Unit> = runCatching {
        require(productId == PRODUCT_YEARLY || productId == PRODUCT_MONTHLY) { "unknown_product" }
        settings[BillingSettingsKeys.MOCK_SUBSCRIPTION_ACTIVE] = true
        settings[BillingSettingsKeys.MOCK_SUBSCRIPTION_PRODUCT] = productId
    }

    override suspend fun restore(): Result<Boolean> = runCatching {
        settings.getBoolean(BillingSettingsKeys.MOCK_SUBSCRIPTION_ACTIVE, false)
    }

    companion object {
        const val PRODUCT_YEARLY = "fitai_pro_yearly"
        const val PRODUCT_MONTHLY = "fitai_pro_monthly"
        private const val TRIAL_DAYS = 7
    }
}
