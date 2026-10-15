package com.cardash.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore
import com.cardash.app.settings.ThemeMode

@Composable
fun DisplaySection(s: Settings) {
    SectionTitle("Theme")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            FilterChip(
                selected = s.themeMode == mode,
                onClick = { SettingsStore.update { it.copy(themeMode = mode) } },
                label = {
                    Text(
                        when (mode) {
                            ThemeMode.AUTO -> "Auto (sunset)"
                            ThemeMode.DAY -> "Day"
                            ThemeMode.NIGHT -> "Night"
                        },
                    )
                },
            )
        }
    }

    SectionTitle("Heat alert")
    Text(
        "Warn at ${s.heatWarnC}°C, critical at ${s.heatWarnC + 5}°C",
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 17.sp,
    )
    Slider(
        value = s.heatWarnC.toFloat(),
        onValueChange = { v -> SettingsStore.update { it.copy(heatWarnC = v.toInt()) } },
        valueRange = 38f..50f,
        steps = 11,
    )
}
