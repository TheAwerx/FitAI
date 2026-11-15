package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.domain.UseCaseState
import com.fitai.app.engagement.AddWeighInUseCase
import com.fitai.app.engagement.GetProgressOverviewUseCase
import com.fitai.app.viewmodel.home.formatOneDecimal
import com.fitai.app.viewmodel.progress.ProgressEffect
import com.fitai.app.viewmodel.progress.ProgressIntent
import com.fitai.app.viewmodel.progress.ProgressState
import com.fitai.app.viewmodel.progress.WeighInForm
import kotlinx.coroutines.launch

class ProgressViewModel(
    private val getProgressOverviewUseCase: GetProgressOverviewUseCase,
    private val addWeighInUseCase: AddWeighInUseCase,
) : BaseViewModel<ProgressState, ProgressIntent, ProgressEffect>(ProgressState()) {

    override fun handleIntent(intent: ProgressIntent) {
        when (intent) {
            ProgressIntent.Load -> load()
            ProgressIntent.OpenWeighIn -> setState { copy(weighIn = WeighInForm(weightText = latestWeightText())) }
            ProgressIntent.DismissWeighIn -> setState { copy(weighIn = null) }
            is ProgressIntent.WeightChanged -> updateForm { copy(weightText = intent.text, weightInvalid = false) }
            is ProgressIntent.WaistChanged -> updateForm { copy(waistText = intent.text, waistInvalid = false) }
            ProgressIntent.SaveWeighIn -> save()
        }
    }

    private fun updateForm(reducer: WeighInForm.() -> WeighInForm) {
        setState { copy(weighIn = weighIn?.reducer()) }
    }

    private fun load() {
        viewModelScope.launch {
            when (val result = getProgressOverviewUseCase()) {
                is UseCaseState.Success -> setState {
                    val loaded = copy(loading = false, loadFailed = false, overview = result.data)
                    val form = weighIn
                    if (form != null && form.weightText.isBlank()) {
                        loaded.copy(weighIn = form.copy(weightText = loaded.latestWeightText()))
                    } else {
                        loaded
                    }
                }
                is UseCaseState.Error -> setState { copy(loading = false, loadFailed = overview == null) }
            }
        }
    }

    private fun save() {
        val form = currentState().weighIn ?: return
        if (form.saving) return
        val weight = form.weightText.toDecimalOrNull()
        val waist = form.waistText.takeIf { it.isNotBlank() }?.let { it.toDecimalOrNull() ?: Double.NaN }
        val weightInvalid = weight == null || weight !in AddWeighInUseCase.WEIGHT_RANGE_KG
        val waistInvalid = waist != null && waist !in AddWeighInUseCase.WAIST_RANGE_CM
        if (weightInvalid || waistInvalid) {
            updateForm { copy(weightInvalid = weightInvalid, waistInvalid = waistInvalid) }
            return
        }
        updateForm { copy(saving = true) }
        viewModelScope.launch {
            when (addWeighInUseCase(weight, waist)) {
                is UseCaseState.Success -> {
                    setState { copy(weighIn = null) }
                    setEffect(ProgressEffect.WeighInSaved)
                    load()
                }
                is UseCaseState.Error -> {
                    updateForm { copy(saving = false) }
                    setEffect(ProgressEffect.SaveFailed)
                }
            }
        }
    }

    private fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

    private fun ProgressState.latestWeightText(): String = overview?.latestWeightKg?.let(::formatOneDecimal).orEmpty()
}
