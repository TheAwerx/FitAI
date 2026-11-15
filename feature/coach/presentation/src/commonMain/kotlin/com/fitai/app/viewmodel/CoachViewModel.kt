package com.fitai.app.viewmodel

import androidx.lifecycle.viewModelScope
import com.fitai.app.access.CheckFeatureAccessUseCase
import com.fitai.app.access.GatedFeature
import com.fitai.app.coach.ClearCoachHistoryUseCase
import com.fitai.app.coach.CoachLimits
import com.fitai.app.coach.CoachSendResult
import com.fitai.app.coach.GetCoachHistoryUseCase
import com.fitai.app.coach.GetWeeklyReportUseCase
import com.fitai.app.coach.SendCoachMessageUseCase
import com.fitai.app.domain.UseCaseState
import com.fitai.app.viewmodel.coach.CoachEffect
import com.fitai.app.viewmodel.coach.CoachIntent
import com.fitai.app.viewmodel.coach.CoachSendError
import com.fitai.app.viewmodel.coach.CoachState
import kotlinx.coroutines.launch

class CoachViewModel(
    private val getWeeklyReportUseCase: GetWeeklyReportUseCase,
    private val getCoachHistoryUseCase: GetCoachHistoryUseCase,
    private val sendCoachMessageUseCase: SendCoachMessageUseCase,
    private val clearCoachHistoryUseCase: ClearCoachHistoryUseCase,
    private val checkFeatureAccessUseCase: CheckFeatureAccessUseCase,
) : BaseViewModel<CoachState, CoachIntent, CoachEffect>(CoachState()) {

    override fun handleIntent(intent: CoachIntent) {
        when (intent) {
            CoachIntent.Load -> load()
            CoachIntent.RetryReport -> loadReport()
            is CoachIntent.InputChanged -> setState {
                copy(input = intent.value.take(CoachLimits.MAX_MESSAGE_CHARS), sendError = sendError.takeIf { pendingMessage != null })
            }
            CoachIntent.Send -> send(currentState().input)
            is CoachIntent.SendPrompt -> send(intent.text)
            CoachIntent.RetrySend -> currentState().pendingMessage?.let(::send)
            CoachIntent.ClearHistory -> clearHistory()
            CoachIntent.Upgrade -> setEffect(CoachEffect.NavigateToPaywall)
        }
    }

    private fun load() {
        loadReport()
        viewModelScope.launch {
            val access = checkFeatureAccessUseCase(GatedFeature.CoachChat)
            val locked = access !is UseCaseState.Success || !access.data.allowed
            setState { copy(chatLocked = locked, accessResolved = true) }
        }
        viewModelScope.launch {
            val history = getCoachHistoryUseCase()
            if (history is UseCaseState.Success) setState { copy(messages = history.data) }
        }
    }

    private fun loadReport() {
        viewModelScope.launch {
            setState { copy(reportLoading = true, reportError = false) }
            when (val result = getWeeklyReportUseCase()) {
                is UseCaseState.Success -> setState { copy(report = result.data, reportLoading = false) }
                is UseCaseState.Error -> setState { copy(reportLoading = false, reportError = report == null) }
            }
        }
    }

    private fun send(text: String) {
        val state = currentState()
        val message = text.trim()
        if (state.sending || state.chatLocked || message.isEmpty()) return
        if (message.length > CoachLimits.MAX_MESSAGE_CHARS) {
            setState { copy(sendError = CoachSendError.TooLong) }
            return
        }
        setState { copy(input = "", pendingMessage = message, sending = true, sendError = null) }
        setEffect(CoachEffect.ScrollToLatest)
        viewModelScope.launch {
            when (val result = sendCoachMessageUseCase(message)) {
                is UseCaseState.Success -> when (val outcome = result.data) {
                    is CoachSendResult.Replied -> setState {
                        copy(messages = outcome.history, pendingMessage = null, sending = false)
                    }
                    CoachSendResult.Locked -> setState {
                        copy(chatLocked = true, pendingMessage = null, sending = false, input = message)
                    }
                    CoachSendResult.Empty -> setState { copy(pendingMessage = null, sending = false) }
                    CoachSendResult.TooLong -> setState {
                        copy(pendingMessage = null, sending = false, input = message, sendError = CoachSendError.TooLong)
                    }
                    CoachSendResult.DailyLimit -> setState {
                        copy(pendingMessage = null, sending = false, input = message, sendError = CoachSendError.DailyLimit)
                    }
                }
                is UseCaseState.Error -> setState { copy(sending = false, sendError = CoachSendError.Failed) }
            }
            setEffect(CoachEffect.ScrollToLatest)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            if (clearCoachHistoryUseCase() is UseCaseState.Success) {
                setState { copy(messages = emptyList(), pendingMessage = null, sendError = null) }
            }
        }
    }
}
