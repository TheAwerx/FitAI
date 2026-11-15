package com.fitai.app.viewmodel.home

import kotlin.math.abs
import kotlin.math.roundToInt

/** "72.4", "72", "-0.6" — dot decimal, one fraction digit at most. */
fun formatOneDecimal(value: Double): String {
    val tenths = (value * 10).roundToInt()
    val whole = tenths / 10
    val fraction = abs(tenths % 10)
    val sign = if (tenths < 0 && whole == 0) "-" else ""
    return if (fraction == 0) "$sign$whole" else "$sign$whole.$fraction"
}
