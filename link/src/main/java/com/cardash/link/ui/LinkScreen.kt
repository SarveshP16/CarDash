package com.cardash.link.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.link.Hotspot
import com.cardash.link.LinkServer
import com.cardash.link.clips.ClipsScreen
import com.cardash.link.LinkStatus
import com.cardash.link.media.LinkListenerService
import com.cardash.link.media.PhoneMedia
import com.cardash.link.proto.LinkPlace
import kotlinx.coroutines.delay

/** One scrolling page: link status, setup still needed, Home | Fav, search, then recents. */
@Composable
fun LinkScreen() {
    val context = LocalContext.current
    val status by LinkServer.status.collectAsStateWithLifecycle()
    val places by LinkServer.places.collectAsStateWithLifecycle()
    val route by LinkServer.route.collectAsStateWithLifecycle()
    val pending by LinkServer.pending.collectAsStateWithLifecycle()
    val pairCode by LinkServer.pairCode.collectAsStateWithLifecycle()
    val suggestions by LinkServer.suggestions.collectAsStateWithLifecycle()
    val connected = status is LinkStatus.Connected
    var query by rememberSaveable { mutableStateOf("") }
    var showFavourites by rememberSaveable { mutableStateOf(false) }
    var showClips by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    if (showClips) {
        ClipsScreen(onBack = { showClips = false })
        return
    }

    LaunchedEffect(query, connected) {
        delay(300)
        LinkServer.suggest(if (connected) query else "")
    }

    fun go(place: LinkPlace) {
        val sent = LinkServer.navigate(place)
        Toast.makeText(
            context,
            if (sent) "Sent ${place.name} to the car" else "${place.name} will be sent when the car connects",
            Toast.LENGTH_SHORT,
        ).show()
        query = ""
    }

    val favourites = places?.favourites.orEmpty()
    fun isFavourite(p: LinkPlace) = favourites.any {
        (p.placeId != null && it.placeId == p.placeId) || it.address.equals(p.address, ignoreCase = true)
    }
    fun toggleFavourite(p: LinkPlace) {
        val ok = if (isFavourite(p)) {
            favourites.firstOrNull { (p.placeId != null && it.placeId == p.placeId) || it.address.equals(p.address, true) }
                ?.let(LinkServer::removeFavourite) ?: false
        } else LinkServer.saveFavourite(p.copy(distanceMeters = null))
        if (!ok) Toast.makeText(context, "Favourites can be changed while the car is connected", Toast.LENGTH_SHORT).show()
    }
    fun setHome(p: LinkPlace) {
        val ok = LinkServer.setHome(p.copy(distanceMeters = null))
        Toast.makeText(
            context,
            if (ok) "Home set to ${p.name}" else "Home can be set while the car is connected",
            Toast.LENGTH_SHORT,
        ).show()
        if (ok) query = ""
    }
    val home = places?.home

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "CarDash Link",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            item { StatusCard(status, onPair = { LinkServer.startPairing() }) }
            item { SetupChecklist() }
            pending?.let { p ->
                item {
                    InfoCard(Icons.Filled.Schedule, "Waiting to send ${p.name}", "Goes to the car as soon as it connects") {
                        IconButton(onClick = LinkServer::cancelPending) { Icon(Icons.Filled.Close, "Cancel") }
                    }
                }
            }
            route?.let { destination ->
                item {
                    InfoCard(Icons.Filled.Navigation, "Navigating to $destination", "On the car") {
                        FilledTonalButton(onClick = { LinkServer.endRoute() }) { Text("End") }
                    }
                }
            }
            item {
                HomeFavRow(
                    home = home,
                    favouriteCount = favourites.size,
                    onHome = {
                        if (home != null) go(home)
                        else Toast.makeText(context, "Search for your home address, then tap the house to set it", Toast.LENGTH_LONG).show()
                    },
                    onFav = { showFavourites = true },
                )
            }
            if (connected) {
                item {
                    InfoCard(
                        Icons.Filled.Videocam, "Dashcam clips", "Watch, save, lock or delete the car's recordings",
                        onClick = { showClips = true },
                    ) {
                        FilledTonalButton(onClick = { showClips = true }) { Text("Open") }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Send a place to the car") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Clear") }
                    },
                    supportingText = if (!connected && query.isNotBlank()) {
                        { Text("Suggestions need the car connected. Tap Send to queue it as typed.") }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (query.isNotBlank()) go(LinkPlace(query.trim(), query.trim()))
                    }),
                )
            }
            if (query.isNotBlank()) {
                items(suggestions, key = { "s" + (it.placeId ?: it.address) }) { s ->
                    PlaceRow(
                        Icons.Filled.Place, s.name,
                        listOfNotNull(s.distanceMeters?.let(::distance), s.address.removePrefix("${s.name}, ")).joinToString(" · "),
                        starred = isFavourite(s), onStar = { toggleFavourite(s) }, onSetHome = { setHome(s) },
                    ) { go(s) }
                }
                // Suggestions come from Google Places (searched on the car phone).
                if (suggestions.isNotEmpty()) item { GoogleMapsAttribution() }
            } else {
                recentList(places?.recents.orEmpty(), ::go)
                item { Tip() }
                item {
                    TextButton(onClick = { showAbout = true }) { Text("About & licences") }
                }
            }
            item { Spacer(Modifier.size(24.dp)) }
        }
    }

    if (showAbout) AboutDialog(onDismiss = { showAbout = false })

    if (showFavourites) {
        FavouritesSheet(
            favourites = favourites,
            onDismiss = { showFavourites = false },
            onGo = { showFavourites = false; go(it) },
            onRemove = ::toggleFavourite,
        )
    }

    pairCode?.let { code ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Pair with your car") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(code.chunked(3).joinToString(" "), fontSize = 44.sp, fontWeight = FontWeight.Medium)
                    Text(
                        "1. Turn on this phone's hotspot and let the car phone join it.\n" +
                            "2. On the car: Settings → Phone link, enter this code and tap Pair.\n\n" +
                            "The code works for 5 minutes.",
                    )
                }
            },
            confirmButton = { TextButton(onClick = LinkServer::cancelPairing) { Text("Cancel") } },
        )
    }
}

private fun LazyListScope.recentList(recents: List<LinkPlace>, go: (LinkPlace) -> Unit) {
    if (recents.isNotEmpty()) {
        item { Heading("Recent") }
        items(recents, key = { "r" + (it.placeId ?: it.address) }) { r ->
            PlaceRow(Icons.Filled.History, r.name, r.address.removePrefix("${r.name}, ")) { go(r) }
        }
    }
}

/** Two big buttons, like the car's: Home routes straight there, Fav opens the saved places. */
@Composable
private fun HomeFavRow(home: LinkPlace?, favouriteCount: Int, onHome: () -> Unit, onFav: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigButton(
            Icons.Filled.Home, "Home",
            home?.address?.substringBefore(",") ?: "Not set",
            Modifier.weight(1f), onHome,
        )
        BigButton(
            Icons.Filled.Star, "Fav",
            if (favouriteCount == 0) "None saved" else "$favouriteCount saved",
            Modifier.weight(1f), onFav,
        )
    }
}

@Composable
private fun BigButton(icon: ImageVector, label: String, detail: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavouritesSheet(
    favourites: List<LinkPlace>,
    onDismiss: () -> Unit,
    onGo: (LinkPlace) -> Unit,
    onRemove: (LinkPlace) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
            Text("Favourites", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
            if (favourites.isEmpty()) {
                Text(
                    "Nothing saved yet. Search for a place and tap its star.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            favourites.forEach { f ->
                PlaceRow(Icons.Filled.Star, f.name, f.address.removePrefix("${f.name}, "), starred = true, onStar = { onRemove(f) }) {
                    onGo(f)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(status: LinkStatus, onPair: () -> Unit) {
    val hotspotOn by Hotspot.on.collectAsStateWithLifecycle()
    val (title, subtitle) = when (status) {
        is LinkStatus.Connected -> "Connected to ${status.carName}" to "Places and music are shared with the car"
        is LinkStatus.Waiting -> "Waiting for ${status.carName}" to
            if (hotspotOn) "Hotspot is on; the car connects when it joins" else "Turn on your hotspot to connect"
        LinkStatus.NotPaired -> "Not paired" to "Pair once with the CarDash phone in your car"
    }
    val colors = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (status is LinkStatus.Connected) colors.primaryContainer else colors.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsCar, null, Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (status == LinkStatus.NotPaired) {
                Button(onClick = onPair) { Text("Pair") }
            } else {
                var confirm by remember { mutableStateOf(false) }
                TextButton(onClick = { confirm = true }) { Text("Forget") }
                if (confirm) {
                    AlertDialog(
                        onDismissRequest = { confirm = false },
                        title = { Text("Forget this car?") },
                        text = { Text("You'll need to pair again with a new code.") },
                        confirmButton = {
                            TextButton(onClick = { confirm = false; LinkServer.forgetCar() }) { Text("Forget") }
                        },
                        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancel") } },
                    )
                }
            }
        }
    }
}

/** Only the steps still missing; re-checked every time the app comes back to the front. */
@SuppressLint("BatteryLife")
@Composable
private fun SetupChecklist() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val mediaAccess by PhoneMedia.hasAccess.collectAsStateWithLifecycle()
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    val notificationsOk = remember(refresh) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val batteryOk = remember(refresh) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!mediaAccess) {
            SetupItem(Icons.Filled.MusicNote, "Allow music access", "Shows what's playing on the car and lets it control music") {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                        Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        ComponentName(context, LinkListenerService::class.java).flattenToString(),
                    ),
                )
            }
        }
        if (!batteryOk) {
            SetupItem(Icons.Filled.BatteryAlert, "Allow running in the background", "So the car can connect any time") {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
                )
            }
        }
        if (!notificationsOk) {
            SetupItem(Icons.Filled.Notifications, "Allow notifications", "For the quiet \"Connected to car\" notification") {
                notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
private fun SetupItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    InfoCard(icon, title, subtitle, onClick = onClick) {
        FilledTonalButton(onClick = onClick) { Text("Allow") }
    }
}

@Composable
private fun InfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    action: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            action()
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
private fun PlaceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    starred: Boolean? = null,
    onStar: () -> Unit = {},
    onSetHome: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onSetHome != null) {
            IconButton(onClick = onSetHome) {
                Icon(Icons.Filled.Home, "Set as Home", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (starred != null) {
            IconButton(onClick = onStar) {
                Icon(
                    if (starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    if (starred) "Remove from favourites" else "Save to favourites",
                    tint = if (starred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Tip() {
    Text(
        "Tip: in Google Maps, tap Share on any place and choose \"Send to car\".",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp),
    )
}

private fun distance(m: Int): String = if (m < 1000) "$m m" else "%.1f km".format(m / 1000f)
