package com.fitai.app.viewmodel.measurements

import com.fitai.app.engagement.MeasurementHistory

enum class MeasurementMetric { Weight, Waist }

data class MeasurementForm(
    val weightText: String = "",
    val waistText: String = "",
    val weightInvalid: Boolean = false,
    val waistInvalid: Boolean = false,
    val saving: Boolean = false,
)

data class MeasurementsState(
    val history: MeasurementHistory? = null,
    val loading: Boolean = true,
    val metric: MeasurementMetric = MeasurementMetric.Weight,
    val months: Int = PERIODS.first(),
    val form: MeasurementForm? = null,
) {
    companion object {
        val PERIODS = listOf(1, 3, 6, 12)
    }
}

sealed interface MeasurementsIntent {
    data object Load : MeasurementsIntent
    data class SelectMetric(val metric: MeasurementMetric) : MeasurementsIntent
    data class SelectMonths(val months: Int) : MeasurementsIntent
    data object OpenForm : MeasurementsIntent
    data object DismissForm : MeasurementsIntent
    data class WeightChanged(val value: String) : MeasurementsIntent
    data class WaistChanged(val value: String) : MeasurementsIntent
    data object Save : MeasurementsIntent
}
