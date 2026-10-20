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
    /** Show the parked screen for [parkedSeconds] before turning the screen off. */
    val parkedScreen: Boolean = true,
    val parkedSeconds: Int = 15,

    // Dashcam
    val dashcamAutoRecord: Boolean = true,
    val dashcamBackCamera: Boolean = true,
    /** Delete the oldest unlocked clips once they pass [dashcamLimitGb]. */
    val dashcamLoop: Boolean = true,
    val dashcamLimitGb: Int = 8,
    /** Record sound with the video. Off by default: it records conversations in the car. */
    val dashcamAudio: Boolean = false,

    /** Package the now-playing card opens; null = auto-detect YouTube Music. */
    val musicPackage: String? = null,
    /** Resume music on Car On if it was playing when the car turned off. */
    val resumeMusicOnCarOn: Boolean = true,

    /** True: route inside CarDash (Navigation SDK), falling back to [navApp]. False: always open [navApp]. */
    val navigateInApp: Boolean = false,
    /** The navigation app opened by the Maps tile, and used for routes when not navigating in-app. */
    val navApp: NavApp = NavApp.GOOGLE_MAPS,
    /** Spoken turn-by-turn directions during in-app navigation. Off: on-screen guidance only. */
    val voiceGuidance: Boolean = false,
    val home: Destination = Destination("Home", ""),
    /** Saved places shown under the Fav tile, in the order they were added. */
    val favourites: List<Destination> = emptyList(),
    /** Last places routed to, newest first (shown in CarDash Link). */
    val recents: List<Destination> = emptyList(),

    /** CarDash Link: the secret agreed when pairing with the main phone; null = not paired. */
    val linkToken: String? = null,
    val linkPhoneName: String? = null,

    /** Pause charging at [chargeLimit]% and resume 5% below it (needs root). */
    val chargeLimitEnabled: Boolean = true,
    val chargeLimit: Int = 80,

    /** Battery temperature (°C) at which the heat warning shows; critical is this + 5. */
    val heatWarnC: Int = 45,
    val themeMode: ThemeMode = ThemeMode.AUTO,
)
