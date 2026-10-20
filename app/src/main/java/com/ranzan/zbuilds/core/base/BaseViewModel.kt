package com.ranzan.zbuilds.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base MVI contract marker interfaces.
 */
interface UiState
interface UiAction
interface UiEffect

/**
 * BaseViewModel implementing Model-View-Intent (MVI) architecture:
 * - [uiState]: Continuous StateFlow observed by UI representing single source of truth.
 * - [onAction]: Direct user action/intent dispatcher.
 * - [uiEffect]: Channel flow for one-time side-effects (navigation, dialogs, snackbars).
 */
abstract class BaseViewModel<State : UiState, Action : UiAction, Effect : UiEffect>(
    initialState: State
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _uiEffect = Channel<Effect>(Channel.BUFFERED)
    val uiEffect: Flow<Effect> = _uiEffect.receiveAsFlow()

    val currentState: State
        get() = _uiState.value

    abstract fun onAction(action: Action)

    protected fun setState(reduce: State.() -> State) {
        _uiState.update { it.reduce() }
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
