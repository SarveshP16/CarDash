package com.cardash.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Work
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.nav.NavigationLauncher
import com.cardash.app.settings.SettingsStore
import com.cardash.app.ui.components.BigIconButton

/** One-tap buttons that open the chosen nav app already navigating. Unset ones are dimmed. */
@Composable
fun DestinationButtons(modifier: Modifier = Modifier) {
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        settings.destinations.forEach { dest ->
            val icon = when (dest.label.lowercase()) {
                "home" -> Icons.Filled.Home
                "work" -> Icons.Filled.Work
                else -> Icons.Filled.Navigation
            }
            BigIconButton(
                icon = icon,
                label = if (dest.isSet) dest.label else "${dest.label} (set up)",
                active = dest.isSet,
                modifier = Modifier.weight(1f).height(72.dp),
            ) {
                if (dest.isSet) NavigationLauncher.navigate(context, dest.address, settings.navApp)
            }
        }
    }
}
