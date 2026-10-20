package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardash.app.nav.InAppNavigator
import com.cardash.app.nav.NavApp
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore

@Composable
fun NavigationSection(s: Settings) {
    SectionTitle("Navigate with")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = s.navigateInApp,
            onClick = { SettingsStore.update { it.copy(navigateInApp = true) } },
            label = { Text("CarDash") },
        )
        NavApp.entries.forEach { app ->
            FilterChip(
                selected = !s.navigateInApp && s.navApp == app,
                onClick = { SettingsStore.update { it.copy(navigateInApp = false, navApp = app) } },
                label = { Text(app.label) },
            )
        }
    }
    Text(
        if (s.navigateInApp) "Turn-by-turn on the dashboard map. If a route fails, opens ${s.navApp.label}."
        else "Search, Home and Work open ${s.navApp.label} navigating. No API key or billing needed.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
    )
    if (s.navigateInApp) {
        // Which app the Maps tile opens, and the fallback when an in-app route fails.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NavApp.entries.forEach { app ->
                FilterChip(
                    selected = s.navApp == app,
                    onClick = { SettingsStore.update { it.copy(navApp = app) } },
                    label = { Text("Backup: ${app.label}") },
                )
            }
        }
        SwitchRow("Voice directions", s.voiceGuidance, "Spoken turn-by-turn while navigating in CarDash") { v ->
            SettingsStore.update { it.copy(voiceGuidance = v) }
            InAppNavigator.setVoice(v)
        }
    }
    OutlinedTextField(
        value = s.home.address,
        // Typing replaces any saved place ID: the text is now what gets routed to.
        onValueChange = { address -> SettingsStore.update { it.copy(home = it.home.copy(address = address, placeId = null)) } },
        label = { Text("Home address") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
    FavouritesEditor(s)
}
