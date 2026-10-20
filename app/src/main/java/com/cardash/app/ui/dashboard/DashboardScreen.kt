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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.BuildConfig
import com.cardash.app.nav.InAppNavigator
import com.cardash.app.nav.NavState

/**
 * Landscape, minimal. Idle: the map fills 60%; on the right the clock line, a larger now-playing
 * tile, then Maps | Home | Fav, system toggles, and cameras/apps/settings. While navigating the
 * map slides to 75% and the right side becomes a slim sidebar (music, cameras, apps, settings, End).
 */
@Composable
fun DashboardScreen(night: Boolean, onOpenApps: () -> Unit, onOpenSettings: () -> Unit) {
    val guiding = InAppNavigator.state.collectAsStateWithLifecycle().value is NavState.Guiding
    // One Row whose weights animate, so the map view is never recreated when the layout switches.
    val mapShare by animateFloatAsState(if (guiding) 0.75f else 0.6f, tween(400), label = "mapShare")
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val left = Modifier.weight(mapShare).fillMaxHeight()
            if (BuildConfig.HAS_MAPS_KEY) {
                NavigationPanel(night = night, modifier = left)
            } else {
                Column(left, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DestinationSearch(Modifier.fillMaxWidth())
                    SpeedCard(Modifier.fillMaxWidth().weight(1f))
                }
            }
            if (guiding) {
                GuidingSidebar(onOpenApps, onOpenSettings, Modifier.weight(1f - mapShare).fillMaxHeight())
            } else Column(
                Modifier.weight(1f - mapShare).fillMaxHeight(),
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
