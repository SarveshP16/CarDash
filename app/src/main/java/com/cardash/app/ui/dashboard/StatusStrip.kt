package com.cardash.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
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
import com.cardash.app.car.CarRepository
import com.cardash.app.car.CarState
import com.cardash.app.settings.SettingsStore
import com.cardash.app.theme.HeatColors
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ClockFormat = DateTimeFormatter.ofPattern("h:mm")

/** Clock, car state, battery and battery temperature. */
@Composable
fun StatusStrip(modifier: Modifier = Modifier) {
    val time by produceState(LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    val car by CarRepository.state.collectAsStateWithLifecycle()
    val battery by BatteryRepository.info.collectAsStateWithLifecycle()
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val heat = HeatRules.level(battery.tempC, settings.heatWarnC)
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            time.format(ClockFormat),
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        Chip(
            Icons.Filled.DirectionsCar,
            if (car == CarState.ON) "On" else "Off",
            if (car == CarState.ON) MaterialTheme.colorScheme.primary else dim,
        )
        Chip(
            if (battery.plugged) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
            "${battery.levelPercent}%",
            dim,
        )
        Chip(
            Icons.Filled.Thermostat,
            "%.0f°C".format(battery.tempC),
            when (heat) {
                HeatLevel.NORMAL -> dim
                HeatLevel.HOT -> HeatColors.Hot
                HeatLevel.CRITICAL -> HeatColors.Critical
            },
        )
    }
}

@Composable
private fun Chip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, color = color, fontSize = 16.sp)
    }
}
