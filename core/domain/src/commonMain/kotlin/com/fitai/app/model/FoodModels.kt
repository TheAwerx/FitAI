package com.fitai.app.model

import kotlin.math.roundToInt

/**
 * Meals of the day, in order. Codes are shared with the diet plan JSON (`functions/src/plans.ts`),
 * `meal_logs.slot` and the Firestore `meal_logs.slot` field.
 */
enum class MealSlot(val code: String, val defaultTime: String) {
    Breakfast("breakfast", "08:00"),
    MorningSnack("morning_snack", "10:30"),
    Lunch("lunch", "13:00"),
    AfternoonSnack("afternoon_snack", "16:30"),
    Dinner("dinner", "19:30"),
    EveningSnack("evening_snack", "21:30"),
    ;

    val isSnack: Boolean get() = this == MorningSnack || this == AfternoonSnack || this == EveningSnack

    companion object {
        /** Shown in the diary when there is no plan. */
        val defaultDiary = listOf(Breakfast, Lunch, AfternoonSnack, Dinner)

        fun fromCode(code: String?): MealSlot? = entries.firstOrNull { it.code == code }

        /** The slot a meal logged at [hour]:[minute] most likely belongs to, among [slots]. */
        fun forTime(hour: Int, minute: Int = 0, slots: List<MealSlot> = entries): MealSlot {
            val now = hour * 60 + minute
            val candidates = slots.ifEmpty { entries }
            return candidates.minBy { slot ->
                val (h, m) = slot.defaultTime.split(":").map(String::toInt)
                kotlin.math.abs(h * 60 + m - now)
            }
        }
    }
}

/**
 * Household serving units. Codes are stored with logged foods and plan foods and are shared with the
 * server prompt; labels are localized on the device. Every food can also be weighed in [GRAM].
 */
object FoodUnits {
    const val GRAM = "g"
    const val ML = "ml"
    const val PIECE = "piece"
    const val SLICE = "slice"
    const val PORTION = "portion"
    const val BOWL = "bowl"
    const val GLASS = "glass"
    const val TEA_GLASS = "tea_glass"
    const val CUP = "cup"
    const val MUG = "mug"
    const val TBSP = "tbsp"
    const val DSP = "dsp"
    const val TSP = "tsp"
    const val HANDFUL = "handful"
    const val LADLE = "ladle"
    const val CAN = "can"
    const val PACK = "pack"
    const val SCOOP = "scoop"

    val all = listOf(
        GRAM, ML, PIECE, SLICE, PORTION, BOWL, GLASS, TEA_GLASS, CUP, MUG, TBSP, DSP, TSP, HANDFUL, LADLE, CAN, PACK, SCOOP,
    )

    /** Units a user may pick for a custom food's serving. */
    val customServing = listOf(PORTION, PIECE, SLICE, BOWL, GLASS, TBSP, PACK, GRAM)

    fun isKnown(code: String?): Boolean = code in all
}

/** Diet-relevant ingredient flags on a [FoodItem]. */
object FoodTags {
    const val MEAT = "meat"
    const val POULTRY = "poultry"
    const val FISH = "fish"
    const val SHELLFISH = "shellfish"
    const val PORK = "pork"
    const val DAIRY = "dairy"
    const val EGG = "egg"
    const val HONEY = "honey"
    const val GLUTEN = "gluten"
    const val ALCOHOL = "alcohol"
    const val NUTS = "nuts"
}

enum class FoodCategory(val code: String) {
    Bread("bread"),
    Breakfast("breakfast"),
    Egg("egg"),
    Cheese("cheese"),
    Dairy("dairy"),
    Meat("meat"),
    Poultry("poultry"),
    Fish("fish"),
    Legume("legume"),
    Grain("grain"),
    Soup("soup"),
    Dish("dish"),
    Street("street"),
    Salad("salad"),
    Vegetable("vegetable"),
    Fruit("fruit"),
    Nut("nut"),
    Dessert("dessert"),
    Snack("snack"),
    Drink("drink"),
    Fat("fat"),
    Sauce("sauce"),
    Custom("custom"),
    ;

    companion object {
        fun fromCode(code: String?): FoodCategory? = entries.firstOrNull { it.code == code }
    }
}

/** [grams] is the weight of one [unit]. */
data class FoodServing(val unit: String, val grams: Double)

/** Calories and macros of an amount of food. */
data class Macros(
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
) {
    operator fun plus(other: Macros) = Macros(
        calories + other.calories,
        proteinG + other.proteinG,
        carbsG + other.carbsG,
        fatG + other.fatG,
    )

    operator fun times(factor: Double) = Macros(calories * factor, proteinG * factor, carbsG * factor, fatG * factor)

    companion object {
        val Zero = Macros(0.0, 0.0, 0.0, 0.0)
    }
}

fun Iterable<Macros>.sum(): Macros = fold(Macros.Zero) { acc, m -> acc + m }

/**
 * One entry of the food database: a bundled food (`core/data/.../files/foods.json`) or a user's custom
 * food ([isCustom]). Nutrition values are per 100 g of the edible part.
 */
data class FoodItem(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val category: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double? = null,
    /** Household servings, the most common first. [FoodUnits.GRAM] is always available on top of these. */
    val servings: List<FoodServing> = emptyList(),
    val tags: Set<String> = emptySet(),
    val keywords: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val source: String? = null,
) {
    fun name(locale: String): String = if (locale == "en") nameEn.ifBlank { nameTr } else nameTr.ifBlank { nameEn }

    /** The servings the picker offers: the food's own units, then grams. */
    val pickerServings: List<FoodServing>
        get() = servings.filter { it.unit != FoodUnits.GRAM && it.grams > 0 } + FoodServing(FoodUnits.GRAM, 1.0)

    val defaultServing: FoodServing get() = pickerServings.first()

    fun gramsOf(unit: String): Double? = pickerServings.firstOrNull { it.unit == unit }?.grams

    fun macrosFor(grams: Double): Macros {
        val factor = grams.coerceAtLeast(0.0) / 100.0
        return Macros(kcalPer100g * factor, proteinPer100g * factor, carbsPer100g * factor, fatPer100g * factor)
    }

    fun macrosFor(unit: String, amount: Double): Macros = macrosFor((gramsOf(unit) ?: 1.0) * amount)

    /** False when one of the profile's diet restrictions rules this food out. */
    fun isAllowedFor(restrictions: Set<String>): Boolean = restrictions.none { code -> excludedTags(code).any { it in tags } }

    companion object {
        /** Tags a diet restriction code (see [DietRestriction]) excludes. Kosher's meat-and-dairy rule is per meal. */
        fun excludedTags(restriction: String): Set<String> = when (restriction) {
            DietRestriction.Vegetarian.code -> setOf(FoodTags.MEAT, FoodTags.POULTRY, FoodTags.FISH, FoodTags.SHELLFISH, FoodTags.PORK)
            DietRestriction.Vegan.code -> setOf(
                FoodTags.MEAT, FoodTags.POULTRY, FoodTags.FISH, FoodTags.SHELLFISH, FoodTags.PORK,
                FoodTags.DAIRY, FoodTags.EGG, FoodTags.HONEY,
            )
            DietRestriction.Pescatarian.code -> setOf(FoodTags.MEAT, FoodTags.POULTRY, FoodTags.PORK)
            DietRestriction.Halal.code -> setOf(FoodTags.PORK, FoodTags.ALCOHOL)
            DietRestriction.Kosher.code -> setOf(FoodTags.PORK, FoodTags.SHELLFISH)
            DietRestriction.GlutenFree.code -> setOf(FoodTags.GLUTEN)
            DietRestriction.DairyFree.code -> setOf(FoodTags.DAIRY)
            else -> emptySet()
        }
    }
}

/** A user-created food, entered per serving; stored per 100 g. */
data class CustomFoodDraft(
    val name: String,
    val servingUnit: String,
    val servingGrams: Double,
    val caloriesPerServing: Double,
    val proteinPerServing: Double,
    val carbsPerServing: Double,
    val fatPerServing: Double,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && servingGrams > 0 && caloriesPerServing >= 0 &&
            proteinPerServing >= 0 && carbsPerServing >= 0 && fatPerServing >= 0 &&
            FoodUnits.isKnown(servingUnit)

    fun per100g(value: Double): Double = value * 100.0 / servingGrams
}

/** Rounds to one decimal, for gram and macro values shown or stored. */
fun Double.roundToTenth(): Double = (this * 10).roundToInt() / 10.0
