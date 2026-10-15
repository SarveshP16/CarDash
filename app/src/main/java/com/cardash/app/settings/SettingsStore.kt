package com.cardash.app.settings

import android.content.Context
import android.content.SharedPreferences
import com.cardash.app.nav.Destination
import com.cardash.app.nav.NavApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SharedPreferences-backed settings, exposed as a StateFlow so UI and the car service react to changes. */
object SettingsStore {
    private lateinit var prefs: SharedPreferences
    private val _settings = MutableStateFlow(Settings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    val current: Settings get() = _settings.value

    fun init(context: Context) {
        prefs = context.getSharedPreferences("cardash", Context.MODE_PRIVATE)
        _settings.value = read()
    }

    fun update(transform: (Settings) -> Settings) {
        val next = transform(_settings.value)
        write(next)
        _settings.value = next
    }

    private fun read(): Settings {
        val d = Settings()
        return Settings(
            carBluetoothAddress = prefs.getString(K_BT_ADDR, null),
            carBluetoothName = prefs.getString(K_BT_NAME, null),
            chargerAlwaysOn = prefs.getBoolean(K_CHARGER_ALWAYS_ON, d.chargerAlwaysOn),
            wifiOnCarOn = prefs.getBoolean(K_WIFI_ON, d.wifiOnCarOn),
            dndWhileDriving = prefs.getBoolean(K_DND, d.dndWhileDriving),
            autoBrightnessOnCarOn = prefs.getBoolean(K_AUTO_BRIGHT, d.autoBrightnessOnCarOn),
            pauseMediaOnCarOff = prefs.getBoolean(K_PAUSE_MEDIA, d.pauseMediaOnCarOff),
            wifiOffOnCarOff = prefs.getBoolean(K_WIFI_OFF, d.wifiOffOnCarOff),
            screenOffOnCarOff = prefs.getBoolean(K_SCREEN_OFF, d.screenOffOnCarOff),
            navApp = prefs.getString(K_NAV_APP, null)
                ?.let { runCatching { NavApp.valueOf(it) }.getOrNull() } ?: d.navApp,
            destinations = prefs.getString(K_DESTINATIONS, null)
                ?.let(Destination::decodeList) ?: d.destinations,
            heatWarnC = prefs.getInt(K_HEAT_WARN, d.heatWarnC),
            themeMode = prefs.getString(K_THEME, null)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: d.themeMode,
        )
    }

    private fun write(s: Settings) {
        prefs.edit()
            .putString(K_BT_ADDR, s.carBluetoothAddress)
            .putString(K_BT_NAME, s.carBluetoothName)
            .putBoolean(K_CHARGER_ALWAYS_ON, s.chargerAlwaysOn)
            .putBoolean(K_WIFI_ON, s.wifiOnCarOn)
            .putBoolean(K_DND, s.dndWhileDriving)
            .putBoolean(K_AUTO_BRIGHT, s.autoBrightnessOnCarOn)
            .putBoolean(K_PAUSE_MEDIA, s.pauseMediaOnCarOff)
            .putBoolean(K_WIFI_OFF, s.wifiOffOnCarOff)
            .putBoolean(K_SCREEN_OFF, s.screenOffOnCarOff)
            .putString(K_NAV_APP, s.navApp.name)
            .putString(K_DESTINATIONS, Destination.encodeList(s.destinations))
            .putInt(K_HEAT_WARN, s.heatWarnC)
            .putString(K_THEME, s.themeMode.name)
            .apply()
    }

    private const val K_BT_ADDR = "car.bt.address"
    private const val K_BT_NAME = "car.bt.name"
    private const val K_CHARGER_ALWAYS_ON = "car.chargerAlwaysOn"
    private const val K_WIFI_ON = "routine.on.wifi"
    private const val K_DND = "routine.dnd"
    private const val K_AUTO_BRIGHT = "routine.on.autoBrightness"
    private const val K_PAUSE_MEDIA = "routine.off.pauseMedia"
    private const val K_WIFI_OFF = "routine.off.wifi"
    private const val K_SCREEN_OFF = "routine.off.screen"
    private const val K_NAV_APP = "nav.app"
    private const val K_DESTINATIONS = "nav.destinations"
    private const val K_HEAT_WARN = "heat.warnC"
    private const val K_THEME = "theme.mode"
}
