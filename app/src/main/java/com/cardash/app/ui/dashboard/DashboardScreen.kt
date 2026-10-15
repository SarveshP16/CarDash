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

/**
 * Landscape layout: map with the speed overlaid on the left; status, now playing,
 * destinations and quick toggles stacked on the right.
 */
@Composable
fun DashboardScreen(night: Boolean, onOpenApps: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.weight(0.56f).fillMaxHeight()) {
                MapPanel(night = night, modifier = Modifier.fillMaxSize())
                SpeedBadge(Modifier.align(Alignment.BottomStart).padding(12.dp))
            }
            Column(
                Modifier.weight(0.44f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatusStrip(Modifier.fillMaxWidth())
                NowPlayingCard(Modifier.fillMaxWidth().weight(1f))
                DestinationButtons(Modifier.fillMaxWidth())
                QuickToggles(
                    onOpenApps = onOpenApps,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        HeatBanner(Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
    }
}
