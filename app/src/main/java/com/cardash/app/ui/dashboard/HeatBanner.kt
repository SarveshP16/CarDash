package com.cardash.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.battery.BatteryRepository
import com.cardash.app.battery.HeatLevel
import com.cardash.app.battery.HeatRules
import com.cardash.app.settings.SettingsStore
import com.cardash.app.theme.HeatColors

/** Overlays the dashboard while the battery is hot. */
@Composable
fun HeatBanner(modifier: Modifier = Modifier) {
    val battery by BatteryRepository.info.collectAsStateWithLifecycle()
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val level = HeatRules.level(battery.tempC, settings.heatWarnC)

    AnimatedVisibility(level != HeatLevel.NORMAL, modifier) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (level == HeatLevel.CRITICAL) HeatColors.Critical else HeatColors.Hot,
            shadowElevation = 8.dp,
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Color.Black)
                Spacer(Modifier.width(10.dp))
                Text(
                    (if (level == HeatLevel.CRITICAL) "Phone is dangerously hot" else "Phone is getting hot") +
                        " · %.1f°C — shade it or unplug the charger".format(battery.tempC),
                    color = Color.Black,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
