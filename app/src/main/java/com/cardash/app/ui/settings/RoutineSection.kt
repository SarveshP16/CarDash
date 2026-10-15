package com.cardash.app.ui.settings

import androidx.compose.runtime.Composable
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore

@Composable
fun RoutineSection(s: Settings) {
    SectionTitle("Car On")
    SwitchRow("Turn Wi-Fi on", s.wifiOnCarOn, "Joins your phone's hotspot") { v ->
        SettingsStore.update { it.copy(wifiOnCarOn = v) }
    }
    SwitchRow("Do Not Disturb while driving", s.dndWhileDriving, "Turned off again on Car Off") { v ->
        SettingsStore.update { it.copy(dndWhileDriving = v) }
    }
    SwitchRow("Auto brightness", s.autoBrightnessOnCarOn) { v ->
        SettingsStore.update { it.copy(autoBrightnessOnCarOn = v) }
    }

    SectionTitle("Car Off")
    SwitchRow("Pause media", s.pauseMediaOnCarOff) { v ->
        SettingsStore.update { it.copy(pauseMediaOnCarOff = v) }
    }
    SwitchRow("Turn Wi-Fi off", s.wifiOffOnCarOff, "Less heat while parked") { v ->
        SettingsStore.update { it.copy(wifiOffOnCarOff = v) }
    }
    SwitchRow("Turn screen off", s.screenOffOnCarOff) { v ->
        SettingsStore.update { it.copy(screenOffOnCarOff = v) }
    }
}
