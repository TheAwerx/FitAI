package com.fitai.app.nutrition

import com.fitai.app.model.DailyMacroSummary
import com.fitai.app.model.DietPlan
import kotlin.test.Test
import kotlin.test.assertEquals

class DayMacroTargetsTest {

    private val plan = DietPlan(
        id = "p",
        userId = "u",
        dailyCalories = 2400,
        proteinG = 150,
        carbsG = 250,
        fatG = 80,
        createdAt = "",
        isActive = true,
    )

    @Test
    fun trainingDayRaisesCarbsAndLowersFat() {
        val targets = plan.targetsFor(isTrainingDay = true)
        assertEquals(150, targets.proteinG)
        assertEquals(300, targets.carbsG)
        assertEquals(72, targets.fatG)
        assertEquals(4 * 150 + 4 * 300 + 9 * 72, targets.calories)
    }

    @Test
    fun restDayLowersCarbsAndRaisesFat() {
        val targets = plan.targetsFor(isTrainingDay = false)
        assertEquals(150, targets.proteinG)
        assertEquals(213, targets.carbsG)
        assertEquals(88, targets.fatG)
        assertEquals(2242, targets.calories)
    }

    @Test
    fun remainingNeverNegative() {
        val targets = plan.targetsFor(isTrainingDay = true)
        val remaining = targets.remainingAfter(DailyMacroSummary(5000, 10.0, 400.0, 10.0))
        assertEquals(0, remaining.calories)
        assertEquals(140.0, remaining.proteinG)
        assertEquals(0.0, remaining.carbsG)
        assertEquals(62.0, remaining.fatG)
    }
}
