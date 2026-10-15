package com.cardash.app.theme

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

/** Sunrise/sunset via the standard "sunrise equation" (accurate to a minute or two; plenty for a theme switch). */
object SunCalc {
    data class SunTimes(val sunrise: Instant, val sunset: Instant)

    /** Null during polar day or polar night. */
    fun sunTimes(date: LocalDate, lat: Double, lon: Double): SunTimes? {
        // Days since J2000 (2000-01-01 12:00 UTC); 10957 = epoch day of 2000-01-01.
        val n = (date.toEpochDay() - 10957).toDouble() + 0.0008
        val jStar = n - lon / 360.0
        val m = (357.5291 + 0.98560028 * jStar).mod(360.0)
        val mRad = m.rad
        val c = 1.9148 * sin(mRad) + 0.02 * sin(2 * mRad) + 0.0003 * sin(3 * mRad)
        val lambda = (m + c + 180 + 102.9372).mod(360.0).rad
        val jTransit = 2451545.0 + jStar + 0.0053 * sin(mRad) - 0.0069 * sin(2 * lambda)
        val declination = asin(sin(lambda) * sin(23.4397.rad))
        val cosH = (sin((-0.833).rad) - sin(lat.rad) * sin(declination)) / (cos(lat.rad) * cos(declination))
        if (abs(cosH) > 1) return null
        val h = acos(cosH) / (2 * PI) // fraction of a day
        return SunTimes(julianToInstant(jTransit - h), julianToInstant(jTransit + h))
    }

    fun isNight(now: Instant, lat: Double, lon: Double, zone: ZoneId): Boolean {
        val times = sunTimes(now.atZone(zone).toLocalDate(), lat, lon) ?: return false
        return now.isBefore(times.sunrise) || now.isAfter(times.sunset)
    }

    private fun julianToInstant(jd: Double): Instant =
        Instant.ofEpochMilli(((jd - 2440587.5) * 86_400_000.0).toLong())

    private val Double.rad get() = this * PI / 180.0
}
