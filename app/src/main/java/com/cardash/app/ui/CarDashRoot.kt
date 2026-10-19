package com.cardash.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.car.ParkedState
import com.cardash.app.location.LocationRepository
import com.cardash.app.settings.SettingsStore
import com.cardash.app.theme.CarDashTheme
import com.cardash.app.theme.rememberIsNight
import com.cardash.app.ui.apps.AppsScreen
import com.cardash.app.ui.dashboard.DashboardScreen
import com.cardash.app.ui.settings.SettingsScreen

enum class Screen { DASHBOARD, APPS, SETTINGS }

@Composable
fun CarDashRoot() {
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    val fix by LocationRepository.fix.collectAsStateWithLifecycle()
    val night = rememberIsNight(settings.themeMode, fix)
    var screen by rememberSaveable { mutableStateOf(Screen.DASHBOARD) }
    val screenOffAt by ParkedState.screenOffAt.collectAsStateWithLifecycle()

    // As the home screen, Back on the dashboard does nothing; elsewhere it returns to the dashboard.
    BackHandler(enabled = screen != Screen.DASHBOARD) { screen = Screen.DASHBOARD }

    CarDashTheme(night = night) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (screen) {
                Screen.DASHBOARD -> DashboardScreen(
                    night = night,
                    onOpenApps = { screen = Screen.APPS },
                    onOpenSettings = { screen = Screen.SETTINGS },
                )
                Screen.APPS -> AppsScreen(onBack = { screen = Screen.DASHBOARD })
                Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.DASHBOARD })
            }
            // After Car Off: covers everything until the screen turns off or "Keep screen on".
            screenOffAt?.let { ParkedScreen(it) }
        }
    }
}
