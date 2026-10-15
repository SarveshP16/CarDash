package com.cardash.app.battery

import android.content.Intent
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class HeatLevel { NORMAL, HOT, CRITICAL }

data class BatteryInfo(
    val levelPercent: Int = 0,
    val tempC: Float = 0f,
    val plugged: Boolean = false,
)

object HeatRules {
    /** Warn at [warnC]; critical 5 °C above that. */
    fun level(tempC: Float, warnC: Int): HeatLevel = when {
        tempC >= warnC + 5 -> HeatLevel.CRITICAL
        tempC >= warnC -> HeatLevel.HOT
        else -> HeatLevel.NORMAL
    }
}

/** Fed from ACTION_BATTERY_CHANGED by [com.cardash.app.car.CarService]. */
object BatteryRepository {
    private val _info = MutableStateFlow(BatteryInfo())
    val info: StateFlow<BatteryInfo> = _info.asStateFlow()

    fun update(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val info = BatteryInfo(
            levelPercent = if (level < 0) 0 else level * 100 / scale,
            // EXTRA_TEMPERATURE is in tenths of a degree Celsius.
            tempC = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0,
        )
        _info.value = info
        return info
    }
}
