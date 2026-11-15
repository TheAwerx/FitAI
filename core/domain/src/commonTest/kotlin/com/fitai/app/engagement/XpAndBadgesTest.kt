package com.fitai.app.engagement

import com.fitai.app.model.BodyMeasurement
import com.fitai.app.model.CompletedSetRecord
import com.fitai.app.model.DietPlan
import com.fitai.app.model.MealLog
import com.fitai.app.model.WorkoutSession
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XpAndBadgesTest {

    private fun nov(day: Int) = LocalDate(2026, 11, day)
    private fun utcDate(iso: String): LocalDate? = runCatching { LocalDate.parse(iso.substring(0, 10)) }.getOrNull()

    private fun session(id: String, day: Int) =
        WorkoutSession(id, "p1", "2026-11-0${day}T10:00:00Z", "2026-11-0${day}T11:00:00Z", 60, null)

    private fun set(sessionId: String, day: Int, name: String, weight: Double, reps: String) = CompletedSetRecord(
        sessionId = sessionId,
        sessionStartedAt = "2026-11-0${day}T10:00:00Z",
        sessionExerciseId = "$sessionId-$name",
        exerciseName = name,
        muscleGroup = "chest",
        setNumber = 1,
        targetReps = reps,
        actualReps = reps,
        weightKg = weight,
        completedAt = "2026-11-0${day}T10:30:00Z",
    )

    private fun meal(day: Int, protein: Double, photo: Boolean = false) = MealLog(
        id = "m$day$protein",
        userId = "u",
        mealName = "meal",
        calories = 500,
        proteinG = protein,
        carbsG = 50.0,
        fatG = 10.0,
        photoUrl = if (photo) MealLog.ANALYZED_PHOTO_MARKER else null,
        loggedAt = "2026-11-0${day}T12:00:00Z",
    )

    private val diet = DietPlan("d", "u", 2200, 120, 250, 70, "2026-11-01T00:00:00Z", true)

    private fun facts() = EngagementFacts.from(
        completedSessions = listOf(session("s1", 2), session("s2", 4)),
        setHistory = listOf(
            set("s1", 2, "Bench", 60.0, "10"),
            set("s2", 4, "Bench", 65.0, "10"),
            set("s2", 4, "Squat", 80.0, "5"),
        ),
        meals = listOf(meal(2, 70.0, photo = true), meal(2, 60.0), meal(3, 40.0)),
        measurements = emptyList(),
        diet = diet,
        trainingDaysOfWeek = setOf(1, 3, 5),
        toDate = ::utcDate,
    )

    @Test
    fun factsDeriveSessionsPrsProteinAndPhotos() {
        val facts = facts()
        assertEquals(listOf(nov(2), nov(4)), facts.sessionDates.sorted())
        assertEquals(listOf(nov(4)), facts.prDates)
        assertEquals(setOf(nov(2)), facts.proteinGoalDates)
        assertEquals(listOf(nov(2)), facts.photoMealDates)
        assertEquals(setOf(nov(2), nov(3), nov(4)), facts.activeDates)
        assertEquals(600.0 + 650.0 + 400.0, facts.volumeByDate.values.sum())
    }

    @Test
    fun xpAddsEventPoints() {
        val facts = facts()
        assertEquals(2 * 100 + 50 + 30 + 20, Xp.points(facts))
        assertEquals(100 + 50, Xp.points(facts) { it == nov(4) })
    }

    @Test
    fun levelThresholds() {
        assertEquals(0, Xp.thresholdFor(1))
        assertEquals(250, Xp.thresholdFor(2))
        assertEquals(750, Xp.thresholdFor(3))
        assertEquals(1, Xp.levelFor(249).level)
        val level2 = Xp.levelFor(500)
        assertEquals(2, level2.level)
        assertEquals(250, level2.xpIntoLevel)
        assertEquals(500, level2.xpForNextLevel)
        assertEquals(0.5f, level2.progress)
        assertEquals(3, Xp.levelFor(750).level)
    }

    @Test
    fun badgesUnlockWithDates() {
        val badges = Badges.compute(facts(), mapOf(7 to nov(8))).associateBy { it.id }
        assertEquals(nov(2), badges.getValue(BadgeId.FirstSession).unlockedAt)
        assertEquals(nov(2), badges.getValue(BadgeId.FirstMealPhoto).unlockedAt)
        assertEquals(nov(4), badges.getValue(BadgeId.FirstPr).unlockedAt)
        assertEquals(nov(8), badges.getValue(BadgeId.Streak7).unlockedAt)
        assertFalse(badges.getValue(BadgeId.FirstWeighIn).unlocked)
        assertFalse(badges.getValue(BadgeId.Streak28).unlocked)
        assertFalse(badges.getValue(BadgeId.Sessions10).unlocked)
        assertFalse(badges.getValue(BadgeId.Volume10t).unlocked)
        assertNull(badges.getValue(BadgeId.Meals100).unlockedAt)
    }

    @Test
    fun day0ChecklistVisibility() {
        val facts = facts()
        val open = Day0.checklist(facts, firstLaunch = nov(1), today = nov(5))
        assertTrue(open.visible)
        assertTrue(open.firstSessionDone && open.firstMealPhotoDone && !open.firstWeighInDone)

        assertFalse(Day0.checklist(facts, firstLaunch = nov(1), today = nov(8)).visible)

        val withWeighIn = facts.copy(weighInDates = listOf(nov(3)))
        assertFalse(Day0.checklist(withWeighIn, firstLaunch = nov(1), today = nov(2)).visible)
    }

    @Test
    fun weighInFactsUseMeasurementDates() {
        val facts = EngagementFacts.from(
            completedSessions = emptyList(),
            setHistory = emptyList(),
            meals = emptyList(),
            measurements = listOf(BodyMeasurement("b", "u", 80.0, measuredAt = "2026-11-03T08:00:00Z")),
            diet = null,
            trainingDaysOfWeek = emptySet(),
            toDate = ::utcDate,
        )
        assertEquals(listOf(nov(3)), facts.weighInDates)
        assertTrue(facts.proteinGoalDates.isEmpty())
    }
}
