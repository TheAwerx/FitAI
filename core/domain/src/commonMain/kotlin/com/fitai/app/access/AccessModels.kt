package com.fitai.app.access

enum class AccessTier { Free, Trial, Pro }

enum class QuotaPeriod { Week, Month }

enum class GatedFeature(val freeQuota: Int, val period: QuotaPeriod) {
    MealPhotoAnalysis(3, QuotaPeriod.Week),
    TodayMode(1, QuotaPeriod.Week),
    PlanRegeneration(1, QuotaPeriod.Month),
    NextMealSuggestion(0, QuotaPeriod.Week),
    CoachChat(0, QuotaPeriod.Week),
    WeeklyReportFull(0, QuotaPeriod.Week),
}

/** [remaining] is null when the tier is unlimited (Trial, Pro). */
data class FeatureAccess(
    val feature: GatedFeature,
    val tier: AccessTier,
    val allowed: Boolean,
    val remaining: Int?,
)

interface EntitlementRepository {
    suspend fun currentTier(): AccessTier
}

interface UsageLimitRepository {
    suspend fun usedInCurrentPeriod(feature: GatedFeature): Int
    suspend fun recordUse(feature: GatedFeature)
}
