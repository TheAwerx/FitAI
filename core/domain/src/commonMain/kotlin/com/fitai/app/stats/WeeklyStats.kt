package com.fitai.app.stats

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.NutritionRepository
import com.fitai.app.repository.ProgressRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.training.PersonalRecords
import com.fitai.app.util.currentIsoWeekKey
import com.fitai.app.util.isoTimestampToLocalDate
import com.fitai.app.util.weekRangeIso
import kotlin.math.roundToInt
import kotlinx.serialization.Serializable

/** Part of the `weeklyCoachReport` wire format. */
@Serializable
data class WeeklyStats(
    val weekKey: String,
    val sessionsCompleted: Int,
    val sessionsPlanned: Int,
    val totalVolumeKg: Double,
    val previousWeekVolumeKg: Double,
    val avgProteinG: Double,
    val proteinTargetG: Int,
    val avgCalories: Int,
    val calorieTarget: Int,
    val mealsLogged: Int,
    val weightChangeKg: Double? = null,
    val prCount: Int = 0,
)

/** Averages are per day with at least one logged meal. */
class ComputeWeeklyStatsUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
    private val nutritionRepository: NutritionRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(weekOffset: Int = 0): UseCaseState<WeeklyStats> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val (start, end) = weekRangeIso(weekOffset)
        val (previousStart, previousEnd) = weekRangeIso(weekOffset - 1)

        val completedSessions = workoutRepository.getCompletedSessionsInRange(user.id, start, end)
        val sessionsCompleted = completedSessions.size
        val prCount = if (completedSessions.isEmpty()) {
            0
        } else {
            PersonalRecords.countForSessions(workoutRepository.getCompletedSetHistory(user.id), completedSessions.map { it.id })
        }
        val sessionsPlanned = workoutRepository.getActivePlan(user.id)
            ?.let { plan -> workoutRepository.getExercisesForPlan(plan.id).map { it.dayOfWeek }.distinct().size }
            ?: 0
        val volume = workoutRepository.getCompletedVolumeInRange(user.id, start, end)
        val previousVolume = workoutRepository.getCompletedVolumeInRange(user.id, previousStart, previousEnd)

        val meals = nutritionRepository.getMealsInRange(user.id, start, end)
        val loggedDays = meals.mapNotNull { isoTimestampToLocalDate(it.loggedAt) }.distinct().size.coerceAtLeast(1)
        val diet = nutritionRepository.getActiveDiet(user.id)

        val weights = progressRepository.getMeasurementsInRange(user.id, start, end).sortedBy { it.measuredAt }
        val weightChange = if (weights.size >= 2) (weights.last().weightKg - weights.first().weightKg).roundTo1() else null

        WeeklyStats(
            weekKey = currentIsoWeekKey(weekOffset),
            sessionsCompleted = sessionsCompleted,
            sessionsPlanned = sessionsPlanned,
            totalVolumeKg = volume.roundTo1(),
            previousWeekVolumeKg = previousVolume.roundTo1(),
            avgProteinG = (meals.sumOf { it.proteinG } / loggedDays).roundTo1(),
            proteinTargetG = diet?.proteinG ?: 0,
            avgCalories = meals.sumOf { it.calories } / loggedDays,
            calorieTarget = diet?.dailyCalories ?: 0,
            mealsLogged = meals.size,
            weightChangeKg = weightChange,
            prCount = prCount,
        )
    }

    private fun Double.roundTo1(): Double = (this * 10).roundToInt() / 10.0
}
