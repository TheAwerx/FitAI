package com.fitai.app.nutrition

import com.fitai.app.model.DietPlan
import com.fitai.app.model.DietRestriction
import com.fitai.app.model.FoodItem
import com.fitai.app.model.FoodServing
import com.fitai.app.model.FoodTags
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.MealLog
import com.fitai.app.model.MealSlot
import com.fitai.app.model.PlanFood
import com.fitai.app.model.PlanMeal
import com.fitai.app.model.PlanMealOption
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DietPlanMathTest {

    private val bread = FoodItem(
        id = "bread_whole_wheat", nameTr = "Tam buğday ekmeği", nameEn = "Whole wheat bread", category = "bread",
        kcalPer100g = 252.0, proteinPer100g = 12.5, carbsPer100g = 42.7, fatPer100g = 3.5,
        servings = listOf(FoodServing(FoodUnits.SLICE, 25.0)), tags = setOf(FoodTags.GLUTEN),
    )
    private val cheese = FoodItem(
        id = "cheese_white", nameTr = "Beyaz peynir", nameEn = "White cheese", category = "cheese",
        kcalPer100g = 264.0, proteinPer100g = 15.0, carbsPer100g = 2.5, fatPer100g = 21.5,
        servings = listOf(FoodServing(FoodUnits.SLICE, 30.0)), tags = setOf(FoodTags.DAIRY),
    )

    private fun near(expected: Double, actual: Double, tolerance: Double = 0.06) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")

    @Test
    fun servingMathUsesTheUnitWeight() {
        val twoSlices = bread.macrosFor(FoodUnits.SLICE, 2.0)
        near(126.0, twoSlices.calories)
        near(6.25, twoSlices.proteinG)
        near(21.35, twoSlices.carbsG)
        near(1.75, twoSlices.fatG)
        // Grams are always available; an unknown unit counts as grams.
        near(252.0, bread.macrosFor(FoodUnits.GRAM, 100.0).calories)
        assertEquals(listOf(FoodUnits.SLICE, FoodUnits.GRAM), bread.pickerServings.map { it.unit })
    }

    @Test
    fun entryForRoundsAndKeepsTheServing() {
        val entry = cheese.entryFor(FoodUnits.SLICE, 1.5, MealSlot.Breakfast.code, "en")
        assertEquals("White cheese", entry.name)
        assertEquals(119, entry.calories) // 45 g x 2.64
        assertEquals(6.8, entry.proteinG)
        assertEquals(45.0, entry.grams)
        assertEquals(FoodUnits.SLICE, entry.servingUnit)
        assertEquals(1.5, entry.servingAmount)
        assertEquals("breakfast", entry.slot)
    }

    @Test
    fun editingAnEntryRecomputesFromTheFoodOrScalesByWeight() {
        val logged = MealLog(
            "m1", "u", "Beyaz peynir", 79, 4.5, 0.8, 6.5, null, "2026-11-15T07:00:00Z",
            slot = "breakfast", foodId = "cheese_white", servingUnit = FoodUnits.SLICE, servingAmount = 1.0, grams = 30.0,
        )
        val fromFood = logged.withAmount(cheese, FoodUnits.GRAM, 60.0, "lunch")
        assertEquals(158, fromFood.calories)
        assertEquals(60.0, fromFood.grams)
        assertEquals(FoodUnits.GRAM, fromFood.servingUnit)
        assertEquals("lunch", fromFood.slot)
        assertEquals("Beyaz peynir", fromFood.mealName)

        // An AI plan food that is not in the database: 2 slices -> 3 slices scales everything by 1.5.
        val aiFood = logged.copy(foodId = null, servingAmount = 2.0, grams = 60.0, calories = 160, proteinG = 9.0)
        val scaled = aiFood.withAmount(null, FoodUnits.SLICE, 3.0)
        assertEquals(240, scaled.calories)
        assertEquals(13.5, scaled.proteinG)
        assertEquals(90.0, scaled.grams)
    }

    @Test
    fun restrictionsExcludeTaggedFoods() {
        assertFalse(cheese.isAllowedFor(setOf(DietRestriction.Vegan.code)))
        assertFalse(cheese.isAllowedFor(setOf(DietRestriction.DairyFree.code)))
        assertTrue(cheese.isAllowedFor(setOf(DietRestriction.Vegetarian.code, DietRestriction.GlutenFree.code)))
        assertFalse(bread.isAllowedFor(setOf(DietRestriction.GlutenFree.code)))
        assertTrue(bread.isAllowedFor(setOf(DietRestriction.Vegan.code, DietRestriction.Halal.code)))
    }

    private fun option(title: String, kcal: Int, protein: Double) = PlanMealOption(
        title = title,
        foods = listOf(PlanFood("bread_whole_wheat", "Ekmek", 2.0, FoodUnits.SLICE, 50.0, kcal, protein, 20.0, 2.0)),
    )

    private val meals = listOf(
        PlanMeal("breakfast", "08:00", listOf(option("A", 400, 20.0), option("B", 420, 25.0)), selected = 0),
        PlanMeal("lunch", "13:00", listOf(option("C", 600, 40.0), option("D", 580, 35.0), option("E", 610, 30.0)), selected = 1),
    )

    @Test
    fun planTotalsAddTheSelectedOptions() {
        val totals = meals.plannedTotals()
        assertEquals(980.0, totals.calories)
        assertEquals(55.0, totals.proteinG)
    }

    @Test
    fun swappingAnAlternativeChangesOnlyThatMeal() {
        val swapped = meals.selectOption("breakfast", 1)
        assertEquals(1, swapped[0].selected)
        assertEquals("B", swapped[0].current?.title)
        assertEquals(1000.0, swapped.plannedTotals().calories)
        assertEquals(listOf(0), swapped[0].alternatives.map { it.index })
        assertEquals(listOf(0, 2), swapped[1].alternatives.map { it.index })
        // Unknown slots or indexes change nothing.
        assertEquals(meals, meals.selectOption("dinner", 0))
        assertEquals(meals, meals.selectOption("lunch", 7))
    }

    @Test
    fun ateAsPlannedLogsEveryFoodOfTheOption() {
        val entries = meals[1].toEntries()
        assertEquals(1, entries.size)
        assertEquals(580, entries[0].calories)
        assertEquals("lunch", entries[0].slot)
        assertEquals(FoodUnits.SLICE, entries[0].servingUnit)
        assertEquals(2.0, entries[0].servingAmount)
        assertEquals(50.0, entries[0].grams)
        assertEquals(610, meals[1].toEntries(2)[0].calories)
        assertTrue(meals[1].toEntries(9).isEmpty())
    }

    @Test
    fun diaryFollowsThePlanAndPlacesUnslottedEntriesByTime() {
        val plan = DietPlan("d", "u", 2000, 120, 220, 70, "2026-11-01T00:00:00Z", true, meals)
        fun log(id: String, slot: String?) =
            MealLog(id, "u", id, 100, 1.0, 1.0, 1.0, null, "2026-11-15T00:${id.padStart(2, '0')}:00Z", slot = slot)
        val minutes = mapOf("1" to 8 * 60 + 30, "2" to 12 * 60 + 50, "3" to 22 * 60, "4" to 9 * 60)
        val todays = listOf(log("1", null), log("2", null), log("3", "evening_snack"), log("4", "lunch"))
        val diary = buildDiary(plan, todays) { minutes[it.id] }
        assertEquals(listOf(MealSlot.Breakfast, MealSlot.Lunch, MealSlot.EveningSnack), diary.map { it.slot })
        assertEquals(listOf("1"), diary[0].entries.map { it.id })
        assertEquals(setOf("2", "4"), diary[1].entries.map { it.id }.toSet())
        assertEquals(listOf("3"), diary[2].entries.map { it.id })
        assertNull(diary[2].plan)
        assertEquals("D", diary[1].plan?.current?.title)
        assertEquals(200.0, diary[1].consumed.calories)
    }

    @Test
    fun diaryWithoutAPlanUsesTheDefaultMeals() {
        val diary = buildDiary(null, emptyList()) { null }
        assertEquals(MealSlot.defaultDiary, diary.map { it.slot })
    }

    @Test
    fun aiFoodsAreLinkedToTheDatabaseByName() {
        val ai = listOf(
            PlanMeal(
                "breakfast", "08:00",
                listOf(PlanMealOption("x", foods = listOf(PlanFood(name = "beyaz PEYNIR"), PlanFood(name = "Gizli tarif")))),
            ),
        )
        val linked = ai.linkFoods(listOf(bread, cheese))
        assertEquals(listOf("cheese_white", null), linked[0].options[0].foods.map { it.foodId })
    }

    @Test
    fun slotForTimePicksTheNearestMeal() {
        assertEquals(MealSlot.Breakfast, MealSlot.forTime(7, 15, MealSlot.defaultDiary))
        assertEquals(MealSlot.Lunch, MealSlot.forTime(12, 0, MealSlot.defaultDiary))
        assertEquals(MealSlot.AfternoonSnack, MealSlot.forTime(16, 0, MealSlot.defaultDiary))
        assertEquals(MealSlot.Dinner, MealSlot.forTime(23, 0, MealSlot.defaultDiary))
    }
}
