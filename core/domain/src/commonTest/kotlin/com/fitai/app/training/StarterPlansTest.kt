package com.fitai.app.training

import com.fitai.app.model.UserProfilePayload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StarterPlansTest {

    private val base = UserProfilePayload(
        name = "Ada",
        age = 30,
        heightCm = 170.0,
        weightKg = 70.0,
        goal = "build_muscle",
        activityLevel = "moderate",
        equipment = "full_gym",
        dietRestrictions = "none",
        gender = "female",
        experience = "intermediate",
        daysPerWeek = 4,
        sessionMinutes = 45,
    )

    @Test
    fun usesDaysAndSessionLength() {
        val plan = StarterPlans.workout(base.copy(daysPerWeek = 3, sessionMinutes = 30), "tr")
        assertEquals(3, plan.daysPerWeek)
        assertEquals(setOf(1, 3, 5), plan.exercises.map { it.dayOfWeek }.toSet())
        assertTrue(plan.exercises.groupBy { it.dayOfWeek }.values.all { it.size == 4 })
    }

    @Test
    fun bodyweightOnlyWithoutEquipment() {
        val plan = StarterPlans.workout(base.copy(equipment = "bodyweight"), "en")
        assertTrue(plan.exercises.none { it.name.contains("Barbell") || it.name.contains("Bench press") })
    }

    @Test
    fun timeObstacleShortensRestAndSaysSo() {
        val normal = StarterPlans.workout(base, "en")
        val rushed = StarterPlans.workout(base.copy(obstacle = "time"), "en")
        assertTrue(rushed.exercises.first().restSeconds < normal.exercises.first().restSeconds)
        assertTrue(rushed.description.contains("Shorter rests"))
    }

    @Test
    fun injuryLowersVolumeAvoidsHeavyRepsAndAddsSafetyNote() {
        val strong = base.copy(goal = "get_stronger", experience = "advanced")
        val plan = StarterPlans.workout(strong.copy(obstacle = "injury"), "en")
        val normal = StarterPlans.workout(strong, "en")
        assertEquals(normal.exercises.first().sets - 1, plan.exercises.first().sets)
        assertEquals("6-8", plan.exercises.first().reps)
        assertNotNull(plan.exercises.first().notes)
        assertTrue(plan.exercises.first().notes!!.contains("pain-free"))
    }

    @Test
    fun motivationTrimsOneExercisePerSession() {
        val normal = StarterPlans.workout(base, "tr")
        val short = StarterPlans.workout(base.copy(obstacle = "motivation"), "tr")
        assertEquals(normal.exercises.size - normal.daysPerWeek, short.exercises.size)
    }

    @Test
    fun neverTriedIsTreatedAsNovice() {
        val plan = StarterPlans.workout(base.copy(daysPerWeek = 3, obstacle = "never_tried"), "en")
        val intermediate = StarterPlans.workout(base.copy(daysPerWeek = 3), "en")
        assertEquals(intermediate.exercises.first().sets - 1, plan.exercises.first().sets)
        // Novices get the full-body split on three days instead of push / pull / legs.
        assertEquals("Bench press", intermediate.exercises.first().name)
        assertEquals("Back squat", plan.exercises.first().name)
        assertTrue(plan.exercises.first().notes!!.contains("technique"))
    }

    @Test
    fun overloadFocusAddsProgressionNoteOnlyToFirstExercise() {
        val plan = StarterPlans.workout(base.copy(focusAreas = listOf("smart_overload")), "en")
        val mondays = plan.exercises.filter { it.dayOfWeek == 1 }
        assertTrue(mondays.first().notes!!.contains("2.5–5%"))
        assertTrue(mondays.drop(1).all { it.notes == null })
        assertNull(StarterPlans.workout(base, "en").exercises.first().notes)
    }

    @Test
    fun dietUsesGenderActivityGoalAndVegan() {
        val female = StarterPlans.diet(base)
        val male = StarterPlans.diet(base.copy(gender = "male"))
        assertTrue(male.dailyCalories > female.dailyCalories)
        val active = StarterPlans.diet(base.copy(activityLevel = "athlete"))
        assertTrue(active.dailyCalories > female.dailyCalories)
        val cut = StarterPlans.diet(base.copy(goal = "lose_fat"))
        assertTrue(cut.dailyCalories < female.dailyCalories)
        val vegan = StarterPlans.diet(base.copy(dietRestrictions = "vegan"))
        assertEquals((70.0 * 2.0 * 1.1).toInt(), vegan.proteinG)
        assertEquals(140, female.proteinG)
    }

    @Test
    fun dietHasGenderSpecificFloor() {
        val tiny = base.copy(weightKg = 40.0, heightCm = 140.0, age = 80, activityLevel = "sedentary", goal = "lose_fat")
        assertEquals(1200, StarterPlans.diet(tiny).dailyCalories)
        assertEquals(1500, StarterPlans.diet(tiny.copy(gender = "male")).dailyCalories)
    }
}
