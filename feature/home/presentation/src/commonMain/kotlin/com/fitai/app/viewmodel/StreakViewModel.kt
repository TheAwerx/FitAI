package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.domain.UseCaseState
import com.fitai.app.engagement.GetWorkoutStreakUseCase
import com.fitai.app.engagement.WorkoutStreak
import kotlinx.coroutines.launch

data class StreakState(
    val streak: WorkoutStreak? = null,
    val loading: Boolean = true,
    val showHowItWorks: Boolean = false,
)

sealed interface StreakIntent {
    data object Load : StreakIntent
    data class ToggleHowItWorks(val show: Boolean) : StreakIntent
}

class StreakViewModel(
    private val getWorkoutStreakUseCase: GetWorkoutStreakUseCase,
) : BaseViewModel<StreakState, StreakIntent, Unit>(StreakState()) {

    override fun handleIntent(intent: StreakIntent) {
        when (intent) {
            StreakIntent.Load -> viewModelScope.launch {
                val streak = (getWorkoutStreakUseCase() as? UseCaseState.Success)?.data
                setState { copy(streak = streak, loading = false) }
            }
            is StreakIntent.ToggleHowItWorks -> setState { copy(showHowItWorks = intent.show) }
        }
    }
}
