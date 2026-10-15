package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.settings.SettingsStore
import com.cardash.app.ui.components.ScreenHeader

/** Two columns to suit landscape: car + routines on the left; navigation, display, system on the right. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Settings", onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CarDetectionSection(s)
                RoutineSection(s)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NavigationSection(s)
                DisplaySection(s)
                SystemSection(Modifier.fillMaxWidth())
            }
        }
    }
}
