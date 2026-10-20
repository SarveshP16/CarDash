package com.cardash.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cardash.app.camera.CamSide
import com.cardash.app.nav.InAppNavigator
import com.cardash.app.ui.components.BigIconButton

/**
 * The 25% column while navigating: clock, date, battery and temperature, compact now playing,
 * front/back camera, Apps and Settings, and End route at the bottom.
 */
@Composable
fun GuidingSidebar(onOpenApps: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusStrip(Modifier.fillMaxWidth().padding(horizontal = 4.dp), compact = true)
        NowPlayingCard(Modifier.fillMaxWidth().weight(1.6f), compact = true)
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CameraButton(CamSide.FRONT, Modifier.weight(1f).fillMaxHeight())
            CameraButton(CamSide.BACK, Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigIconButton(Icons.Filled.Apps, null, Modifier.weight(1f).fillMaxHeight(), onClick = onOpenApps)
            BigIconButton(Icons.Filled.Settings, null, Modifier.weight(1f).fillMaxHeight(), onClick = onOpenSettings)
        }
        BigIconButton(
            Icons.Filled.Close, "End route", Modifier.fillMaxWidth().weight(0.8f),
            container = MaterialTheme.colorScheme.error, horizontal = true,
        ) { InAppNavigator.stop() }
    }
}
