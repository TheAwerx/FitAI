package com.fitai.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Intent, Effect>(
    initialState: State,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<Effect>(extraBufferCapacity = 64)
    val effects: SharedFlow<Effect> = _effects.asSharedFlow()

    fun onIntent(intent: Intent) {
        handleIntent(intent)
    }

    protected abstract fun handleIntent(intent: Intent)

    protected fun setState(reducer: State.() -> State) {
        _state.update { it.reducer() }
    }

    protected fun setEffect(effect: Effect) {
        viewModelScope.launch {
            _effects.emit(effect)
        }
    }

    protected fun currentState(): State = _state.value
}
