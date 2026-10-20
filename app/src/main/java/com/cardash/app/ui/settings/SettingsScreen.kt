package com.cardash.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhonelinkRing
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.settings.SettingsStore
import com.cardash.app.ui.components.ScreenHeader
import com.cardash.app.ui.components.TileShape

private enum class SettingsTab(val label: String, val icon: ImageVector) {
    CAR("Car", Icons.Filled.DirectionsCar),
    NAVIGATION("Navigation", Icons.Filled.Navigation),
    MUSIC("Music", Icons.Filled.MusicNote),
    LINK("Phone link", Icons.Filled.PhonelinkRing),
    DASHCAM("Dashcam", Icons.Filled.Videocam),
    BATTERY("Battery", Icons.Filled.BatteryChargingFull),
    SYSTEM("System", Icons.Filled.Settings),
}

/** Tabs down the left (suits landscape), the chosen tab's settings scrolling on the right. */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val s by SettingsStore.settings.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(SettingsTab.CAR) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ScreenHeader("Settings", onBack)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(
                Modifier.width(210.dp).fillMaxHeight().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SettingsTab.entries.forEach { t -> TabItem(t, selected = t == tab) { tab = t } }
            }
            // Keyed so each tab starts scrolled to the top.
            key(tab) {
                Column(
                    Modifier.weight(1f).widthIn(max = 720.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (tab) {
                        SettingsTab.CAR -> {
                            CarDetectionSection(s)
                            RoutineSection(s)
                        }
                        SettingsTab.NAVIGATION -> NavigationSection(s)
                        SettingsTab.MUSIC -> MusicSection(s)
                        SettingsTab.LINK -> LinkSection(s)
                        SettingsTab.DASHCAM -> DashcamSection(s)
                        SettingsTab.BATTERY -> BatterySection(s)
                        SettingsTab.SYSTEM -> {
                            ThemeSection(s)
                            SystemSection(Modifier.fillMaxWidth())
                            LegalSection()
                        }
                    }
                    Spacer(Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TabItem(tab: SettingsTab, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val content = if (selected) colors.onPrimary else colors.onSurface
    Row(
        Modifier.fillMaxWidth()
            .clip(TileShape)
            .background(if (selected) colors.primary else colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(tab.icon, null, tint = content, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(tab.label, color = content, fontSize = 17.sp)
    }
}
