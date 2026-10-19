package com.cardash.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** [bearing] (degrees) is only set while moving, so a parked car's map doesn't spin on GPS noise. */
data class Fix(val lat: Double, val lon: Double, val speedKmh: Int?, val bearing: Float? = null)

/**
 * Platform GPS, so speed and sun times don't depend on Google Play services.
 * Only runs while the dashboard is visible, so a parked phone isn't heated by GPS.
 */
object LocationRepository {
    private val _fix = MutableStateFlow<Fix?>(null)
    val fix: StateFlow<Fix?> = _fix.asStateFlow()

    private var manager: LocationManager? = null

    private val listener = LocationListener { location -> _fix.value = location.toFix() }

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start(context: Context) {
        if (manager != null || !hasPermission(context)) return
        val lm = context.applicationContext.getSystemService(LocationManager::class.java)
        if (_fix.value == null) {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?.let { _fix.value = it.toFix().copy(speedKmh = null, bearing = null) }
        }
        runCatching {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0f, listener, Looper.getMainLooper())
            manager = lm
        }
    }

    fun stop() {
        manager?.removeUpdates(listener)
        manager = null
        // Keep the last position (for sunrise/sunset), but forget the speed.
        _fix.value = _fix.value?.copy(speedKmh = null, bearing = null)
    }

    private fun Location.toFix() = Fix(
        lat = latitude,
        lon = longitude,
        speedKmh = if (hasSpeed()) SpeedMath.toKmh(speed) else null,
        bearing = if (hasBearing() && hasSpeed() && SpeedMath.toKmh(speed) > 0) bearing else null,
    )
}

object SpeedMath {
    /** m/s → km/h, rounded; GPS jitter under ~3 km/h shows as 0 so a parked car doesn't read "2". */
    fun toKmh(metersPerSecond: Float): Int {
        val kmh = metersPerSecond * 3.6f
        return if (kmh < 3f) 0 else Math.round(kmh)
    }
}
