package com.fitai.app.viewmodel.paywall

import com.fitai.app.billing.SubscriptionPeriod
import com.fitai.app.billing.SubscriptionProduct
import com.fitai.app.onboarding.OnboardingSummary

data class PaywallState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val products: List<SubscriptionProduct> = emptyList(),
    val selectedProductId: String? = null,
    val purchasing: Boolean = false,
    val restoring: Boolean = false,
    /** Answers from onboarding for the "your plan" card; null outside the funnel. */
    val summary: OnboardingSummary? = null,
    val showExitOffer: Boolean = false,
) {
    val selectedProduct: SubscriptionProduct?
        get() = products.firstOrNull { it.id == selectedProductId }

    val yearlyProduct: SubscriptionProduct?
        get() = products.firstOrNull { it.period == SubscriptionPeriod.Yearly }

    val busy: Boolean get() = purchasing || restoring

    /** Trial days the primary CTA advertises; 0 means a plain subscribe CTA. */
    val selectedTrialDays: Int
        get() = selectedProduct?.takeIf { it.period == SubscriptionPeriod.Yearly }?.trialDays ?: 0
}

sealed interface PaywallIntent {
    data class ProductSelected(val productId: String) : PaywallIntent
    data object Purchase : PaywallIntent
    /** Exit-offer CTA: buys the yearly plan. */
    data object PurchaseOffer : PaywallIntent
    data object Restore : PaywallIntent
    data object Close : PaywallIntent
    data object DismissExitOffer : PaywallIntent
    data object Decline : PaywallIntent
    data object RetryLoad : PaywallIntent
}

enum class PaywallMessage { RestoreSuccess, RestoreNotFound, GenericError }

sealed interface PaywallEffect {
    /** Purchase or restore succeeded; the personal AI plan is unlocked. */
    data object Unlocked : PaywallEffect
    /** The user left without buying; they continue with the starter plan. */
    data object Declined : PaywallEffect
    data class ShowMessage(val message: PaywallMessage) : PaywallEffect
}
