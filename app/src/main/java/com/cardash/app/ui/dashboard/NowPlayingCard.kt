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
import com.cardash.app.link.LinkClient
import com.cardash.app.media.MusicApp
import com.cardash.app.media.NowPlaying
import com.cardash.app.media.NowPlayingRepository
import com.cardash.app.settings.SettingsStore
import com.cardash.app.ui.components.DashCard
import com.cardash.link.proto.Msg

/** Song, artist and transport controls for whichever player is active, here or on the main phone. */
/** [compact]: smaller art, text and controls for the narrow sidebar while navigating. */
@Composable
fun NowPlayingCard(modifier: Modifier = Modifier, compact: Boolean = false) {
    val local by NowPlayingRepository.nowPlaying.collectAsStateWithLifecycle()
    // Music on the main phone (CarDash Link) shows unless something plays on this phone itself.
    val remote by LinkClient.track.collectAsStateWithLifecycle()
    val onPhone = remote != null && local?.isPlaying != true
    val nowPlaying = if (onPhone) remote!!.let { NowPlaying(it.title, it.artist, it.art, it.playing, "") } else local
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
                    Modifier.size(if (compact) 40.dp else 56.dp).clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant)
                        .clickable(enabled = !onPhone) {
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
                Column(Modifier.weight(1f).padding(start = if (compact) 8.dp else 12.dp)) {
                    Text(
                        when {
                            nowPlaying == null && !hasAccess -> "Media access off"
                            nowPlaying == null -> "Not playing"
                            else -> nowPlaying!!.title
                        },
                        fontSize = if (compact) 15.sp else 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            nowPlaying == null && !hasAccess -> "Run setup.sh"
                            nowPlaying == null -> musicApp?.let { "Tap the art to open ${it.second}" } ?: "Open a music app"
                            else -> nowPlaying!!.artist
                        },
                        fontSize = if (compact) 12.sp else 14.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Control(Icons.Filled.SkipPrevious, "Previous", compact = compact) { if (onPhone) LinkClient.command(Msg.Action.PREVIOUS) else NowPlayingRepository.previous() }
                Control(
                    if (nowPlaying?.isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play/pause",
                    accent = true,
                    compact = compact,
                ) { if (onPhone) LinkClient.command(Msg.Action.TOGGLE) else NowPlayingRepository.playPause() }
                Control(Icons.Filled.SkipNext, "Next", compact = compact) { if (onPhone) LinkClient.command(Msg.Action.NEXT) else NowPlayingRepository.next() }
            }
        }
    }
}

@Composable
private fun Control(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    accent: Boolean = false,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = onClick, modifier = Modifier.size(if (compact) 44.dp else 52.dp)) {
        val size = when {
            compact && accent -> 34.dp
            compact -> 28.dp
            accent -> 40.dp
            else -> 32.dp
        }
        Icon(icon, description, tint = if (accent) colors.primary else colors.onSurface, modifier = Modifier.size(size))
    }
}
