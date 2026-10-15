package com.cardash.app.ui.settings

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore

private data class PairedDevice(val name: String, val address: String)

@SuppressLint("MissingPermission")
private fun pairedDevices(context: Context): List<PairedDevice> =
    context.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices.orEmpty()
        .map { PairedDevice(it.name ?: it.address, it.address) }
        .sortedBy { it.name.lowercase() }

/** Pick the car's hands-free device, and say whether the charger socket switches off with the engine. */
@Composable
fun CarDetectionSection(s: Settings) {
    val context = LocalContext.current
    val devices = remember { pairedDevices(context) }

    SectionTitle("Car detection")
    Text(
        "Car's Bluetooth (pair it in Android Bluetooth settings first)",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
    )
    if (devices.isEmpty()) {
        Text("No paired devices", color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp)
    }
    (listOf<PairedDevice?>(null) + devices).forEach { device ->
        val selected = device?.address == s.carBluetoothAddress
        Row(
            Modifier.fillMaxWidth()
                .clickable {
                    SettingsStore.update { it.copy(carBluetoothAddress = device?.address, carBluetoothName = device?.name) }
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(
                device?.name ?: "None (charger only)",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 17.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    SwitchRow(
        "Charger stays on with engine off",
        s.chargerAlwaysOn,
        "Then only Bluetooth decides when the car is on",
    ) { v -> SettingsStore.update { it.copy(chargerAlwaysOn = v) } }
}
