package com.cardash.app

import com.cardash.app.battery.HeatLevel
import com.cardash.app.battery.HeatRules
import com.cardash.app.location.SpeedMath
import com.cardash.app.nav.Destination
import org.junit.Assert.assertEquals
import org.junit.Test

class MiscLogicTest {
    @Test fun `heat levels`() {
        assertEquals(HeatLevel.NORMAL, HeatRules.level(44.9f, 45))
        assertEquals(HeatLevel.HOT, HeatRules.level(45f, 45))
        assertEquals(HeatLevel.CRITICAL, HeatRules.level(50f, 45))
    }

    @Test fun `speed conversion hides gps jitter`() {
        assertEquals(0, SpeedMath.toKmh(0.5f))
        assertEquals(60, SpeedMath.toKmh(16.67f))
    }

    @Test fun `destinations round-trip and strip separators`() {
        val list = listOf(Destination("Home", "1 George St, Sydney"), Destination("Gym\t", "Line\nbreak"))
        val decoded = Destination.decodeList(Destination.encodeList(list))
        assertEquals(listOf(Destination("Home", "1 George St, Sydney"), Destination("Gym", "Line break")), decoded)
    }

    @Test fun `empty address round-trips`() {
        val list = listOf(Destination("Work", ""))
        assertEquals(list, Destination.decodeList(Destination.encodeList(list)))
    }
}
