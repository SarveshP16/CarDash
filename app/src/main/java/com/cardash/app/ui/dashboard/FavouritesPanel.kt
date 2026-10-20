package com.cardash.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cardash.app.nav.Destination
import com.cardash.app.settings.Favourites

/** Saved places in a left-side panel (same style as search). Tap to route, bin to remove. */
@Composable
fun FavouritesPanel(favourites: List<Destination>, onPick: (Destination) -> Unit, onDismiss: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(
                Modifier.fillMaxHeight().fillMaxWidth(0.45f)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(52.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", modifier = Modifier.size(28.dp))
                    }
                    Text("Favourites", fontSize = 24.sp, fontWeight = FontWeight.Medium)
                }
                if (favourites.isEmpty()) {
                    Text(
                        "No saved places yet. In Where to?, tap ☆ next to a suggestion, or add places in Settings → Navigation.",
                        color = muted,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(favourites, key = { "${it.label}|${it.address}" }) { place ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(place) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp).size(24.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(place.label, fontSize = 17.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(place.address, fontSize = 13.sp, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { Favourites.remove(place) }) {
                                Icon(Icons.Filled.Delete, "Remove ${place.label}", tint = muted)
                            }
                        }
                    }
                }
            }
            }
        }
    }
}
