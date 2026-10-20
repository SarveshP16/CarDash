package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.widget.Toast
import com.cardash.app.admin.DeviceOwner
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore
import kotlin.math.roundToInt

@Composable
fun DashcamSection(s: Settings) {
    SectionTitle("Dashcam")
    SwitchRow("Record when the car turns on", s.dashcamAutoRecord, "Stops when the car turns off") { v ->
        SettingsStore.update { it.copy(dashcamAutoRecord = v) }
    }
    if (s.dashcamAutoRecord) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = s.dashcamBackCamera,
                onClick = { SettingsStore.update { it.copy(dashcamBackCamera = true) } },
                label = { Text("Back camera (road)") },
            )
            FilterChip(
                selected = !s.dashcamBackCamera,
                onClick = { SettingsStore.update { it.copy(dashcamBackCamera = false) } },
                label = { Text("Front camera") },
            )
        }
    }
    val context = LocalContext.current
    SwitchRow(
        "Record sound",
        s.dashcamAudio,
        "Includes conversations in the car: let passengers know. Applies from the next recording.",
    ) { v ->
        if (v && !DeviceOwner.grant(context, Manifest.permission.RECORD_AUDIO)) {
            Toast.makeText(context, "Microphone permission missing. Run setup.sh", Toast.LENGTH_LONG).show()
            return@SwitchRow
        }
        SettingsStore.update { it.copy(dashcamAudio = v) }
    }
    SwitchRow(
        "Loop recording",
        s.dashcamLoop,
        "Keeps ${s.dashcamLimitGb} GB (about ${s.dashcamLimitGb * 10} min); deletes the oldest. Impact clips are kept.",
    ) { v -> SettingsStore.update { it.copy(dashcamLoop = v) } }
    if (s.dashcamLoop) {
        Slider(
            value = s.dashcamLimitGb.toFloat(),
            onValueChange = { v -> SettingsStore.update { it.copy(dashcamLimitGb = v.roundToInt()) } },
            valueRange = 2f..32f,
            steps = 14,
        )
    }
    Text(
        "Clips save to Movies/CarDash in 3-minute parts. A hard jolt locks the current part.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
    )
}
