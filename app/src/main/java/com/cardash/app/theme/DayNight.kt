package com.cardash.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.cardash.app.location.Fix
import com.cardash.app.settings.ThemeMode
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

object DayNight {
    // Sydney, used until the phone has had its first GPS fix.
    private const val FALLBACK_LAT = -33.8688
    private const val FALLBACK_LON = 151.2093

    fun isNight(mode: ThemeMode, fix: Fix?, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) =
        when (mode) {
            ThemeMode.DAY -> false
            ThemeMode.NIGHT -> true
            ThemeMode.AUTO -> SunCalc.isNight(now, fix?.lat ?: FALLBACK_LAT, fix?.lon ?: FALLBACK_LON, zone)
        }
}

/** Re-checks once a minute so the theme flips at sunrise/sunset without user action. */
@Composable
fun rememberIsNight(mode: ThemeMode, fix: Fix?): Boolean {
    // GPS updates every second; ~10 km precision is plenty for sun times and avoids restarting the loop.
    val coarse = fix?.let { Fix(Math.round(it.lat * 10) / 10.0, Math.round(it.lon * 10) / 10.0, null) }
    val night by produceState(DayNight.isNight(mode, coarse), mode, coarse) {
        while (true) {
            value = DayNight.isNight(mode, coarse)
            delay(60_000)
        }
    }
    return night
}
