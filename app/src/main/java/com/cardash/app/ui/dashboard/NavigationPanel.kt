package com.cardash.app.ui.dashboard

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.location.LocationRepository
import com.cardash.app.nav.InAppNavigator
import com.cardash.app.nav.NavState
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.navigation.ForceNightMode
import com.google.android.libraries.navigation.NavigationView

/**
 * The Navigation SDK's map: a follow-me Google map with traffic when idle, full turn-by-turn
 * (header, speedometer, speed limit, ETA) while guiding. The "Where to?" box and our speed badge
 * only show when idle, since the SDK draws its own while navigating.
 */
@SuppressLint("MissingPermission")
@Composable
fun NavigationPanel(night: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val nav by InAppNavigator.state.collectAsStateWithLifecycle()
    val guiding = nav is NavState.Guiding

    var map by remember { mutableStateOf<GoogleMap?>(null) }
    val navView = remember {
        NavigationView(context).apply {
            onCreate(null)
            setSpeedometerEnabled(true)
            setSpeedLimitIconEnabled(true)
            getMapAsync { m ->
                m.isTrafficEnabled = true
                if (LocationRepository.hasPermission(context)) m.isMyLocationEnabled = true
                m.uiSettings.isMapToolbarEnabled = false
                map = m
            }
        }
    }

    // Following needs the navigator, which only exists once Google's terms are accepted.
    val navReady = nav is NavState.Ready || nav is NavState.Guiding
    LaunchedEffect(map, navReady) {
        val m = map ?: return@LaunchedEffect
        if (navReady) runCatching { m.followMyLocation(GoogleMap.CameraPerspective.TILTED) }
    }

    // NavigationView must be driven through the host's lifecycle.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { navView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> { navView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> { navView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> { navView.onStop(); started = false }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (resumed) navView.onPause()
            if (started) navView.onStop()
            navView.onDestroy()
        }
    }

    Box(modifier.clip(RoundedCornerShape(20.dp))) {
        AndroidView(
            factory = { navView },
            update = { it.setForceNightMode(if (night) ForceNightMode.FORCE_NIGHT else ForceNightMode.FORCE_DAY) },
            modifier = Modifier.fillMaxSize(),
        )
        if (!guiding) {
            DestinationSearch(Modifier.align(Alignment.TopStart).padding(12.dp).fillMaxWidth(0.8f))
            // Bottom-right: the Google logo and recenter button live bottom-left.
            SpeedBadge(Modifier.align(Alignment.BottomEnd).padding(12.dp))
        }
        (nav as? NavState.Routing)?.let { StatusChip("Finding ${it.query}…", Modifier.align(Alignment.Center)) }
        (nav as? NavState.Unavailable)?.let {
            StatusChip("In-app navigation off: ${it.reason}", Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        }
    }
}

@Composable
private fun StatusChip(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
        Text(text, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
    }
}
