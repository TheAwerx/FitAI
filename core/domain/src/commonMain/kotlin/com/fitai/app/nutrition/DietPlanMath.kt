package com.fitai.app.nutrition

import com.fitai.app.model.DietPlan
import com.fitai.app.model.FoodItem
import com.fitai.app.model.FoodUnits
import com.fitai.app.model.Macros
import com.fitai.app.model.MealEntryDraft
import com.fitai.app.model.MealLog
import com.fitai.app.model.MealSlot
import com.fitai.app.model.PlanFood
import com.fitai.app.model.PlanMeal
import com.fitai.app.model.roundToTenth
import com.fitai.app.model.sum
import kotlin.math.roundToInt

/** Sum of the selected option of every meal. */
fun List<PlanMeal>.plannedTotals(): Macros = mapNotNull { it.current?.macros }.sum()

fun DietPlan.plannedTotals(): Macros = meals.plannedTotals()

/** Meals with [slot] switched to option [index]; unchanged when either doesn't exist. */
fun List<PlanMeal>.selectOption(slot: String, index: Int): List<PlanMeal> = map { meal ->
    if (meal.slot == slot && index in meal.options.indices) meal.copy(selected = index) else meal
}

/** Diary entries for eating option [optionIndex] (default: the selected one) of this meal as planned. */
fun PlanMeal.toEntries(optionIndex: Int = selectedIndex): List<MealEntryDraft> {
    val option = options.getOrNull(optionIndex) ?: return emptyList()
    return option.foods.filter { it.name.isNotBlank() }.map { food ->
        MealEntryDraft(
            name = food.name,
            calories = food.calories.coerceAtLeast(0),
            proteinG = food.proteinG.coerceAtLeast(0.0).roundToTenth(),
            carbsG = food.carbsG.coerceAtLeast(0.0).roundToTenth(),
            fatG = food.fatG.coerceAtLeast(0.0).roundToTenth(),
            slot = slot,
            foodId = food.foodId,
            servingUnit = food.unit.takeIf { food.grams > 0 } ?: FoodUnits.GRAM,
            servingAmount = food.amount.takeIf { it > 0 } ?: food.grams,
            grams = food.grams.takeIf { it > 0 },
        )
    }
}

/** Diary entry for [amount] of [unit] of this food. */
fun FoodItem.entryFor(unit: String, amount: Double, slot: String?, locale: String): MealEntryDraft {
    val gramsPerUnit = gramsOf(unit) ?: 1.0
    val grams = (gramsPerUnit * amount).coerceAtLeast(0.0)
    val macros = macrosFor(grams)
    return MealEntryDraft(
        name = name(locale),
        calories = macros.calories.roundToInt(),
        proteinG = macros.proteinG.roundToTenth(),
        carbsG = macros.carbsG.roundToTenth(),
        fatG = macros.fatG.roundToTenth(),
        slot = slot,
        foodId = id,
        servingUnit = if (gramsOf(unit) != null) unit else FoodUnits.GRAM,
        servingAmount = amount,
        grams = grams.roundToTenth(),
    )
}

/**
 * The entry with a new amount. With the [food] it is recomputed from the database values; without it (an
 * AI plan food, or a custom food that was deleted) the old values are scaled by weight, in grams.
 */
fun MealLog.withAmount(food: FoodItem?, unit: String, amount: Double, slot: String? = this.slot): MealLog {
    if (food != null) {
        val entry = food.entryFor(unit, amount, slot, "tr")
        return copy(
            calories = entry.calories,
            proteinG = entry.proteinG,
            carbsG = entry.carbsG,
            fatG = entry.fatG,
            servingUnit = entry.servingUnit,
            servingAmount = entry.servingAmount,
            grams = entry.grams,
            slot = slot,
        )
    }
    val oldGrams = grams?.takeIf { it > 0 } ?: return copy(slot = slot)
    val gramsPerUnit = if (unit == servingUnit && (servingAmount ?: 0.0) > 0) oldGrams / servingAmount!! else 1.0
    val newGrams = gramsPerUnit * amount
    val factor = newGrams / oldGrams
    return copy(
        calories = (calories * factor).roundToInt(),
        proteinG = (proteinG * factor).roundToTenth(),
        carbsG = (carbsG * factor).roundToTenth(),
        fatG = (fatG * factor).roundToTenth(),
        servingUnit = if (unit == servingUnit) unit else FoodUnits.GRAM,
        servingAmount = amount,
        grams = newGrams.roundToTenth(),
        slot = slot,
    )
}

/**
 * Links plan foods without an id (AI plans) to the database when the name matches exactly, ignoring case
 * and Turkish accents, so they show up in recents and can be edited from the database values.
 */
fun List<PlanMeal>.linkFoods(catalog: List<FoodItem>): List<PlanMeal> {
    if (catalog.isEmpty()) return this
    val byName = HashMap<String, FoodItem>()
    catalog.forEach { food ->
        byName.getOrPut(normalizeFoodText(food.nameTr)) { food }
        byName.getOrPut(normalizeFoodText(food.nameEn)) { food }
    }
    fun PlanFood.linked(): PlanFood {
        if (foodId != null) return this
        val food = byName[normalizeFoodText(name)] ?: return this
        return copy(foodId = food.id)
    }
    return map { meal -> meal.copy(options = meal.options.map { option -> option.copy(foods = option.foods.map { it.linked() }) }) }
}

/** A diary section: one meal of the day with the plan for it and what was logged. */
data class DiarySlot(
    val slot: MealSlot,
    val time: String,
    val plan: PlanMeal?,
    val entries: List<MealLog>,
) {
    val consumed: Macros get() = entries.map { Macros(it.calories.toDouble(), it.proteinG, it.carbsG, it.fatG) }.sum()
}

/**
 * Today's diary: the plan's meals in order (or [MealSlot.defaultDiary] without a plan). Entries without a
 * slot, or with a slot the plan doesn't have, go to the nearest meal by [localMinutesOf] their log time.
 */
fun buildDiary(plan: DietPlan?, todayMeals: List<MealLog>, localMinutesOf: (MealLog) -> Int?): List<DiarySlot> {
    val planned = plan?.meals.orEmpty().filter { it.mealSlot != null }.distinctBy { it.slot }
    val slots = if (planned.isNotEmpty()) {
        planned.mapNotNull { it.mealSlot }
    } else {
        MealSlot.defaultDiary
    }.let { base ->
        // A slot that only has logged entries (e.g. an evening snack) still gets its own section.
        val extra = todayMeals.mapNotNull { MealSlot.fromCode(it.slot) }.filter { it !in base }
        (base + extra).distinct().sortedBy { it.ordinal }
    }
    val grouped = todayMeals.groupBy { meal ->
        MealSlot.fromCode(meal.slot)?.takeIf { it in slots }
            ?: localMinutesOf(meal)?.let { MealSlot.forTime(it / 60, it % 60, slots) }
            ?: slots.first()
    }
    return slots.map { slot ->
        val planMeal = planned.firstOrNull { it.slot == slot.code }
        DiarySlot(
            slot = slot,
            time = planMeal?.time?.takeIf { it.isNotBlank() } ?: slot.defaultTime,
            plan = planMeal,
            entries = grouped[slot].orEmpty().sortedBy { it.loggedAt },
        )
    }
}
