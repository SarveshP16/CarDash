package com.cardash.app.settings

import com.cardash.app.nav.Destination
import com.cardash.app.nav.NavApp

enum class ThemeMode { AUTO, DAY, NIGHT }

data class Settings(
    /** MAC address of the car's hands-free Bluetooth device; null until picked in Settings. */
    val carBluetoothAddress: String? = null,
    val carBluetoothName: String? = null,
    /** True when the car's USB/12V socket stays powered with the engine off: then only Bluetooth decides "car off". */
    val chargerAlwaysOn: Boolean = false,

    // Car On routine
    val wifiOnCarOn: Boolean = true,
    val dndWhileDriving: Boolean = true,
    val autoBrightnessOnCarOn: Boolean = true,

    // Car Off routine (Bluetooth is never turned off: it's needed to detect the car next time)
    val pauseMediaOnCarOff: Boolean = true,
    val wifiOffOnCarOff: Boolean = true,
    val screenOffOnCarOff: Boolean = true,

    val navApp: NavApp = NavApp.GOOGLE_MAPS,
    val destinations: List<Destination> = listOf(Destination("Home", ""), Destination("Work", "")),

    /** Battery temperature (°C) at which the heat warning shows; critical is this + 5. */
    val heatWarnC: Int = 45,
    val themeMode: ThemeMode = ThemeMode.AUTO,
)
