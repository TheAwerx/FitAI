package com.fitai.app.viewmodel.paywall

import com.fitai.app.billing.SubscriptionPeriod
import com.fitai.app.billing.SubscriptionProduct
import com.fitai.app.model.TrialState
import com.fitai.app.model.TrialStatus
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
    /**
     * Free-trial answer for this account or, before sign-up, this device. Null when it could not be
     * checked (offline): the trial is still offered, because starting it is verified by the server.
     */
    val trialStatus: TrialStatus? = null,
    val requestingTrial: Boolean = false,
) {
    val selectedProduct: SubscriptionProduct?
        get() = products.firstOrNull { it.id == selectedProductId }

    val yearlyProduct: SubscriptionProduct?
        get() = products.firstOrNull { it.period == SubscriptionPeriod.Yearly }

    val busy: Boolean get() = purchasing || restoring || requestingTrial

    /** One trial per account and device: not offered once this device or account used it. */
    val trialOffered: Boolean get() = trialStatus?.canStart ?: true

    /** Why the trial isn't offered, for the notice card; null when it is offered. */
    val trialNotice: TrialStatus?
        get() = trialStatus?.takeIf {
            it.state == TrialState.UsedOnDevice || it.state == TrialState.Expired || it.state == TrialState.Active
        }

    fun trialDaysFor(product: SubscriptionProduct): Int =
        if (trialOffered && product.period == SubscriptionPeriod.Yearly) product.trialDays else 0

    /** Trial days the primary CTA advertises; 0 means a plain subscribe CTA. */
    val selectedTrialDays: Int
        get() = selectedProduct?.let(::trialDaysFor) ?: 0
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

enum class PaywallMessage {
    RestoreSuccess,
    RestoreNotFound,
    GenericError,
    TrialUsedOnDevice,
    TrialUsedByAccount,
    TrialRequiresAccount,
    TrialStartFailed,
}

sealed interface PaywallEffect {
    /**
     * Purchase, restore or trial start succeeded; the personal AI plan is unlocked. Before sign-up the
     * trial is only requested and starts right after sign-up.
     */
    data object Unlocked : PaywallEffect
    /** The user left without buying; they continue with the starter plan. */
    data object Declined : PaywallEffect
    data class ShowMessage(val message: PaywallMessage) : PaywallEffect
}
