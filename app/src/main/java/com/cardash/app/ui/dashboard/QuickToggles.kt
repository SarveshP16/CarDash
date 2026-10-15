package com.cardash.app.ui.dashboard

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.car.CarRepository
import com.cardash.app.car.CarService
import com.cardash.app.car.CarState
import com.cardash.app.system.SystemToggles
import com.cardash.app.ui.components.BigIconButton
import kotlinx.coroutines.delay

private data class ToggleStates(val wifi: Boolean, val bluetooth: Boolean, val dnd: Boolean, val autoBrightness: Boolean)

private fun readStates(context: Context) = ToggleStates(
    wifi = SystemToggles.isWifiOn(context),
    bluetooth = SystemToggles.isBluetoothOn(context),
    dnd = SystemToggles.isDndOn(context),
    autoBrightness = SystemToggles.isAutoBrightness(context),
)

/** Wi-Fi, Bluetooth, DND, brightness, manual car mode, apps and settings. */
@Composable
fun QuickToggles(onOpenApps: () -> Unit, onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    // System state can change outside the app (e.g. a routine), so poll every couple of seconds.
    val states by produceState(readStates(context), refresh) {
        while (true) {
            value = readStates(context)
            delay(2_000)
        }
    }
    val car by CarRepository.state.collectAsStateWithLifecycle()
    fun toggle(action: () -> Unit) { action(); refresh++ }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val buttonModifier = Modifier.weight(1f).height(64.dp)
        BigIconButton(Icons.Filled.Wifi, null, buttonModifier, states.wifi) {
            toggle { SystemToggles.setWifi(context, !states.wifi) }
        }
        BigIconButton(Icons.Filled.Bluetooth, null, buttonModifier, states.bluetooth) {
            toggle { SystemToggles.setBluetooth(context, !states.bluetooth) }
        }
        BigIconButton(Icons.Filled.DoNotDisturbOn, null, buttonModifier, states.dnd) {
            toggle { SystemToggles.setDnd(context, !states.dnd) }
        }
        BigIconButton(
            if (states.autoBrightness) Icons.Filled.BrightnessAuto else Icons.Filled.BrightnessLow,
            null, buttonModifier, states.autoBrightness,
        ) {
            // Auto ↔ dim (for night driving).
            toggle {
                if (states.autoBrightness) SystemToggles.setBrightness(context, 40)
                else SystemToggles.setAutoBrightness(context, true)
            }
        }
        // Manual car mode, for testing without the car: forces On/Off; long-term the signals decide.
        BigIconButton(Icons.Filled.PowerSettingsNew, null, buttonModifier, car == CarState.ON) {
            CarService.start(
                context,
                if (car == CarState.ON) CarService.ACTION_FORCE_OFF else CarService.ACTION_FORCE_ON,
            )
        }
        BigIconButton(Icons.Filled.Apps, null, buttonModifier, onClick = onOpenApps)
        BigIconButton(Icons.Filled.Settings, null, buttonModifier, onClick = onOpenSettings)
    }
}
