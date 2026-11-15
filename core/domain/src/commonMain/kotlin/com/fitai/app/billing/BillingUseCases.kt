package com.fitai.app.billing

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.toUseCaseState

class GetSubscriptionProductsUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(): UseCaseState<List<SubscriptionProduct>> =
        billingRepository.getProducts().toUseCaseState()
}

class PurchaseSubscriptionUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(productId: String): UseCaseState<Unit> =
        billingRepository.purchase(productId).toUseCaseState()
}

class RestorePurchasesUseCase(
    private val billingRepository: BillingRepository,
) {
    suspend operator fun invoke(): UseCaseState<Boolean> =
        billingRepository.restore().toUseCaseState()
}
