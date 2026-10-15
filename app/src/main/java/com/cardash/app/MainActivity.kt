package com.cardash.app

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cardash.app.car.CarRepository
import com.cardash.app.car.CarState
import com.cardash.app.location.LocationRepository
import com.cardash.app.media.NowPlayingRepository
import com.cardash.app.ui.CarDashRoot
import kotlinx.coroutines.launch

/** The launcher activity: the phone's home screen. */
class MainActivity : ComponentActivity() {

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { LocationRepository.start(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over the (disabled) keyguard and wake the screen when brought up on "Car On".
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        hideSystemBars()

        // Keep the screen on only while the car is on.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                CarRepository.state.collect { state ->
                    if (state == CarState.ON) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        }

        // Device Owner grants location silently; otherwise ask once.
        if (!LocationRepository.hasPermission(this)) {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        setContent { CarDashRoot() }
    }

    override fun onStart() {
        super.onStart()
        LocationRepository.start(this)
        // Picks up notification access granted since launch.
        NowPlayingRepository.start(this)
    }

    override fun onStop() {
        LocationRepository.stop()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
