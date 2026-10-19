package com.cardash.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.battery.BatteryRepository
import com.cardash.app.battery.HeatLevel
import com.cardash.app.battery.HeatRules
import com.cardash.app.car.ParkedState
import com.cardash.app.settings.SettingsStore
import com.cardash.app.theme.HeatColors
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

private val ClockFormat = DateTimeFormatter.ofPattern("h:mm")
private val DayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM")
private val Dim = Color(0xFF8A8A8A)

/**
 * Shown after Car Off, always dark: a big clock, battery and temperature, and a countdown to the
 * screen turning off. "Keep screen on" cancels the countdown.
 */
@Composable
fun ParkedScreen(screenOffAt: Long) {
    val context = LocalContext.current
    val battery by BatteryRepository.info.collectAsStateWithLifecycle()
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(250)
        }
    }
    val secondsLeft = ceil((screenOffAt - now).coerceAtLeast(0) / 1000.0).toInt()
    val time = LocalDateTime.now()
    val tempColor = when (HeatRules.level(battery.tempC, settings.heatWarnC)) {
        HeatLevel.NORMAL -> Dim
        HeatLevel.HOT -> HeatColors.Hot
        HeatLevel.CRITICAL -> HeatColors.Critical
    }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black)
            // Swallow taps so nothing underneath gets pressed.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time.format(ClockFormat), fontSize = 120.sp, lineHeight = 120.sp, fontWeight = FontWeight.Light, color = Color(0xFFBDBDBD))
            Text("Parked · ${time.format(DayFormat)}", fontSize = 20.sp, color = Dim)
            Spacer(Modifier.height(10.dp))
            Row {
                Text("Battery ${battery.levelPercent}% · ", fontSize = 18.sp, color = Dim)
                Text("%.0f°C".format(battery.tempC), fontSize = 18.sp, color = tempColor)
            }
            Spacer(Modifier.height(28.dp))
            Text("Screen off in $secondsLeft s", fontSize = 16.sp, color = Dim)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { ParkedState.cancel() }) {
                    Text("Keep screen on", color = Color(0xFFE0E0E0), modifier = Modifier.padding(horizontal = 8.dp))
                }
                OutlinedButton(onClick = { ParkedState.turnOffNow(context) }) {
                    Text("Turn off now", color = Color(0xFFE0E0E0), modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}
