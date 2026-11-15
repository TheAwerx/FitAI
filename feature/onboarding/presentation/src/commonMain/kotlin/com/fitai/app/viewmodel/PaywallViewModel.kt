package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.billing.GetSubscriptionProductsUseCase
import com.fitai.app.billing.PurchaseSubscriptionUseCase
import com.fitai.app.billing.RestorePurchasesUseCase
import com.fitai.app.billing.SubscriptionPeriod
import com.fitai.app.billing.SubscriptionProduct
import com.fitai.app.domain.UseCaseState
import com.fitai.app.onboarding.GetOnboardingSummaryUseCase
import com.fitai.app.trial.GetTrialStatusUseCase
import com.fitai.app.trial.RequestTrialUseCase
import com.fitai.app.trial.TrialErrors
import com.fitai.app.trial.TrialRequestOutcome
import com.fitai.app.trial.TrialRequestResult
import com.fitai.app.viewmodel.paywall.PaywallEffect
import com.fitai.app.viewmodel.paywall.PaywallIntent
import com.fitai.app.viewmodel.paywall.PaywallMessage
import com.fitai.app.viewmodel.paywall.PaywallState
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Closing the paywall first shows the exit offer; declining it continues without the personal plan.
 * The yearly plan's free trial is offered only while this account and device haven't used it
 * (server-checked); its CTA starts the app trial instead of a purchase.
 */
class PaywallViewModel(
    private val getSubscriptionProductsUseCase: GetSubscriptionProductsUseCase,
    private val purchaseSubscriptionUseCase: PurchaseSubscriptionUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase,
    private val getOnboardingSummaryUseCase: GetOnboardingSummaryUseCase,
    private val getTrialStatusUseCase: GetTrialStatusUseCase,
    private val requestTrialUseCase: RequestTrialUseCase,
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
            PaywallIntent.Purchase -> buyOrStartTrial(currentState().selectedProduct)
            PaywallIntent.PurchaseOffer -> buyOrStartTrial(currentState().yearlyProduct)
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
            // Checked with the products so the trial label doesn't flash in and out.
            val trial = async {
                withTimeoutOrNull(TRIAL_CHECK_TIMEOUT_MS) {
                    (getTrialStatusUseCase(forceRefresh = true) as? UseCaseState.Success)?.data
                }
            }
            val products = getSubscriptionProductsUseCase()
            val trialStatus = trial.await()
            if (trialStatus != null) setState { copy(trialStatus = trialStatus) }
            when (val result = products) {
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

    private fun buyOrStartTrial(product: SubscriptionProduct?) {
        val s = currentState()
        if (product == null || s.busy) return
        if (s.trialDaysFor(product) > 0) requestTrial(product) else purchase(product.id)
    }

    private fun requestTrial(product: SubscriptionProduct) {
        val params = mapOf(PARAM_PRODUCT_ID to product.id)
        viewModelScope.launch {
            setState { copy(requestingTrial = true, selectedProductId = product.id) }
            val result = requestTrialUseCase()
            setState { copy(requestingTrial = false) }
            when (result) {
                is UseCaseState.Success -> onTrialResult(result.data, params)
                is UseCaseState.Error -> setEffect(
                    PaywallEffect.ShowMessage(
                        if (result.message == TrialErrors.ACCOUNT_REQUIRED) PaywallMessage.TrialRequiresAccount
                        else PaywallMessage.TrialStartFailed,
                    ),
                )
            }
        }
    }

    private fun onTrialResult(result: TrialRequestResult, params: Map<String, String>) {
        when (result.outcome) {
            TrialRequestOutcome.Started, TrialRequestOutcome.AlreadyActive, TrialRequestOutcome.PendingSignup -> {
                when (result.outcome) {
                    TrialRequestOutcome.Started -> trackEventUseCase(AnalyticsEvents.TRIAL_STARTED, params)
                    TrialRequestOutcome.PendingSignup -> trackEventUseCase(AnalyticsEvents.TRIAL_REQUESTED, params)
                    else -> Unit
                }
                setState { copy(trialStatus = result.status ?: trialStatus, showExitOffer = false) }
                setEffect(PaywallEffect.Unlocked)
            }
            TrialRequestOutcome.UsedOnDevice, TrialRequestOutcome.UsedByAccount -> {
                trackEventUseCase(AnalyticsEvents.TRIAL_REFUSED, params + (PARAM_REASON to result.outcome.name))
                // The CTA switches to a plain subscribe and the notice card explains why.
                setState { copy(trialStatus = result.status ?: trialStatus) }
                setEffect(
                    PaywallEffect.ShowMessage(
                        if (result.outcome == TrialRequestOutcome.UsedOnDevice) PaywallMessage.TrialUsedOnDevice
                        else PaywallMessage.TrialUsedByAccount,
                    ),
                )
            }
        }
    }

    private fun purchase(productId: String) {
        val s = currentState()
        val trialDays = s.products.firstOrNull { it.id == productId }?.let(s::trialDaysFor) ?: 0
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
        const val PARAM_REASON = "reason"
        const val TRIAL_CHECK_TIMEOUT_MS = 6_000L
    }
}
