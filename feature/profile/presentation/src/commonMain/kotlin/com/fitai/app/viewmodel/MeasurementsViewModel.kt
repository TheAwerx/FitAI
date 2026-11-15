package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.domain.UseCaseState
import com.fitai.app.engagement.AddWeighInUseCase
import com.fitai.app.engagement.GetMeasurementHistoryUseCase
import com.fitai.app.viewmodel.measurements.MeasurementForm
import com.fitai.app.viewmodel.measurements.MeasurementsIntent
import com.fitai.app.viewmodel.measurements.MeasurementsState
import kotlinx.coroutines.launch

class MeasurementsViewModel(
    private val getMeasurementHistoryUseCase: GetMeasurementHistoryUseCase,
    private val addWeighInUseCase: AddWeighInUseCase,
) : BaseViewModel<MeasurementsState, MeasurementsIntent, Unit>(MeasurementsState()) {

    override fun handleIntent(intent: MeasurementsIntent) {
        when (intent) {
            MeasurementsIntent.Load -> load()
            is MeasurementsIntent.SelectMetric -> setState { copy(metric = intent.metric) }
            is MeasurementsIntent.SelectMonths -> setState { copy(months = intent.months) }
            MeasurementsIntent.OpenForm -> setState { copy(form = MeasurementForm()) }
            MeasurementsIntent.DismissForm -> setState { copy(form = null) }
            is MeasurementsIntent.WeightChanged -> updateForm {
                copy(weightText = intent.value.filter { it.isDigit() || it == '.' || it == ',' }, weightInvalid = false)
            }
            is MeasurementsIntent.WaistChanged -> updateForm {
                copy(waistText = intent.value.filter { it.isDigit() || it == '.' || it == ',' }, waistInvalid = false)
            }
            MeasurementsIntent.Save -> save()
        }
    }

    private fun updateForm(block: MeasurementForm.() -> MeasurementForm) {
        val form = currentState().form ?: return
        setState { copy(form = form.block()) }
    }

    private fun load() {
        viewModelScope.launch {
            val history = (getMeasurementHistoryUseCase() as? UseCaseState.Success)?.data
            setState { copy(history = history, loading = false) }
        }
    }

    private fun save() {
        val form = currentState().form ?: return
        if (form.saving) return
        val weight = form.weightText.replace(',', '.').toDoubleOrNull()
        val waist = form.waistText.takeIf { it.isNotBlank() }?.replace(',', '.')?.toDoubleOrNull()
        val weightInvalid = weight == null || weight !in AddWeighInUseCase.WEIGHT_RANGE_KG
        val waistInvalid = form.waistText.isNotBlank() && (waist == null || waist !in AddWeighInUseCase.WAIST_RANGE_CM)
        if (weightInvalid || waistInvalid) {
            setState { copy(form = form.copy(weightInvalid = weightInvalid, waistInvalid = waistInvalid)) }
            return
        }
        viewModelScope.launch {
            setState { copy(form = form.copy(saving = true)) }
            when (addWeighInUseCase(weight!!, waist)) {
                is UseCaseState.Success -> {
                    setState { copy(form = null) }
                    load()
                }
                is UseCaseState.Error -> setState { copy(form = form.copy(saving = false, weightInvalid = true)) }
            }
        }
    }
}
