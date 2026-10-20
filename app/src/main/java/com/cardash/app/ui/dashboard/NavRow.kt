package com.cardash.app.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.nav.NavigationLauncher
import com.cardash.app.nav.RouteStarter
import com.cardash.app.settings.SettingsStore
import com.cardash.app.ui.components.BigIconButton

/**
 * Maps | Home | Fav as equal tiles. Maps opens the nav app (accent tile), Home starts the route
 * home, Fav opens the list of saved places. Only shown when not navigating (the sidebar has End).
 */
@Composable
fun NavRow(modifier: Modifier = Modifier) {
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showFavourites by remember { mutableStateOf(false) }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val tile = Modifier.weight(1f).fillMaxHeight()
        BigIconButton(Icons.Filled.Map, settings.navApp.shortLabel, tile, highlighted = true, horizontal = true) {
            if (!NavigationLauncher.open(context, settings.navApp)) {
                Toast.makeText(context, "${settings.navApp.label} isn't installed", Toast.LENGTH_LONG).show()
            }
        }
        val home = settings.home
        BigIconButton(Icons.Filled.Home, "Home", tile, active = home.isSet, horizontal = true) {
            if (home.isSet) RouteStarter.go(context, scope, home.address, home.asSuggestion())
            else Toast.makeText(context, "Set your home address in Settings → Navigation", Toast.LENGTH_LONG).show()
        }
        BigIconButton(Icons.Filled.Star, "Fav", tile, active = settings.favourites.isNotEmpty(), horizontal = true) {
            showFavourites = true
        }
    }

    if (showFavourites) {
        FavouritesPanel(
            favourites = settings.favourites,
            onPick = { place ->
                showFavourites = false
                RouteStarter.go(context, scope, place.address, place.asSuggestion())
            },
            onDismiss = { showFavourites = false },
        )
    }
}
