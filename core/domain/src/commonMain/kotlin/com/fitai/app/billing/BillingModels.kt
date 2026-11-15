package com.fitai.app.billing

enum class SubscriptionPeriod { Monthly, Yearly }

data class SubscriptionProduct(
    val id: String,
    val period: SubscriptionPeriod,
    val priceLabel: String,
    val pricePerMonthLabel: String,
    val trialDays: Int,
    val savingsPercent: Int?,
    /** Price of the same period on the monthly plan, shown struck through; null when there is no discount. */
    val originalPriceLabel: String? = null,
    val pricePerWeekLabel: String = pricePerMonthLabel,
)

interface BillingRepository {
    suspend fun getProducts(): Result<List<SubscriptionProduct>>
    suspend fun purchase(productId: String): Result<Unit>
    /** Success(true) when an active subscription was found and restored. */
    suspend fun restore(): Result<Boolean>
}
