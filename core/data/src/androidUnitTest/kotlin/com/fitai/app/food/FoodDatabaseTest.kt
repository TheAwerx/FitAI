package com.fitai.app.food

import com.fitai.app.model.DietRestriction
import com.fitai.app.model.FitnessGoal
import com.fitai.app.model.FoodCategory
import com.fitai.app.model.FoodItem
import com.fitai.app.model.FoodTags
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.MealSlot
import com.fitai.app.model.UserProfilePayload
import com.fitai.app.nutrition.StarterDiet
import com.fitai.app.nutrition.normalizeFoodText
import com.fitai.app.nutrition.plannedTotals
import com.fitai.app.nutrition.searchFoods
import com.fitai.app.training.StarterPlans
import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Checks the bundled food database (`composeResources/files/foods.json`) and the starter diet built from it. */
class FoodDatabaseTest {

    private val foods: List<FoodItem> by lazy {
        parseFoodDatabase(File("src/commonMain/composeResources/files/foods.json").readText())
    }

    private val knownTags = setOf(
        FoodTags.MEAT, FoodTags.POULTRY, FoodTags.FISH, FoodTags.SHELLFISH, FoodTags.PORK, FoodTags.DAIRY,
        FoodTags.EGG, FoodTags.HONEY, FoodTags.GLUTEN, FoodTags.ALCOHOL, FoodTags.NUTS,
    )

    @Test
    fun everyFoodIsWellFormed() {
        assertTrue(foods.size >= 250, "only ${foods.size} foods")
        assertEquals(foods.size, foods.map { it.id }.toSet().size, "duplicate ids")
        foods.forEach { food ->
            assertTrue(food.nameTr.isNotBlank() && food.nameEn.isNotBlank(), food.id)
            assertTrue(FoodCategory.fromCode(food.category) != null, "${food.id}: category ${food.category}")
            assertTrue(food.servings.isNotEmpty(), "${food.id}: no household serving")
            food.servings.forEach { assertTrue(FoodUnits.isKnown(it.unit) && it.grams > 0, "${food.id}: ${it.unit}") }
            assertTrue(knownTags.containsAll(food.tags), "${food.id}: ${food.tags}")
            assertTrue(food.source in setOf("usda", "recipe", "label"), "${food.id}: source ${food.source}")
            if (FoodTags.ALCOHOL !in food.tags && food.kcalPer100g >= 20) {
                val fromMacros = 4 * food.proteinPer100g + 4 * food.carbsPer100g + 9 * food.fatPer100g
                val drift = abs(fromMacros - food.kcalPer100g)
                assertTrue(drift <= 10 || drift / food.kcalPer100g <= 0.20, "${food.id}: ${food.kcalPer100g} kcal vs $fromMacros")
            }
        }
    }

    @Test
    fun turkishStaplesAreCovered() {
        val names = foods.map { normalizeFoodText(it.nameTr) }
        listOf(
            "beyaz ekmek", "simit", "beyaz peynir", "kaşar peyniri", "siyah zeytin", "haşlanmış yumurta", "menemen",
            "mercimek çorbası", "etli kuru fasulye", "pirinç pilavı", "bulgur pilavı", "ızgara köfte", "tavuk döner",
            "lahmacun", "kıymalı pide", "ayran", "yoğurt (tam yağlı)", "baklava", "fındık", "çay (şekersiz)",
        ).forEach { name -> assertTrue(normalizeFoodText(name) in names, "missing $name") }
    }

    @Test
    fun searchOverTheRealCatalog() {
        assertEquals("Mercimek çorbası", searchFoods(foods, "mercimek", "tr").first().nameTr)
        assertEquals("Ayran", searchFoods(foods, "ayran", "tr").first().nameTr)
        assertTrue(searchFoods(foods, "peynir", "tr").size >= 8)
        assertTrue(searchFoods(foods, "kasar", "tr").any { it.id == "cheese_kashar" })
        assertTrue(searchFoods(foods, "doner", "tr").take(3).any { it.id == "chicken_doner" })
        assertEquals("cheese_white", searchFoods(foods, "feta", "en").first().id)
    }

    private fun profile(restrictions: String, goal: String = "lose_fat", gender: String = "female", weight: Double = 68.0) =
        UserProfilePayload(
            name = "Test", age = 30, heightCm = 168.0, weightKg = weight, goal = goal, activityLevel = "moderate",
            equipment = "", dietRestrictions = restrictions, gender = gender,
        )

    private val restrictionCases = listOf(
        "none", "vegetarian", "vegan", "pescatarian", "halal", "kosher", "gluten-free", "dairy-free",
        "vegan,gluten-free", "vegetarian,dairy-free", "kosher,gluten-free",
    )

    @Test
    fun starterDietIsDetailedAndRespectsEveryRestriction() {
        val byId = foods.associateBy { it.id }
        restrictionCases.forEach { codes ->
            listOf("lose_fat" to "female", "build_muscle" to "male", "stay_healthy" to "female").forEach { (goal, gender) ->
                val label = "$codes/$goal"
                val plan = StarterPlans.diet(profile(codes, goal, gender, if (gender == "male") 82.0 else 64.0), foods, "tr")
                val restrictions = codes.split(",").toSet() - DietRestriction.None.code
                assertTrue(plan.meals.size >= 4, "$label: ${plan.meals.size} meals")
                assertEquals(plan.meals.map { it.slot }, plan.meals.sortedBy { MealSlot.fromCode(it.slot)!!.ordinal }.map { it.slot })
                val shares = StarterDiet.slotShares(plan.dailyCalories, FitnessGoal.fromCode(goal)).toMap()
                plan.meals.forEach { meal ->
                    assertTrue(meal.options.size >= 2, "$label ${meal.slot}: no alternative")
                    assertTrue(meal.time.matches(Regex("\\d{2}:\\d{2}")), meal.time)
                    val slotKcal = plan.dailyCalories * shares.getValue(MealSlot.fromCode(meal.slot)!!)
                    meal.options.forEach { option ->
                        assertTrue(option.title.isNotBlank() && option.note.isNotBlank(), "$label: ${option.title}")
                        val optionFoods = option.foods.map { byId.getValue(it.foodId!!) }
                        optionFoods.forEach { food ->
                            assertTrue(food.isAllowedFor(restrictions), "$label: ${food.id} in ${option.title}")
                        }
                        if ("kosher" in restrictions) {
                            val meat = optionFoods.any { FoodTags.MEAT in it.tags || FoodTags.POULTRY in it.tags }
                            assertTrue(!(meat && optionFoods.any { FoodTags.DAIRY in it.tags }), "$label: meat with dairy")
                        }
                        option.foods.forEach { food ->
                            assertTrue(food.amount > 0 && food.grams > 0, "$label: ${food.name}")
                            val db = byId.getValue(food.foodId!!).macrosFor(food.grams)
                            assertTrue(abs(db.calories - food.calories) <= 1.0, "$label: ${food.name} kcal")
                        }
                        // Every option (main or alternative) is sized for the meal's share of the day; small
                        // snacks get an absolute margin because portions move in half servings.
                        val kcal = option.macros.calories
                        assertTrue(
                            abs(kcal - slotKcal) <= maxOf(slotKcal * 0.25, 60.0),
                            "$label ${meal.slot}: ${option.title} $kcal vs $slotKcal",
                        )
                    }
                }
                val totals = plan.meals.plannedTotals()
                assertTrue(
                    abs(totals.calories - plan.dailyCalories) <= plan.dailyCalories * 0.10,
                    "$label: ${totals.calories} kcal vs ${plan.dailyCalories}",
                )
                assertTrue(
                    abs(totals.proteinG - plan.proteinG) <= plan.proteinG * 0.25,
                    "$label: ${totals.proteinG} g protein vs ${plan.proteinG}",
                )
            }
        }
    }

    @Test
    fun bigBudgetsGetFiveMeals() {
        val plan = StarterPlans.diet(profile("none", "build_muscle", "male", 90.0), foods, "en")
        assertEquals(
            listOf(MealSlot.Breakfast, MealSlot.MorningSnack, MealSlot.Lunch, MealSlot.AfternoonSnack, MealSlot.Dinner).map { it.code },
            plan.meals.map { it.slot },
        )
        // English plans use the English food names.
        assertTrue(plan.meals.first().options.first().foods.all { food -> foods.first { it.id == food.foodId }.nameEn == food.name })
    }
}
