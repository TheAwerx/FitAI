package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.domain.UseCaseState
import com.fitai.app.training.DetectSessionPRsUseCase
import com.fitai.app.viewmodel.workout.SessionSummaryEffect
import com.fitai.app.viewmodel.workout.SessionSummaryIntent
import com.fitai.app.viewmodel.workout.SessionSummaryState
import com.fitai.app.workout.GetSessionSummaryUseCase
import kotlinx.coroutines.launch

class SessionSummaryViewModel(
    private val getSessionSummaryUseCase: GetSessionSummaryUseCase,
    private val detectSessionPRsUseCase: DetectSessionPRsUseCase,
) : BaseViewModel<SessionSummaryState, SessionSummaryIntent, SessionSummaryEffect>(SessionSummaryState()) {

    override fun handleIntent(intent: SessionSummaryIntent) {
        when (intent) {
            is SessionSummaryIntent.Load -> load(intent.sessionId)
        }
    }

    private fun load(sessionId: String) {
        setState { SessionSummaryState(loading = true) }
        viewModelScope.launch {
            val summary = (getSessionSummaryUseCase(sessionId) as? UseCaseState.Success)?.data
            val records = (detectSessionPRsUseCase(sessionId) as? UseCaseState.Success)?.data.orEmpty()
            setState { copy(summary = summary, personalRecords = records, loading = false) }
        }
    }
}
