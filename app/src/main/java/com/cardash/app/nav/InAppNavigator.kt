package com.cardash.app.nav

import android.app.Activity
import android.content.Context
import android.util.Log
import com.cardash.app.BuildConfig
import com.cardash.app.location.LocationRepository
import com.cardash.app.settings.SettingsStore
import com.google.android.libraries.navigation.AudioGuidanceSettings
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.Waypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface NavState {
    data object Starting : NavState
    /** Navigation can't be used (key not enabled for the Navigation SDK, terms declined…). */
    data class Unavailable(val reason: String) : NavState
    data object Ready : NavState
    data class Routing(val query: String) : NavState
    data class Guiding(val destination: String) : NavState
}

/**
 * Turn-by-turn inside CarDash via Google's Navigation SDK. Billed per destination
 * (first 1,000/month free), so a route is only requested when the driver picks one.
 */
object InAppNavigator {
    private const val TAG = "InAppNavigator"
    private var navigator: Navigator? = null
    private val _state = MutableStateFlow<NavState>(NavState.Starting)
    val state: StateFlow<NavState> = _state.asStateFlow()

    val isReady: Boolean get() = navigator != null

    /** Needs an Activity: the very first call shows Google's navigation terms dialog. */
    fun init(activity: Activity) {
        if (navigator != null || !BuildConfig.HAS_MAPS_KEY) return
        NavigationApi.getNavigator(activity, object : NavigationApi.NavigatorListener {
            override fun onNavigatorReady(n: Navigator) {
                navigator = n
                n.addArrivalListener { stop() }
                setVoice(SettingsStore.current.voiceGuidance)
                _state.value = if (n.isGuidanceRunning) NavState.Guiding("") else NavState.Ready
            }

            override fun onError(errorCode: Int) {
                _state.value = NavState.Unavailable(
                    when (errorCode) {
                        NavigationApi.ErrorCode.NOT_AUTHORIZED ->
                            "API key isn't enabled for the Navigation SDK"
                        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> "Navigation terms not accepted"
                        NavigationApi.ErrorCode.NETWORK_ERROR -> "No internet connection"
                        NavigationApi.ErrorCode.LOCATION_PERMISSION_MISSING -> "Location permission missing"
                        else -> "Navigation error $errorCode"
                    },
                )
            }
        })
    }

    /** Finds [query] and starts guidance. Returns an error message to show, or null on success. */
    suspend fun navigateTo(
        context: Context,
        query: String,
        picked: PlacesSearch.Suggestion? = null,
        label: String? = null,
    ): String? {
        val n = navigator ?: return "Navigation isn't ready yet"
        _state.value = NavState.Routing(picked?.name ?: label ?: query)
        // A picked suggestion already identifies the place: route by its ID, no extra lookup.
        val place = picked?.let { Place(it.name, 0.0, 0.0, it.placeId) } ?: findPlace(context, query, label)
        // Not the place itself: logs are readable over USB debugging, and destinations reveal where you go.
        Log.i(TAG, if (place == null) "Destination not found" else "Destination found")
        if (place == null) {
            _state.value = NavState.Ready
            return "Couldn't find \"$query\""
        }
        val waypoint = place.toWaypoint()
        val status = suspendCancellableCoroutine { cont ->
            n.setDestination(waypoint, RoutingOptions()) { cont.resume(it.routeStatus) }
        }
        Log.i(TAG, "Route status: $status")
        if (status != Navigator.RouteStatus.OK) {
            _state.value = NavState.Ready
            return when (status) {
                Navigator.RouteStatus.NO_ROUTE_FOUND -> "No route to ${place.label}"
                // Google also reports a key that isn't allowed to use the Navigation SDK as a network error.
                Navigator.RouteStatus.NETWORK_ERROR ->
                    "Route request failed: check the API key has the Navigation SDK enabled"
                Navigator.RouteStatus.QUOTA_CHECK_FAILED -> "API key isn't enabled for the Navigation SDK"
                Navigator.RouteStatus.LOCATION_DISABLED, Navigator.RouteStatus.LOCATION_UNKNOWN ->
                    "Waiting for a GPS fix"
                else -> "Couldn't get a route ($status)"
            }
        }
        n.startGuidance()
        _state.value = NavState.Guiding(place.label)
        return null
    }

    private data class Place(val label: String, val lat: Double, val lon: Double, val placeId: String?) {
        fun toWaypoint(): Waypoint {
            val builder = Waypoint.builder().setTitle(label)
            // Place IDs give better arrival points and ETAs; fall back to coordinates if unsupported.
            placeId?.let {
                try {
                    return builder.setPlaceIdString(it).build()
                } catch (_: Waypoint.UnsupportedPlaceIdException) {
                }
            }
            return builder.setLatLng(lat, lon).build()
        }
    }

    /** Businesses by name via Places; plain addresses via the built-in geocoder as a fallback. */
    private suspend fun findPlace(context: Context, query: String, label: String?): Place? {
        // Pasted coordinates are routed to directly: no search, no Places request.
        Coordinates.parse(query)?.let { return Place(label ?: it.toString(), it.lat, it.lon, null) }
        val near = LocationRepository.fix.value
        PlacesSearch.search(context, query, near)?.let { results ->
            val best = Geocoding.nearest(results.map { Triple(it, it.lat, it.lon) }, near)
            if (best != null) return Place(best.name.ifBlank { query }, best.lat, best.lon, best.placeId.ifBlank { null })
        }
        return Geocoding.find(context, query, near)?.let { Place(it.label, it.lat, it.lon, null) }
    }

    /** Voice on: spoken turn-by-turn. Off: silent, directions stay on screen. Takes effect immediately. */
    fun setVoice(on: Boolean) {
        val mode = if (on) AudioGuidanceSettings.GuidanceMode.VOICE_ALERTS_AND_GUIDANCE else AudioGuidanceSettings.GuidanceMode.SILENT
        navigator?.setAudioGuidanceSettings(AudioGuidanceSettings.builder().setGuidanceMode(mode).build())
    }

    fun stop() {
        navigator?.let {
            it.stopGuidance()
            it.clearDestinations()
        }
        if (navigator != null) _state.value = NavState.Ready
    }
}
