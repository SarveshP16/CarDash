package com.cardash.app.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.location.LocationRepository
import com.cardash.app.ui.components.TileShape

/** GPS speed overlaid on the map. Shows "--" until there's a fix with speed. */
@Composable
fun SpeedBadge(modifier: Modifier = Modifier) {
    val fix by LocationRepository.fix.collectAsStateWithLifecycle()
    Surface(
        modifier = modifier,
        shape = TileShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                fix?.speedKmh?.toString() ?: "--",
                fontSize = 42.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text("km/h", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
