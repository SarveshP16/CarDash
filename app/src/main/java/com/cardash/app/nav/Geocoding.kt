package com.cardash.app.nav

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.cardash.app.location.Fix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.cos
import kotlin.math.pow

/**
 * Turns typed/spoken text into coordinates with Android's built-in Geocoder (served by Google
 * Play services, no Places API bill). Results near the car are preferred, so "Bunnings" finds
 * the closest store rather than one in another state.
 */
object Geocoding {
    data class Place(val label: String, val lat: Double, val lon: Double)

    /** ~50 km around the car. */
    private const val NEARBY_DEGREES = 0.5

    suspend fun find(context: Context, query: String, near: Fix?): Place? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val nearby = near?.let { lookup(geocoder, query, it) }.orEmpty()
        val results = nearby.ifEmpty { lookup(geocoder, query, null) }
        val best = nearest(results.map { Triple(it, it.latitude, it.longitude) }, near) ?: return null
        return Place(label(best, query), best.latitude, best.longitude)
    }

    /** Picks the candidate closest to [near] (or the first, without a fix). */
    fun <T> nearest(candidates: List<Triple<T, Double, Double>>, near: Fix?): T? {
        if (near == null) return candidates.firstOrNull()?.first
        // Equirectangular distance is plenty for ranking places within a city.
        val scale = cos(Math.toRadians(near.lat))
        return candidates.minByOrNull { (_, lat, lon) ->
            (lat - near.lat).pow(2) + ((lon - near.lon) * scale).pow(2)
        }?.first
    }

    private fun label(address: Address, query: String): String {
        val name = address.featureName?.takeIf { it.isNotBlank() && !it.all(Char::isDigit) }
        val street = address.thoroughfare
        return when {
            name != null && street != null && name != street -> "$name, $street"
            name != null -> name
            else -> address.getAddressLine(0) ?: query
        }
    }

    private suspend fun lookup(geocoder: Geocoder, query: String, near: Fix?): List<Address> {
        val box = near?.let {
            doubleArrayOf(it.lat - NEARBY_DEGREES, it.lon - NEARBY_DEGREES, it.lat + NEARBY_DEGREES, it.lon + NEARBY_DEGREES)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                val listener = object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                    override fun onError(errorMessage: String?) = cont.resume(emptyList())
                }
                if (box == null) geocoder.getFromLocationName(query, 5, listener)
                else geocoder.getFromLocationName(query, 5, box[0], box[1], box[2], box[3], listener)
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                    if (box == null) geocoder.getFromLocationName(query, 5)
                    else geocoder.getFromLocationName(query, 5, box[0], box[1], box[2], box[3])
                }.getOrNull().orEmpty()
            }
        }
    }
}
