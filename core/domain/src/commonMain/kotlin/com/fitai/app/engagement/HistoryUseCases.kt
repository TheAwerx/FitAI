package com.fitai.app.engagement

import com.fitai.app.domain.UseCaseState
import com.fitai.app.domain.useCaseCatching
import com.fitai.app.repository.LocalUserRepository
import com.fitai.app.repository.ProgressRepository
import com.fitai.app.repository.WorkoutRepository
import com.fitai.app.training.PersonalRecords
import com.fitai.app.training.repsDone
import com.fitai.app.util.isoTimestampToLocalDate
import com.fitai.app.util.todayLocalDate
import com.fitai.app.util.weekStart
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class WorkoutHistoryEntry(
    val sessionId: String,
    val date: LocalDate,
    val muscleGroups: List<String>,
    val planTitle: String?,
    val durationMinutes: Int,
    val volumeKg: Double,
    val completedSets: Int,
)

data class WorkoutHistory(
    /** Newest first. */
    val entries: List<WorkoutHistoryEntry>,
    val personalRecordCount: Int,
)

enum class WorkoutTrendMetric { Volume, Duration, Sessions }

data class TrendBucket(val weekStart: LocalDate, val value: Double)

/** One bucket per ISO week, oldest first, ending with the current week. */
fun WorkoutHistory.weeklyTrend(metric: WorkoutTrendMetric, weeks: Int, today: LocalDate = todayLocalDate()): List<TrendBucket> {
    val currentMonday = weekStart(today)
    val byWeek = entries.groupBy { weekStart(it.date) }
    return (weeks - 1 downTo 0).map { back ->
        val monday = currentMonday.minus(DatePeriod(days = 7 * back))
        val inWeek = byWeek[monday].orEmpty()
        val value = when (metric) {
            WorkoutTrendMetric.Volume -> inWeek.sumOf { it.volumeKg }
            WorkoutTrendMetric.Duration -> inWeek.sumOf { it.durationMinutes }.toDouble()
            WorkoutTrendMetric.Sessions -> inWeek.size.toDouble()
        }
        TrendBucket(monday, value)
    }
}

class GetWorkoutHistoryUseCase(
    private val localUserRepository: LocalUserRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend operator fun invoke(): UseCaseState<WorkoutHistory> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val end = todayLocalDate().plus(DatePeriod(days = 1)).atStartOfDayIn(TimeZone.currentSystemDefault()).toString()
        val sessions = workoutRepository.getCompletedSessionsInRange(user.id, "1970-01-01T00:00:00Z", end)
        val sets = workoutRepository.getCompletedSetHistory(user.id)
        val setsBySession = sets.groupBy { it.sessionId }
        val planTitles = mutableMapOf<String, String?>()
        val entries = sessions.mapNotNull { session ->
            val date = isoTimestampToLocalDate(session.completedAt ?: session.startedAt) ?: return@mapNotNull null
            val sessionSets = setsBySession[session.id].orEmpty()
            val title = planTitles.getOrPut(session.planId) { workoutRepository.getPlanById(session.planId)?.title }
            WorkoutHistoryEntry(
                sessionId = session.id,
                date = date,
                muscleGroups = sessionSets.map { it.muscleGroup.trim() }.filter { it.isNotEmpty() }.distinct(),
                planTitle = title,
                durationMinutes = session.durationMinutes ?: 0,
                volumeKg = sessionSets.sumOf { (it.weightKg ?: 0.0) * (it.repsDone() ?: 0) },
                completedSets = sessionSets.size,
            )
        }.sortedByDescending { it.date }
        WorkoutHistory(
            entries = entries,
            personalRecordCount = PersonalRecords.bestPerExercise(sets).size,
        )
    }
}

data class MeasurementEntry(val date: LocalDate, val weightKg: Double, val waistCm: Double?)

data class MeasurementHistory(
    /** Weight given in onboarding; the baseline for "change since start". */
    val startWeightKg: Double,
    val startDate: LocalDate?,
    /** Oldest first, last entry of each day. */
    val entries: List<MeasurementEntry>,
) {
    val latestWeightKg: Double get() = entries.lastOrNull()?.weightKg ?: startWeightKg
    val changeSinceStartKg: Double get() = latestWeightKg - startWeightKg
}

class GetMeasurementHistoryUseCase(
    private val localUserRepository: LocalUserRepository,
    private val progressRepository: ProgressRepository,
) {
    suspend operator fun invoke(): UseCaseState<MeasurementHistory> = useCaseCatching {
        val user = localUserRepository.getLatestUser() ?: error("no_user")
        val entries = progressRepository.getMeasurementsForUser(user.id)
            .sortedBy { it.measuredAt }
            .mapNotNull { m -> isoTimestampToLocalDate(m.measuredAt)?.let { MeasurementEntry(it, m.weightKg, m.waistCm) } }
            .associateBy { it.date }
            .values
            .toList()
        MeasurementHistory(
            startWeightKg = user.weightKg,
            startDate = isoTimestampToLocalDate(user.createdAt),
            entries = entries,
        )
    }
}
