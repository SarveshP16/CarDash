package com.cardash.app.ui.dashboard

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.cardash.app.media.NowPlayingRepository
import com.cardash.app.ui.components.DashCard

private const val SPOTIFY = "com.spotify.music"

/** Song, artist and transport controls for whichever player is active. */
@Composable
fun NowPlayingCard(modifier: Modifier = Modifier) {
    val nowPlaying by NowPlayingRepository.nowPlaying.collectAsStateWithLifecycle()
    val hasAccess by NowPlayingRepository.hasAccess.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    DashCard(modifier) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.fillMaxHeight().aspectRatio(1f).clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant)
                    .clickable { openPlayer(context, nowPlaying?.packageName ?: SPOTIFY) },
                contentAlignment = Alignment.Center,
            ) {
                val art = nowPlaying?.art
                if (art != null) {
                    Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(40.dp))
                }
            }
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(start = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        when {
                            !hasAccess -> "Media access not granted"
                            nowPlaying == null -> "Nothing playing"
                            else -> nowPlaying!!.title
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            !hasAccess -> "Run setup.sh to allow it"
                            nowPlaying == null -> "Tap the art to open Spotify"
                            else -> nowPlaying!!.artist
                        },
                        fontSize = 15.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Control(Icons.Filled.SkipPrevious, "Previous") { NowPlayingRepository.previous() }
                    Control(
                        if (nowPlaying?.isPlaying == true) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "Play/pause",
                        primary = true,
                    ) { NowPlayingRepository.playPause() }
                    Control(Icons.Filled.SkipNext, "Next") { NowPlayingRepository.next() }
                }
            }
        }
    }
}

@Composable
private fun Control(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (primary) 64.dp else 56.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.primary else colors.surfaceVariant),
    ) {
        Icon(
            icon, description,
            tint = if (primary) colors.onPrimary else colors.onSurface,
            modifier = Modifier.size(if (primary) 38.dp else 32.dp),
        )
    }
}

private fun openPlayer(context: Context, packageName: String) {
    context.packageManager.getLaunchIntentForPackage(packageName)?.let(context::startActivity)
}
