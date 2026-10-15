package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cardash.app.nav.NavApp
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore

@Composable
fun NavigationSection(s: Settings) {
    SectionTitle("Navigation")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NavApp.entries.forEach { app ->
            FilterChip(
                selected = s.navApp == app,
                onClick = { SettingsStore.update { it.copy(navApp = app) } },
                label = { Text(app.label) },
            )
        }
    }
    s.destinations.forEachIndexed { index, dest ->
        OutlinedTextField(
            value = dest.address,
            onValueChange = { address ->
                SettingsStore.update { st ->
                    st.copy(destinations = st.destinations.toMutableList().also { it[index] = dest.copy(address = address) })
                }
            },
            label = { Text("${dest.label} address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
