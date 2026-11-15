package com.fitai.app.ui.plangeneration

import com.fitai.app.access.PlanQuota
import com.fitai.app.access.PlanQuotaErrors
import com.fitai.app.localization.Strings
import com.fitai.app.model.GeneratedWorkoutPlan
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import kotlinx.coroutines.delay

enum class PlanGenerationPhase { Running, Success, Failed }

/** What [PlanGenerationOverlay] shows. Owned by the screen's ViewModel. */
data class PlanGenerationUiState(
    val phase: PlanGenerationPhase = PlanGenerationPhase.Running,
    /** Weekdays (1 = Monday) of the new plan; lit in the week grid once it is ready. */
    val trainingDays: Set<Int> = emptySet(),
    val planTitle: String? = null,
    /** Rights left after this run; null when the run does not use the quota (first plan). */
    val quota: PlanQuota? = null,
    val errorCode: String? = null,
    val canRetry: Boolean = false,
    /** True when a failed run would have spent a right, so the overlay can say it did not. */
    val spendsRight: Boolean = true,
)

object PlanGenerationTiming {
    /** Long enough to read the three steps and see the week deal when the dev mock answers at once. */
    const val MIN_VISIBLE_MS = 4_500L
}

/** Runs [block] and waits until at least [minMillis] passed, so a fast answer does not flash the overlay. */
suspend fun <T> withMinimumDuration(minMillis: Long = PlanGenerationTiming.MIN_VISIBLE_MS, block: suspend () -> T): T {
    val start = TimeSource.Monotonic.markNow()
    val result = block()
    val remaining = minMillis.milliseconds - start.elapsedNow()
    if (remaining.isPositive()) delay(remaining)
    return result
}

fun GeneratedWorkoutPlan.trainingDays(): Set<Int> = exercises.map { it.dayOfWeek }.filter { it in 1..7 }.toSet()

/** User-facing text for a failed plan generation. */
fun planGenerationErrorText(code: String?, strings: Strings): String = when (code) {
    PlanQuotaErrors.EXHAUSTED -> strings.planQuotaErrorExhausted
    PlanQuotaErrors.IN_PROGRESS -> strings.planQuotaErrorInProgress
    PlanQuotaErrors.INITIAL_LIMIT -> strings.planQuotaErrorInitialLimit
    else -> strings.planGenFailedBody
}
