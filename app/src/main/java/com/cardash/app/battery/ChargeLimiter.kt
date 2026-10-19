package com.cardash.app.battery

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Keeps the battery between (limit − 5)% and limit% instead of sitting at 100% in a hot car.
 *
 * The OnePlus 5 can't run off USB with battery charging paused: writing 0 to `charging_enabled`
 * cuts USB power too. So while "holding", the phone runs on battery down to the resume level,
 * then charging is switched back on. Needs root (Magisk asks once).
 */
object ChargeLimiter {
    private const val TAG = "ChargeLimiter"
    private const val NODE = "/sys/class/power_supply/battery/charging_enabled"
    private const val PREFS = "cardash_state"
    private const val KEY_HOLDING = "charge.holding"
    const val RESUME_GAP = 5

    private val _holding = MutableStateFlow(false)
    /** True while CarDash has charging switched off. USB then reads as unplugged. */
    val holding: StateFlow<Boolean> = _holding.asStateFlow()

    private val _rootFailed = MutableStateFlow(false)
    val rootFailed: StateFlow<Boolean> = _rootFailed.asStateFlow()

    enum class Action { PAUSE, RESUME, NONE }

    /** Pure decision, unit-tested. */
    fun decide(enabled: Boolean, level: Int, limit: Int, plugged: Boolean, holding: Boolean): Action = when {
        holding && (!enabled || level <= limit - RESUME_GAP) -> Action.RESUME
        !holding && enabled && plugged && level >= limit -> Action.PAUSE
        else -> Action.NONE
    }

    /** Called from the car service on every battery update. */
    suspend fun onBattery(context: Context, info: BatteryInfo, enabled: Boolean, limit: Int) {
        when (decide(enabled, info.levelPercent, limit, info.plugged, _holding.value)) {
            Action.PAUSE -> {
                // Mark the hold first: cutting charging makes USB read as unplugged immediately.
                setHolding(context, true)
                if (!setCharging(false)) setHolding(context, false)
            }
            Action.RESUME -> release(context)
            Action.NONE -> Unit
        }
    }

    /** Turns charging back on (car off, setting switched off, or a hold left over from a crash/reboot). */
    suspend fun release(context: Context) {
        val wasHolding = _holding.value || prefs(context).getBoolean(KEY_HOLDING, false)
        if (!wasHolding) return
        setCharging(true)
        setHolding(context, false)
    }

    private fun setHolding(context: Context, value: Boolean) {
        _holding.value = value
        prefs(context).edit().putBoolean(KEY_HOLDING, value).apply()
    }

    private suspend fun setCharging(on: Boolean): Boolean = withContext(Dispatchers.IO) {
        val ok = runCatching {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo ${if (on) 1 else 0} > $NODE"))
            p.waitFor() == 0
        }.getOrDefault(false)
        if (!ok) Log.w(TAG, "Couldn't set charging_enabled=$on (root denied?)")
        _rootFailed.value = !ok
        ok
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
