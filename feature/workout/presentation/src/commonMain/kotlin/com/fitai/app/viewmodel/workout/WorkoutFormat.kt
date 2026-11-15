package com.fitai.app.viewmodel.workout

import kotlin.math.abs
import kotlin.math.roundToLong

/** "62.5", "60", "61.25" — dot decimal; localized display goes through `Strings.workoutKg`. */
fun formatKg(value: Double): String {
    val hundredths = (value * 100).roundToLong()
    val whole = hundredths / 100
    val fraction = abs(hundredths % 100)
    return when {
        fraction == 0L -> whole.toString()
        fraction % 10 == 0L -> "$whole.${fraction / 10}"
        else -> "$whole.${fraction.toString().padStart(2, '0')}"
    }
}

/** Accepts both "62,5" and "62.5"; blank or invalid → null. */
fun parseKg(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }
