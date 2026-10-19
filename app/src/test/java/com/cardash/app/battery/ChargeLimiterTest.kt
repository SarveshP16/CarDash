package com.cardash.app.battery

import com.cardash.app.battery.ChargeLimiter.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class ChargeLimiterTest {
    private fun decide(level: Int, holding: Boolean, enabled: Boolean = true, plugged: Boolean = true) =
        ChargeLimiter.decide(enabled, level, limit = 80, plugged = plugged, holding = holding)

    @Test fun `pauses at the limit while plugged in`() = assertEquals(Action.PAUSE, decide(80, holding = false))

    @Test fun `keeps charging below the limit`() = assertEquals(Action.NONE, decide(79, holding = false))

    @Test fun `does nothing when not plugged in`() = assertEquals(Action.NONE, decide(90, holding = false, plugged = false))

    @Test fun `holds between resume level and limit`() = assertEquals(Action.NONE, decide(77, holding = true))

    @Test fun `resumes 5 percent below the limit`() = assertEquals(Action.RESUME, decide(75, holding = true))

    @Test fun `switching the setting off resumes charging`() =
        assertEquals(Action.RESUME, decide(80, holding = true, enabled = false))

    @Test fun `disabled never pauses`() = assertEquals(Action.NONE, decide(100, holding = false, enabled = false))
}
