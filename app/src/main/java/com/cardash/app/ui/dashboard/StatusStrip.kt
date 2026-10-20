package com.cardash.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.battery.BatteryRepository
import com.cardash.app.battery.ChargeLimiter
import com.cardash.app.battery.HeatLevel
import com.cardash.app.battery.HeatRules
import com.cardash.app.camera.CameraRecordService
import com.cardash.app.settings.SettingsStore
import com.cardash.app.theme.HeatColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val ClockFormat = DateTimeFormatter.ofPattern("h:mm")
private val DayFormat = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * Big light clock; date, battery and temperature small on the right (temperature turns amber/red when hot).
 * [compact]: smaller, for the narrow sidebar while navigating.
 */
@Composable
fun StatusStrip(modifier: Modifier = Modifier, compact: Boolean = false) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000)
        }
    }
    val battery by BatteryRepository.info.collectAsStateWithLifecycle()
    val holding by ChargeLimiter.holding.collectAsStateWithLifecycle()
    val recording by CameraRecordService.recordingSide.collectAsStateWithLifecycle()
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val clockSize = if (compact) 30.sp else 46.sp
    val small = if (compact) 12.sp else 14.sp
    val tempColor = when (HeatRules.level(battery.tempC, settings.heatWarnC)) {
        HeatLevel.NORMAL -> muted
        HeatLevel.HOT -> HeatColors.Hot
        HeatLevel.CRITICAL -> HeatColors.Critical
    }

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            now.format(ClockFormat),
            fontSize = clockSize,
            lineHeight = clockSize,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Status icons before the date: red REC while the dashcam records.
                if (recording != null) {
                    Text(if (compact) "● " else "● REC  ", fontSize = small, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                }
                Text(now.format(DayFormat), fontSize = small, color = muted, textAlign = TextAlign.End, maxLines = 1)
            }
            Row {
                // ⏸ = charge limit reached, running on battery until it drops 5%.
                val charge = when {
                    holding -> " ⏸"
                    battery.plugged -> " ⚡"
                    else -> ""
                }
                Text("${battery.levelPercent}%$charge · ", fontSize = small, color = muted, maxLines = 1)
                Text("%.0f°".format(battery.tempC), fontSize = small, color = tempColor, maxLines = 1)
            }
        }
    }
}
