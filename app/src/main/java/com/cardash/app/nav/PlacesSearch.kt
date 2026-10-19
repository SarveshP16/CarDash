package com.cardash.app.nav

import android.content.Context
import android.util.Log
import com.cardash.app.BuildConfig
import com.cardash.app.location.Fix
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.SearchByTextRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Finds businesses and places by name ("Bunnings", "Westfield Parramatta") with the Places SDK
 * (Places API (New) Text Search), biased to ~50 km around the car. Text Search Pro: 5,000 free
 * requests a month. The SDK signs requests with the app's identity, which the Android-restricted
 * key requires. Returns null on any failure so the caller can fall back to the geocoder.
 */
object PlacesSearch {
    private const val TAG = "PlacesSearch"
    private const val BIAS_RADIUS_M = 50_000.0
    private val FIELDS = listOf(Place.Field.ID, Place.Field.DISPLAY_NAME, Place.Field.FORMATTED_ADDRESS, Place.Field.LOCATION)

    data class Result(val placeId: String, val name: String, val lat: Double, val lon: Double)

    /** A live suggestion while typing. [distanceMeters] is from the car, when known. */
    data class Suggestion(val placeId: String, val name: String, val detail: String, val distanceMeters: Int?) {
        /** Name plus address, for handing off to Google Maps / Waze. */
        val fullText: String get() = if (detail.isBlank()) name else "$name, $detail"
    }

    private fun ensureInit(context: Context): Context {
        val app = context.applicationContext
        if (!Places.isInitialized()) Places.initializeWithNewPlacesApiEnabled(app, BuildConfig.MAPS_API_KEY)
        return app
    }

    /** One session token per search box opening, so Google groups the keystrokes of one search. */
    fun newSession(): AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    /**
     * Places Autocomplete (10,000 free requests a month). Biased to ~50 km around the car and
     * limited to Australia. Returns null on failure (offline, API not enabled).
     */
    suspend fun suggest(context: Context, query: String, near: Fix?, session: AutocompleteSessionToken): List<Suggestion>? {
        if (!BuildConfig.HAS_MAPS_KEY) return null
        val app = ensureInit(context)
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .setSessionToken(session)
            .setCountries("AU")
            .apply {
                near?.let {
                    val here = LatLng(it.lat, it.lon)
                    setOrigin(here)
                    setLocationBias(CircularBounds.newInstance(here, BIAS_RADIUS_M))
                }
            }
            .build()
        return suspendCancellableCoroutine { cont ->
            Places.createClient(app).findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    cont.resume(
                        response.autocompletePredictions.map {
                            Suggestion(
                                placeId = it.placeId,
                                name = it.getPrimaryText(null).toString(),
                                detail = it.getSecondaryText(null).toString(),
                                distanceMeters = it.distanceMeters,
                            )
                        },
                    )
                }
                .addOnFailureListener {
                    Log.w(TAG, "Autocomplete failed", it)
                    cont.resume(null)
                }
        }
    }

    suspend fun search(context: Context, query: String, near: Fix?): List<Result>? {
        if (!BuildConfig.HAS_MAPS_KEY) return null
        val app = ensureInit(context)
        val request = SearchByTextRequest.builder(query, FIELDS)
            .setMaxResultCount(5)
            .apply { near?.let { setLocationBias(CircularBounds.newInstance(LatLng(it.lat, it.lon), BIAS_RADIUS_M)) } }
            .build()
        return suspendCancellableCoroutine { cont ->
            Places.createClient(app).searchByText(request)
                .addOnSuccessListener { response ->
                    cont.resume(
                        response.places.mapNotNull { p ->
                            val loc = p.location ?: return@mapNotNull null
                            Result(p.id.orEmpty(), p.displayName ?: p.formattedAddress.orEmpty(), loc.latitude, loc.longitude)
                        },
                    )
                }
                .addOnFailureListener {
                    Log.w(TAG, "Places text search failed", it)
                    cont.resume(null)
                }
        }
    }
}
