package com.cardash.app.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class SunCalcTest {
    private val sydney = ZoneId.of("Australia/Sydney")
    private val lat = -33.8688
    private val lon = 151.2093

    private fun assertNear(expected: ZonedDateTime, actual: java.time.Instant) {
        val diff = Duration.between(expected.toInstant(), actual).abs()
        assertTrue("expected ~$expected, got ${actual.atZone(expected.zone)}", diff <= Duration.ofMinutes(5))
    }

    @Test fun `sydney winter solstice`() {
        // Published times: sunrise 07:00, sunset 16:54 AEST.
        val t = SunCalc.sunTimes(LocalDate.of(2026, 6, 21), lat, lon)!!
        assertNear(ZonedDateTime.of(2026, 6, 21, 7, 0, 0, 0, sydney), t.sunrise)
        assertNear(ZonedDateTime.of(2026, 6, 21, 16, 54, 0, 0, sydney), t.sunset)
    }

    @Test fun `sydney summer solstice`() {
        // Published times: sunrise 05:41, sunset 20:05 AEDT.
        val t = SunCalc.sunTimes(LocalDate.of(2026, 12, 21), lat, lon)!!
        assertNear(ZonedDateTime.of(2026, 12, 21, 5, 41, 0, 0, sydney), t.sunrise)
        assertNear(ZonedDateTime.of(2026, 12, 21, 20, 5, 0, 0, sydney), t.sunset)
    }

    @Test fun `noon is day and midnight is night`() {
        val noon = ZonedDateTime.of(2026, 10, 16, 12, 0, 0, 0, sydney).toInstant()
        val midnight = ZonedDateTime.of(2026, 10, 16, 23, 30, 0, 0, sydney).toInstant()
        assertFalse(SunCalc.isNight(noon, lat, lon, sydney))
        assertTrue(SunCalc.isNight(midnight, lat, lon, sydney))
    }

    @Test fun `wrong phone time zone still gives the right answer`() {
        // Sydney mid-morning, but the phone thinks it's in New York (evening, previous date).
        val sydneyMorning = ZonedDateTime.of(2026, 10, 16, 10, 30, 0, 0, sydney).toInstant()
        val sydneyNight = ZonedDateTime.of(2026, 10, 16, 22, 30, 0, 0, sydney).toInstant()
        val newYork = ZoneId.of("America/New_York")
        assertFalse(SunCalc.isNight(sydneyMorning, lat, lon, newYork))
        assertTrue(SunCalc.isNight(sydneyNight, lat, lon, newYork))
    }

    @Test fun `polar night has no sun times`() {
        assertNull(SunCalc.sunTimes(LocalDate.of(2026, 12, 21), 78.2, 15.6))
    }
}
