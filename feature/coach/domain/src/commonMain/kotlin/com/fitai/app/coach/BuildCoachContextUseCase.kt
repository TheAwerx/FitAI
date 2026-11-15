package com.fitai.app.coach

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.ProgressRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.training.PersonalRecords
import com.fitai.app.util.isoTimestampToLocalDate
import com.fitai.app.util.todayLocalDate
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Compact plain-text summary of the last [CoachLimits.CONTEXT_DAYS] days for the coach prompt.
 * Written in English for the model; it is never shown in the UI.
 */
class BuildCoachContextUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(): UseCaseState<String> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val days = CoachLimits.CONTEXT_DAYS
        val half = days / 2
        val today = todayLocalDate()
        val end = today.plus(DatePeriod(days = 1)).isoStart()
        val start = today.minus(DatePeriod(days = days - 1)).isoStart()
        val recentStart = today.minus(DatePeriod(days = half - 1)).isoStart()

        val lines = mutableListOf<String>()
        lines += "Goal: ${user.goal}."

        val plan = workoutRepository.getActivePlan(user.id)?.takeUnless { it.isAdHoc }
        lines += if (plan != null) "Plan: ${plan.title}, ${plan.daysPerWeek} days/week." else "Plan: none."

        val sessions = workoutRepository.getCompletedSessionsInRange(user.id, start, end)
        val recentSessions = sessions.count { it.startedAt >= recentStart }
        lines += "Sessions last $days days: ${sessions.size} (last $half days: $recentSessions, " +
            "previous $half days: ${sessions.size - recentSessions})."

        val recentVolume = workoutRepository.getCompletedVolumeInRange(user.id, recentStart, end)
        val previousVolume = workoutRepository.getCompletedVolumeInRange(user.id, start, recentStart)
        val trend = if (previousVolume > 0.0) {
            val percent = ((recentVolume - previousVolume) / previousVolume * 100).roundToInt()
            " (${percent.signed()}%)"
        } else {
            ""
        }
        lines += "Volume last $half days: ${recentVolume.roundToInt()} kg vs previous $half days: " +
            "${previousVolume.roundToInt()} kg$trend."

        val history = workoutRepository.getCompletedSetHistory(user.id)
        if (history.isNotEmpty()) {
            val recentPrs = PersonalRecords.countForSessions(history, sessions.map { it.id })
            val top = PersonalRecords.bestPerExercise(history).take(TOP_PR_COUNT).joinToString("; ") {
                "${it.exerciseName.take(MAX_EXERCISE_NAME)} ${it.weightKg.oneDecimal()} kg x${it.reps} " +
                    "(e1RM ${it.e1rm.roundToInt()} kg)"
            }
            lines += "PRs last $days days: $recentPrs. Best lifts: $top."
        }

        val diet = nutritionRepository.getActiveDiet(user.id)
        val meals = nutritionRepository.getMealsInRange(user.id, start, end)
        if (meals.isEmpty()) {
            lines += "Nutrition: no meals logged."
        } else {
            val loggedDays = meals.mapNotNull { isoTimestampToLocalDate(it.loggedAt) }.distinct().size.coerceAtLeast(1)
            val calories = meals.sumOf { it.calories } / loggedDays
            val protein = (meals.sumOf { it.proteinG } / loggedDays).roundToInt()
            val calorieTarget = diet?.dailyCalories?.let { " (target $it)" }.orEmpty()
            val proteinTarget = diet?.proteinG?.let { " (target $it g)" }.orEmpty()
            lines += "Nutrition ($loggedDays/$days days logged): avg $calories kcal$calorieTarget, " +
                "protein $protein g$proteinTarget."
        }

        val latest = progressRepository.getLatestMeasurement(user.id)
        lines += if (latest == null) {
            "Weight: ${user.weightKg.oneDecimal()} kg (profile, not logged)."
        } else {
            val inRange = progressRepository.getMeasurementsInRange(user.id, start, end)
            val change = if (inRange.size >= 2) {
                val sorted = inRange.sortedBy { it.measuredAt }
                ", change ${(sorted.last().weightKg - sorted.first().weightKg).oneDecimalSigned()} kg in $days days"
            } else {
                ""
            }
            val date = isoTimestampToLocalDate(latest.measuredAt)?.let { " on $it" }.orEmpty()
            "Weight: latest ${latest.weightKg.oneDecimal()} kg$date$change."
        }

        lines.joinToString("\n").take(CoachLimits.MAX_CONTEXT_CHARS)
    }

    private fun LocalDate.isoStart(): String = atStartOfDayIn(TimeZone.currentSystemDefault()).toString()

    private companion object {
        const val TOP_PR_COUNT = 3
        const val MAX_EXERCISE_NAME = 32
    }
}

private fun Int.signed(): String = if (this > 0) "+$this" else toString()

private fun Double.oneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    val sign = if (tenths < 0) "-" else ""
    return "$sign${abs(tenths) / 10}.${abs(tenths) % 10}"
}

private fun Double.oneDecimalSigned(): String = if (this > 0.0) "+${oneDecimal()}" else oneDecimal()
