package com.cardash.app.car

import org.junit.Assert.assertEquals
import org.junit.Test

class CarStateLogicTest {
    private fun resolve(powered: Boolean, bt: Boolean, alwaysOn: Boolean = false, manual: CarState? = null) =
        CarStateLogic.resolve(CarSignals(powered, bt, manual), alwaysOn)

    @Test fun `charger alone turns the car on`() = assertEquals(CarState.ON, resolve(powered = true, bt = false))

    @Test fun `bluetooth alone turns the car on`() = assertEquals(CarState.ON, resolve(powered = false, bt = true))

    @Test fun `no signals means off`() = assertEquals(CarState.OFF, resolve(powered = false, bt = false))

    @Test fun `always-on charger is ignored`() =
        assertEquals(CarState.OFF, resolve(powered = true, bt = false, alwaysOn = true))

    @Test fun `always-on charger still lets bluetooth decide`() =
        assertEquals(CarState.ON, resolve(powered = true, bt = true, alwaysOn = true))

    @Test fun `manual override wins`() {
        assertEquals(CarState.OFF, resolve(powered = true, bt = true, manual = CarState.OFF))
        assertEquals(CarState.ON, resolve(powered = false, bt = false, manual = CarState.ON))
    }
}
