package com.fitai.app.engagement

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object Xp {
    const val SESSION = 100
    const val PERSONAL_RECORD = 50
    const val PROTEIN_GOAL_DAY = 30
    const val MEAL_PHOTO = 20
    const val LEVEL_STEP = 250

    /** XP from [facts] whose event date passes [include]. */
    fun points(facts: EngagementFacts, include: (LocalDate) -> Boolean = { true }): Int =
        facts.sessionDates.count(include) * SESSION +
            facts.prDates.count(include) * PERSONAL_RECORD +
            facts.proteinGoalDates.count(include) * PROTEIN_GOAL_DAY +
            facts.photoMealDates.count(include) * MEAL_PHOTO

    /** Total XP needed to reach [level]; level n + 1 needs LEVEL_STEP × n(n + 1) / 2. */
    fun thresholdFor(level: Int): Int = LEVEL_STEP * (level - 1) * level / 2

    fun levelFor(totalXp: Int): LevelProgress {
        val xp = totalXp.coerceAtLeast(0)
        var level = 1
        while (xp >= thresholdFor(level + 1)) level++
        val floor = thresholdFor(level)
        return LevelProgress(
            level = level,
            totalXp = xp,
            xpIntoLevel = xp - floor,
            xpForNextLevel = thresholdFor(level + 1) - floor,
        )
    }
}

object Badges {
    const val STREAK_SHORT = 7
    const val STREAK_LONG = 28
    const val SESSIONS = 10
    const val MEALS = 100
    const val VOLUME_KG = 10_000.0

    val streakMilestones = listOf(STREAK_SHORT, STREAK_LONG)

    fun compute(facts: EngagementFacts, streakMilestoneDates: Map<Int, LocalDate>): List<Badge> =
        BadgeId.entries.map { id ->
            val unlockedAt = when (id) {
                BadgeId.FirstSession -> facts.sessionDates.nth(1)
                BadgeId.FirstMealPhoto -> facts.photoMealDates.nth(1)
                BadgeId.FirstWeighIn -> facts.weighInDates.nth(1)
                BadgeId.FirstPr -> facts.prDates.nth(1)
                BadgeId.Streak7 -> streakMilestoneDates[STREAK_SHORT]
                BadgeId.Streak28 -> streakMilestoneDates[STREAK_LONG]
                BadgeId.Sessions10 -> facts.sessionDates.nth(SESSIONS)
                BadgeId.Meals100 -> facts.mealDates.nth(MEALS)
                BadgeId.Volume10t -> facts.dateVolumeReached(VOLUME_KG)
            }
            Badge(id = id, unlocked = unlockedAt != null, unlockedAt = unlockedAt)
        }

    private fun List<LocalDate>.nth(n: Int): LocalDate? = sorted().getOrNull(n - 1)
}

object Day0 {
    const val VISIBLE_DAYS = 7

    /** Hidden once all three are done or [VISIBLE_DAYS] days after [firstLaunch]. */
    fun checklist(facts: EngagementFacts, firstLaunch: LocalDate?, today: LocalDate): Day0Checklist {
        val withinWindow = firstLaunch == null || today < firstLaunch.plus(DatePeriod(days = VISIBLE_DAYS))
        val partial = Day0Checklist(
            firstSessionDone = facts.sessionDates.isNotEmpty(),
            firstMealPhotoDone = facts.photoMealDates.isNotEmpty(),
            firstWeighInDone = facts.weighInDates.isNotEmpty(),
            visible = false,
        )
        return partial.copy(visible = withinWindow && !partial.allDone)
    }
}
