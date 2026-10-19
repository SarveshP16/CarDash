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
            parkedScreen = prefs.getBoolean(K_PARKED, d.parkedScreen),
            parkedSeconds = prefs.getInt(K_PARKED_SECONDS, d.parkedSeconds),
            dashcamAutoRecord = prefs.getBoolean(K_DASH_AUTO, d.dashcamAutoRecord),
            dashcamBackCamera = prefs.getBoolean(K_DASH_BACK, d.dashcamBackCamera),
            dashcamLoop = prefs.getBoolean(K_DASH_LOOP, d.dashcamLoop),
            dashcamLimitGb = prefs.getInt(K_DASH_LIMIT, d.dashcamLimitGb),
            musicPackage = prefs.getString(K_MUSIC_PKG, null),
            resumeMusicOnCarOn = prefs.getBoolean(K_RESUME_MUSIC, d.resumeMusicOnCarOn),
            voiceGuidance = prefs.getBoolean(K_VOICE, d.voiceGuidance),
            navigateInApp = prefs.getBoolean(K_IN_APP, d.navigateInApp),
            navApp = prefs.getString(K_NAV_APP, null)
                ?.let { runCatching { NavApp.valueOf(it) }.getOrNull() } ?: d.navApp,
            destinations = prefs.getString(K_DESTINATIONS, null)
                ?.let(Destination::decodeList) ?: d.destinations,
            chargeLimitEnabled = prefs.getBoolean(K_CHARGE_LIMIT_ON, d.chargeLimitEnabled),
            chargeLimit = prefs.getInt(K_CHARGE_LIMIT, d.chargeLimit),
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
            .putBoolean(K_PARKED, s.parkedScreen)
            .putInt(K_PARKED_SECONDS, s.parkedSeconds)
            .putBoolean(K_DASH_AUTO, s.dashcamAutoRecord)
            .putBoolean(K_DASH_BACK, s.dashcamBackCamera)
            .putBoolean(K_DASH_LOOP, s.dashcamLoop)
            .putInt(K_DASH_LIMIT, s.dashcamLimitGb)
            .putString(K_MUSIC_PKG, s.musicPackage)
            .putBoolean(K_RESUME_MUSIC, s.resumeMusicOnCarOn)
            .putBoolean(K_VOICE, s.voiceGuidance)
            .putBoolean(K_IN_APP, s.navigateInApp)
            .putString(K_NAV_APP, s.navApp.name)
            .putString(K_DESTINATIONS, Destination.encodeList(s.destinations))
            .putBoolean(K_CHARGE_LIMIT_ON, s.chargeLimitEnabled)
            .putInt(K_CHARGE_LIMIT, s.chargeLimit)
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
    private const val K_PARKED = "routine.off.parkedScreen"
    private const val K_PARKED_SECONDS = "routine.off.parkedSeconds"
    private const val K_DASH_AUTO = "dashcam.auto"
    private const val K_DASH_BACK = "dashcam.back"
    private const val K_DASH_LOOP = "dashcam.loop"
    private const val K_DASH_LIMIT = "dashcam.limitGb"
    private const val K_MUSIC_PKG = "music.package"
    private const val K_RESUME_MUSIC = "routine.on.resumeMusic"
    private const val K_VOICE = "nav.voice"
    private const val K_IN_APP = "nav.inApp"
    private const val K_NAV_APP = "nav.app"
    private const val K_DESTINATIONS = "nav.destinations"
    private const val K_CHARGE_LIMIT_ON = "battery.limit.enabled"
    private const val K_CHARGE_LIMIT = "battery.limit"
    private const val K_HEAT_WARN = "heat.warnC"
    private const val K_THEME = "theme.mode"
}
