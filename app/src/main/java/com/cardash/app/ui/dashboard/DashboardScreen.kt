package com.cardash.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cardash.app.BuildConfig

/**
 * Landscape, minimal: the map fills 60% on the left. On the right, the clock line, a larger
 * now-playing tile, then three equal rows: Maps | Home | Work, system toggles, and cameras/apps/settings.
 */
@Composable
fun DashboardScreen(night: Boolean, onOpenApps: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val left = Modifier.weight(0.6f).fillMaxHeight()
            if (BuildConfig.HAS_MAPS_KEY) {
                NavigationPanel(night = night, modifier = left)
            } else {
                Column(left, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DestinationSearch(Modifier.fillMaxWidth())
                    SpeedCard(Modifier.fillMaxWidth().weight(1f))
                }
            }
            Column(
                Modifier.weight(0.4f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusStrip(Modifier.fillMaxWidth().padding(horizontal = 4.dp))
                // Music gets the most room; the three button rows share the rest equally.
                NowPlayingCard(Modifier.fillMaxWidth().weight(1.7f))
                NavRow(Modifier.fillMaxWidth().weight(1f))
                SystemToggleRow(Modifier.fillMaxWidth().weight(1f))
                CameraAppsRow(
                    onOpenApps = onOpenApps,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
        HeatBanner(Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
    }
}
