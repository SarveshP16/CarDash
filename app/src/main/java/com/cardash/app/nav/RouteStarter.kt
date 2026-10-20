package com.cardash.app.nav

import android.content.Context
import android.widget.Toast
import com.cardash.app.settings.Recents
import com.cardash.app.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One entry point for "take me to X", following Settings → Navigate with:
 * - CarDash: in-app turn-by-turn; if that fails (no route, billing, offline) it hands off to the app.
 * - Google Maps / Waze: open that app already navigating (no API key or billing needed).
 */
object RouteStarter {
    /** [label]: a name to show for [query] when it has none of its own (e.g. coordinates sent from the phone). */
    fun go(
        context: Context,
        scope: CoroutineScope,
        query: String,
        picked: PlacesSearch.Suggestion? = null,
        label: String? = null,
    ) {
        // Apps get the suggestion's name and address, which they resolve to the same place.
        val q = (picked?.fullText ?: query).trim()
        if (q.isEmpty()) return
        Recents.add(Destination(picked?.name ?: label ?: q, q, picked?.placeId))
        val s = SettingsStore.current
        if (s.navigateInApp && InAppNavigator.isReady) {
            scope.launch {
                val error = InAppNavigator.navigateTo(context, q, picked, label) ?: return@launch
                Toast.makeText(context, "$error. Opening ${s.navApp.label}.", Toast.LENGTH_LONG).show()
                handOff(context, q)
            }
            return
        }
        handOff(context, q)
    }

    private fun handOff(context: Context, query: String) {
        val app = SettingsStore.current.navApp
        if (!NavigationLauncher.navigate(context, query, app)) {
            Toast.makeText(context, "${app.label} isn't installed", Toast.LENGTH_LONG).show()
        }
    }
}
