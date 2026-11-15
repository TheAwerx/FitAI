package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.billing.GetSubscriptionProductsUseCase
import com.fitai.app.billing.PurchaseSubscriptionUseCase
import com.fitai.app.billing.RestorePurchasesUseCase
import com.fitai.app.billing.SubscriptionPeriod
import com.fitai.app.domain.UseCaseState
import com.fitai.app.onboarding.GetOnboardingSummaryUseCase
import com.fitai.app.viewmodel.paywall.PaywallEffect
import com.fitai.app.viewmodel.paywall.PaywallIntent
import com.fitai.app.viewmodel.paywall.PaywallMessage
import com.fitai.app.viewmodel.paywall.PaywallState
import kotlinx.coroutines.launch

/** Closing the paywall first shows the exit offer; declining it continues without the personal plan. */
class PaywallViewModel(
    private val getSubscriptionProductsUseCase: GetSubscriptionProductsUseCase,
    private val purchaseSubscriptionUseCase: PurchaseSubscriptionUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val getOnboardingSummaryUseCase: GetOnboardingSummaryUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<PaywallState, PaywallIntent, PaywallEffect>(PaywallState()) {

    init {
        trackEventUseCase(AnalyticsEvents.PAYWALL_VIEWED)
        loadProducts()
        viewModelScope.launch {
            val summary = (getOnboardingSummaryUseCase() as? UseCaseState.Success)?.data
            setState { copy(summary = summary) }
        }
    }

    override fun handleIntent(intent: PaywallIntent) {
        when (intent) {
            is PaywallIntent.ProductSelected -> {
                if (currentState().busy) return
                setState { copy(selectedProductId = intent.productId) }
            }
            PaywallIntent.Purchase -> purchase(currentState().selectedProduct?.id)
            PaywallIntent.PurchaseOffer -> purchase(currentState().yearlyProduct?.id)
            PaywallIntent.Restore -> restore()
            PaywallIntent.Close -> {
                if (currentState().busy) return
                trackEventUseCase(AnalyticsEvents.PAYWALL_EXIT_INTENT)
                setState { copy(showExitOffer = true) }
            }
            PaywallIntent.DismissExitOffer -> setState { copy(showExitOffer = false) }
            PaywallIntent.Decline -> {
                if (currentState().busy) return
                trackEventUseCase(AnalyticsEvents.PAYWALL_DECLINED)
                setState { copy(showExitOffer = false) }
                setEffect(PaywallEffect.Declined)
            }
            PaywallIntent.RetryLoad -> loadProducts()
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            setState { copy(loading = true, loadFailed = false) }
            when (val result = getSubscriptionProductsUseCase()) {
                is UseCaseState.Success -> {
                    val products = result.data
                    val default = products.firstOrNull { it.period == SubscriptionPeriod.Yearly } ?: products.firstOrNull()
                    setState {
                        copy(
                            loading = false,
                            loadFailed = products.isEmpty(),
                            products = products,
                            selectedProductId = selectedProductId?.takeIf { id -> products.any { it.id == id } }
                                ?: default?.id,
                        )
                    }
                }
                is UseCaseState.Error -> setState { copy(loading = false, loadFailed = true) }
            }
        }
    }

    private fun purchase(productId: String?) {
        val s = currentState()
        if (productId == null || s.busy) return
        val trialDays = s.products.firstOrNull { it.id == productId }
            ?.takeIf { it.period == SubscriptionPeriod.Yearly }?.trialDays ?: 0
        val params = mapOf(PARAM_PRODUCT_ID to productId)
        viewModelScope.launch {
            setState { copy(purchasing = true, selectedProductId = productId) }
            trackEventUseCase(AnalyticsEvents.PURCHASE_STARTED, params)
            when (purchaseSubscriptionUseCase(productId)) {
                is UseCaseState.Success -> {
                    trackEventUseCase(AnalyticsEvents.PURCHASE_COMPLETED, params + (PARAM_TRIAL_DAYS to trialDays.toString()))
                    setState { copy(purchasing = false, showExitOffer = false) }
                    setEffect(PaywallEffect.Unlocked)
                }
                is UseCaseState.Error -> {
                    setState { copy(purchasing = false) }
                    setEffect(PaywallEffect.ShowMessage(PaywallMessage.GenericError))
                }
            }
        }
    }

    private fun restore() {
        if (currentState().busy) return
        viewModelScope.launch {
            setState { copy(restoring = true) }
            val result = restorePurchasesUseCase()
            setState { copy(restoring = false) }
            when {
                result is UseCaseState.Success && result.data -> {
                    setEffect(PaywallEffect.ShowMessage(PaywallMessage.RestoreSuccess))
                    setEffect(PaywallEffect.Unlocked)
                }
                result is UseCaseState.Success ->
                    setEffect(PaywallEffect.ShowMessage(PaywallMessage.RestoreNotFound))
                else -> setEffect(PaywallEffect.ShowMessage(PaywallMessage.GenericError))
            }
        }
    }

    private companion object {
        const val PARAM_PRODUCT_ID = "product_id"
        const val PARAM_TRIAL_DAYS = "trial_days"
    }
}
