package com.cardash.app.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.nav.InAppNavigator
import com.cardash.app.nav.NavState
import com.cardash.app.nav.NavigationLauncher
import com.cardash.app.nav.RouteStarter
import com.cardash.app.settings.SettingsStore
import com.cardash.app.ui.components.BigIconButton

/**
 * Maps | Home | Work as equal tiles. Maps opens the Google Maps app (accent tile); while CarDash
 * is navigating it becomes "End". Destinations start turn-by-turn; unset ones are dimmed.
 */
@Composable
fun NavRow(modifier: Modifier = Modifier) {
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val guiding = InAppNavigator.state.collectAsStateWithLifecycle().value is NavState.Guiding
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val error = MaterialTheme.colorScheme.error

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val tile = Modifier.weight(1f).fillMaxHeight()
        if (guiding) {
            BigIconButton(Icons.Filled.Close, "End", tile, container = error, horizontal = true) { InAppNavigator.stop() }
        } else {
            BigIconButton(Icons.Filled.Map, settings.navApp.shortLabel, tile, highlighted = true, horizontal = true) {
                if (!NavigationLauncher.open(context, settings.navApp)) {
                    Toast.makeText(context, "${settings.navApp.label} isn't installed", Toast.LENGTH_LONG).show()
                }
            }
        }
        settings.destinations.forEach { dest ->
            val icon = when (dest.label.lowercase()) {
                "home" -> Icons.Filled.Home
                "work" -> Icons.Filled.Work
                else -> Icons.Filled.Navigation
            }
            // Unset destinations stay labelled but dimmed; set them in Settings → Navigation.
            BigIconButton(icon, dest.label, tile, active = dest.isSet, horizontal = true) {
                if (dest.isSet) RouteStarter.go(context, scope, dest.address)
            }
        }
    }
}
