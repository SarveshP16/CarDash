package com.cardash.app.car

enum class CarState { ON, OFF }

/** Raw inputs the car state is derived from. */
data class CarSignals(
    val powered: Boolean = false,
    val carBluetoothConnected: Boolean = false,
    /** Set from the dashboard's "Car mode" button for testing without the car; overrides everything. */
    val manualOverride: CarState? = null,
)

object CarStateLogic {
    /**
     * Either signal turns the car on. If the charger socket stays powered with the engine off,
     * power is ignored and Bluetooth alone decides.
     */
    fun resolve(signals: CarSignals, chargerAlwaysOn: Boolean): CarState {
        signals.manualOverride?.let { return it }
        val powered = signals.powered && !chargerAlwaysOn
        return if (powered || signals.carBluetoothConnected) CarState.ON else CarState.OFF
    }

    /**
     * USB power briefly drops while the engine cranks, and Bluetooth can blip, so "off" waits
     * this long before running the Car Off routine. "On" is immediate.
     */
    const val OFF_DEBOUNCE_MS = 20_000L
}
