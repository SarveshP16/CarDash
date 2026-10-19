package com.cardash.app.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.app.apps.LaunchableApps
import com.cardash.app.media.MusicApp
import com.cardash.app.media.NowPlayingRepository
import com.cardash.app.settings.SettingsStore
import com.cardash.app.ui.components.DashCard

/** Song, artist and transport controls for whichever player is active. */
@Composable
fun NowPlayingCard(modifier: Modifier = Modifier) {
    val nowPlaying by NowPlayingRepository.nowPlaying.collectAsStateWithLifecycle()
    val hasAccess by NowPlayingRepository.hasAccess.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val settings by SettingsStore.settings.collectAsStateWithLifecycle()
    // The app opened from the art when nothing is playing (YouTube Music unless changed in Settings).
    val musicApp = remember(settings.musicPackage) {
        MusicApp.resolve(context)?.let { it to (LaunchableApps.label(context, it) ?: it) }
    }

    DashCard(modifier) {
        // Art and title on top, controls spread along the bottom: titles get the full width.
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant)
                        .clickable {
                            (nowPlaying?.packageName ?: musicApp?.first)?.let { LaunchableApps.launch(context, it) }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val art = nowPlaying?.art
                    if (art != null) {
                        Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.MusicNote, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(26.dp))
                    }
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        when {
                            !hasAccess -> "Media access off"
                            nowPlaying == null -> "Not playing"
                            else -> nowPlaying!!.title
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            !hasAccess -> "Run setup.sh"
                            nowPlaying == null -> musicApp?.let { "Tap the art to open ${it.second}" } ?: "Open a music app"
                            else -> nowPlaying!!.artist
                        },
                        fontSize = 14.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Control(Icons.Filled.SkipPrevious, "Previous") { NowPlayingRepository.previous() }
                Control(
                    if (nowPlaying?.isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play/pause",
                    accent = true,
                ) { NowPlayingRepository.playPause() }
                Control(Icons.Filled.SkipNext, "Next") { NowPlayingRepository.next() }
            }
        }
    }
}

@Composable
private fun Control(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        Icon(
            icon, description,
            tint = if (accent) colors.primary else colors.onSurface,
            modifier = Modifier.size(if (accent) 40.dp else 32.dp),
        )
    }
}
