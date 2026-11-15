package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.access.CheckFeatureAccessUseCase
import com.fitai.app.access.GatedFeature
import com.fitai.app.access.RecordFeatureUseUseCase
import com.fitai.app.analytics.AnalyticsEvents
import com.fitai.app.analytics.TrackEventUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.viewmodel.workout.TodayModeIntent
import com.fitai.app.viewmodel.workout.TodayModeState
import com.fitai.app.viewmodel.workout.WorkoutEffect
import com.fitai.app.workout.StartTodayModeSessionUseCase
import com.fitai.app.workout.TodayModeInput
import kotlinx.coroutines.launch

class TodayModeViewModel(
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
    private val recordFeatureUseUseCase: RecordFeatureUseUseCase,
    private val startTodayModeSessionUseCase: StartTodayModeSessionUseCase,
    private val trackEventUseCase: TrackEventUseCase,
) : BaseViewModel<TodayModeState, TodayModeIntent, WorkoutEffect>(TodayModeState()) {

    override fun handleIntent(intent: TodayModeIntent) {
        when (intent) {
            TodayModeIntent.Load -> load()
            is TodayModeIntent.SelectMinutes -> setState { copy(minutes = intent.minutes) }
            is TodayModeIntent.SelectEnergy -> setState { copy(energy = intent.energy.coerceIn(1, 5)) }
            is TodayModeIntent.TogglePain -> setState {
                copy(painAreas = if (intent.area in painAreas) painAreas - intent.area else painAreas + intent.area)
            }
            TodayModeIntent.ClearPain -> setState { copy(painAreas = emptySet()) }
            is TodayModeIntent.SelectLocation -> setState { copy(location = intent.location) }
            TodayModeIntent.Submit -> submit()
        }
    }

    private fun load() {
        setState { TodayModeState(checkingAccess = true) }
        viewModelScope.launch {
            when (val result = checkFeatureAccessUseCase(GatedFeature.TodayMode)) {
                is UseCaseState.Success -> setState { copy(access = result.data, checkingAccess = false) }
                is UseCaseState.Error -> setState { copy(checkingAccess = false, errorMessage = result.message) }
            }
        }
    }

    private fun submit() {
        val state = currentState()
        if (state.submitting) return
        val access = state.access ?: return load()
        if (!access.allowed) {
            setEffect(WorkoutEffect.NavigateToPaywall)
            return
        }
        setState { copy(submitting = true, errorMessage = null) }
        viewModelScope.launch {
            val input = TodayModeInput(
                availableMinutes = state.minutes,
                energy = state.energy,
                painAreas = state.painAreas.map { it.code },
                location = state.location,
            )
            when (val result = startTodayModeSessionUseCase(input)) {
                is UseCaseState.Success -> {
                    recordFeatureUseUseCase(GatedFeature.TodayMode)
                    trackEventUseCase(
                        AnalyticsEvents.TODAY_MODE_USED,
                        mapOf(
                            "minutes" to input.availableMinutes.toString(),
                            "energy" to input.energy.toString(),
                            "location" to input.location,
                            "pain_areas" to input.painAreas.joinToString(","),
                        ),
                    )
                    trackEventUseCase(AnalyticsEvents.WORKOUT_STARTED, mapOf("source" to "today_mode"))
                    setState { copy(submitting = false) }
                    setEffect(WorkoutEffect.NavigateToActiveSession(result.data.session.id))
                }
                is UseCaseState.Error -> setState { copy(submitting = false, errorMessage = result.message) }
            }
        }
    }
}
