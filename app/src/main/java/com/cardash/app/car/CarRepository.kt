package com.cardash.app.car

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Shared car state, written by [CarService] and read by the UI. */
object CarRepository {
    private val _state = MutableStateFlow(CarState.OFF)
    val state: StateFlow<CarState> = _state.asStateFlow()

    private val _signals = MutableStateFlow(CarSignals())
    val signals: StateFlow<CarSignals> = _signals.asStateFlow()

    internal fun setState(state: CarState) { _state.value = state }
    internal fun setSignals(signals: CarSignals) { _signals.value = signals }
}
