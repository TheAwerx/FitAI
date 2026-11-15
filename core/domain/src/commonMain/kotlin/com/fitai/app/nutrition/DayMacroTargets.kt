package com.fitai.app.nutrition

import com.fitai.app.model.DailyMacroSummary
import com.fitai.app.model.DietPlan
import com.fitai.app.model.MealLog
import kotlin.math.roundToInt

/** Today's macro targets after the training/rest day adjustment of the active [DietPlan]. */
data class DayMacroTargets(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val isTrainingDay: Boolean,
)

private const val TRAINING_CARB_FACTOR = 1.20
private const val TRAINING_FAT_FACTOR = 0.90
private const val REST_CARB_FACTOR = 0.85
private const val REST_FAT_FACTOR = 1.10

/**
 * Training day: carbs +20 %, fat −10 %. Rest day: carbs −15 %, fat +10 %.
 * Protein is unchanged; calories are recomputed as 4p + 4c + 9f.
 */
fun DietPlan.targetsFor(isTrainingDay: Boolean): DayMacroTargets {
    val carbs = carbsG * if (isTrainingDay) TRAINING_CARB_FACTOR else REST_CARB_FACTOR
    val fat = fatG * if (isTrainingDay) TRAINING_FAT_FACTOR else REST_FAT_FACTOR
    return DayMacroTargets(
        calories = (4.0 * proteinG + 4.0 * carbs + 9.0 * fat).roundToInt(),
        proteinG = proteinG,
        carbsG = carbs.roundToInt(),
        fatG = fat.roundToInt(),
        isTrainingDay = isTrainingDay,
    )
}

fun List<MealLog>.sumMacros(): DailyMacroSummary = DailyMacroSummary(
    calories = sumOf { it.calories },
    proteinG = sumOf { it.proteinG },
    carbsG = sumOf { it.carbsG },
    fatG = sumOf { it.fatG },
)

/** Targets minus [consumed], clamped at zero per macro. */
fun DayMacroTargets.remainingAfter(consumed: DailyMacroSummary): DailyMacroSummary = DailyMacroSummary(
    calories = (calories - consumed.calories).coerceAtLeast(0),
    proteinG = (proteinG - consumed.proteinG).coerceAtLeast(0.0),
    carbsG = (carbsG - consumed.carbsG).coerceAtLeast(0.0),
    fatG = (fatG - consumed.fatG).coerceAtLeast(0.0),
)
