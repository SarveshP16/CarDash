package com.cardash.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.location.LocationRepository
import com.cardash.app.ui.components.DashCard

/** Big GPS speedometer. Shows "--" until there's a fix with speed. */
@Composable
fun SpeedCard(modifier: Modifier = Modifier) {
    val fix by LocationRepository.fix.collectAsStateWithLifecycle()
    DashCard(modifier) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                fix?.speedKmh?.toString() ?: "--",
                fontSize = 150.sp,
                lineHeight = 150.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text("km/h", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
