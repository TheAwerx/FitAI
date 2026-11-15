package com.fitai.app.engagement

import com.fitai.app.training.PersonalRecord
import kotlinx.datetime.LocalDate

/** [longestDays] only covers the [Streaks.LOOKBACK_DAYS] window; null when no streak exists there. */
data class StreakStatus(
    val currentDays: Int,
    val longestDays: Int?,
    val todayActive: Boolean,
    val freezeAvailableThisWeek: Boolean,
    val freezeUsedThisWeek: Boolean,
)

data class LevelProgress(
    val level: Int,
    val totalXp: Int,
    /** XP earned inside the current level. */
    val xpIntoLevel: Int,
    /** XP needed to go from the current level to the next one. */
    val xpForNextLevel: Int,
) {
    val progress: Float get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel).coerceIn(0f, 1f)
}

data class Day0Checklist(
    val firstSessionDone: Boolean,
    val firstMealPhotoDone: Boolean,
    val firstWeighInDone: Boolean,
    val visible: Boolean,
) {
    val completedCount: Int get() = listOf(firstSessionDone, firstMealPhotoDone, firstWeighInDone).count { it }
    val allDone: Boolean get() = completedCount == 3
}

data class EngagementSummary(
    val streak: StreakStatus,
    val weeklyXp: Int,
    val level: LevelProgress,
    val checklist: Day0Checklist,
)

enum class BadgeId(val key: String) {
    FirstSession("first_session"),
    FirstMealPhoto("first_meal_photo"),
    FirstWeighIn("first_weigh_in"),
    FirstPr("first_pr"),
    Streak7("streak_7"),
    Streak28("streak_28"),
    Sessions10("sessions_10"),
    Meals100("meals_100"),
    Volume10t("volume_10t"),
}

data class Badge(
    val id: BadgeId,
    val unlocked: Boolean,
    val unlockedAt: LocalDate? = null,
)

/** One day of the current ISO week (Monday first) in the workout streak strip. */
data class StreakDay(
    val date: LocalDate,
    val trained: Boolean,
    val isToday: Boolean,
    val isTrainingDay: Boolean,
)

/** Streak counted on completed workouts only; planned rest days keep it alive (see [Streaks]). */
data class WorkoutStreak(
    val currentDays: Int,
    val longestDays: Int?,
    val trainedToday: Boolean,
    val totalSessions: Int,
    val week: List<StreakDay>,
)

enum class StarterTaskId { Notifications, FirstWorkout, FirstMealPhoto, FirstWeighIn }

data class StarterTask(val id: StarterTaskId, val done: Boolean)

/** First-week goals behind the floating goals button; the gift unlocks when every task is done. */
data class StarterChallenge(
    val tasks: List<StarterTask>,
    val visible: Boolean,
) {
    val completedCount: Int get() = tasks.count { it.done }
    val progress: Float get() = if (tasks.isEmpty()) 0f else completedCount.toFloat() / tasks.size
    val allDone: Boolean get() = tasks.all { it.done }
    val nextTask: StarterTask? get() = tasks.firstOrNull { !it.done }
}

data class WeightPoint(val date: LocalDate, val weightKg: Double)

data class WeeklyVolume(val weekKey: String, val weekStart: LocalDate, val volumeKg: Double)

data class ProgressOverview(
    /** Oldest first, one point per day (last measurement of the day). */
    val weightPoints: List<WeightPoint>,
    val latestWeightKg: Double?,
    /** Oldest to newest week, current week last. */
    val weeklyVolumes: List<WeeklyVolume>,
    val personalRecords: List<PersonalRecord>,
    val badges: List<Badge>,
) {
    /** Change between the first and last point of [weightPoints]; null with fewer than two points. */
    val weightChangeKg: Double?
        get() = if (weightPoints.size >= 2) weightPoints.last().weightKg - weightPoints.first().weightKg else null
}
