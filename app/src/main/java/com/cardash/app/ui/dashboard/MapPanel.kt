package com.cardash.app.ui.dashboard

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

// CARTO basemaps (same as ServoBuddy): a light style for day, a dark one for night. No API key.
private fun cartoSource(name: String, style: String) = XYTileSource(
    name, 0, 20, 256, ".png",
    arrayOf("a", "b", "c", "d").map { "https://$it.basemaps.cartocdn.com/$style/" }.toTypedArray(),
    "© OpenStreetMap contributors © CARTO",
)

private val DayTiles = cartoSource("CartoVoyager", "rastertiles/voyager")
private val NightTiles = cartoSource("CartoDark", "dark_all")

/** A follow-me map preview. Navigation itself is handed off to Google Maps / Waze. */
@Composable
fun MapPanel(night: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mapView = remember { createMapView(context) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    Box(modifier.clip(RoundedCornerShape(20.dp))) {
        AndroidView(
            factory = { mapView },
            update = { it.setTileSource(if (night) NightTiles else DayTiles) },
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            "© OpenStreetMap © CARTO",
            fontSize = 9.sp,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
        )
    }
}

private fun createMapView(context: Context): MapView {
    Configuration.getInstance().userAgentValue = context.packageName
    return MapView(context).apply {
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        controller.setZoom(16.0)
        val me = MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply {
            enableMyLocation()
            enableFollowLocation()
        }
        overlays.add(me)
    }
}
