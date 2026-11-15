package com.fitai.app.viewmodel.progress

import com.fitai.app.engagement.ProgressOverview

data class WeighInForm(
    val weightText: String = "",
    val waistText: String = "",
    val weightInvalid: Boolean = false,
    val waistInvalid: Boolean = false,
    val saving: Boolean = false,
)

data class ProgressState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val overview: ProgressOverview? = null,
    /** Non-null while the weigh-in dialog is open. */
    val weighIn: WeighInForm? = null,
)

sealed interface ProgressIntent {
    data object Load : ProgressIntent
    data object OpenWeighIn : ProgressIntent
    data object DismissWeighIn : ProgressIntent
    data class WeightChanged(val text: String) : ProgressIntent
    data class WaistChanged(val text: String) : ProgressIntent
    data object SaveWeighIn : ProgressIntent
}

sealed interface ProgressEffect {
    data object WeighInSaved : ProgressEffect
    data object SaveFailed : ProgressEffect
}
