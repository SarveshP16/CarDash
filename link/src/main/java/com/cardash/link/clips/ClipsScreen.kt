package com.cardash.link.clips

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cardash.link.CarClips
import com.cardash.link.LinkServer
import com.cardash.link.proto.ClipInfo
import com.cardash.link.proto.Msg
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DayFormat = DateTimeFormatter.ofPattern("EEE d MMM")
private val TimeFormat = DateTimeFormatter.ofPattern("h:mm a")

/** The car's dashcam clips, newest first, grouped by day. Tap one to play it. */
@Composable
fun ClipsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clips by LinkServer.clips.collectAsStateWithLifecycle()
    val progress by ClipFiles.progress.collectAsStateWithLifecycle()
    val saved by ClipFiles.saved.collectAsStateWithLifecycle()
    var playingId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        LinkServer.listClips()
        ClipFiles.refreshSaved(context)
    }
    BackHandler { if (playingId != null) playingId = null else onBack() }

    val all = clips
    val playing = all?.items?.firstOrNull { it.id == playingId }
    if (all != null && playing != null) {
        PlayerScreen(all, playing, progress[playing.id], ClipFiles.fileName(playing) in saved, onClose = { playingId = null })
        return
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Column(Modifier.weight(1f)) {
                    Text("Dashcam clips", style = MaterialTheme.typography.titleLarge)
                    all?.let {
                        Text(
                            buildString {
                                append("${it.items.size} clips · ${size(it.usedBytes)}")
                                if (it.limitBytes > 0) append(" of ${size(it.limitBytes)}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            when {
                LinkServer.isConnected.not() && all == null -> Message("Connect to the car to see its clips: turn on your hotspot and let the car phone join.")
                all == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                all.items.isEmpty() -> Message("No clips yet. The dashcam records while the car is on.")
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                    all.items.groupBy { dayOf(it) }.forEach { (day, dayClips) ->
                        item(key = "d$day") {
                            Text(
                                dayLabel(day),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp),
                            )
                        }
                        items(dayClips, key = { it.id }) { clip ->
                            ClipRow(
                                all, clip, progress[clip.id], ClipFiles.fileName(clip) in saved,
                                onPlay = { playingId = clip.id },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipRow(all: CarClips, clip: ClipInfo, saving: Float?, isSaved: Boolean, onPlay: () -> Unit) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onPlay).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Thumbnail(all.thumbUrl(clip.id), Modifier.width(128.dp).aspectRatio(16f / 9f))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${timeOf(clip)} · ${clip.side.replaceFirstChar(Char::uppercase)}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "${duration(clip.durationMs)} · ${size(clip.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (saving != null) LinearProgressIndicator(progress = { saving }, Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        IconButton(onClick = { LinkServer.clipAction(clip.id, if (clip.locked) Msg.ClipOp.UNLOCK else Msg.ClipOp.LOCK) }) {
            Icon(
                if (clip.locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                if (clip.locked) "Unlock (loop recording may delete it)" else "Lock (keep it)",
                tint = if (clip.locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SaveButton(saving, isSaved) { ClipFiles.save(context, clip, all.videoUrl(clip.id)) }
    }
}

@Composable
private fun SaveButton(saving: Float?, isSaved: Boolean, onSave: () -> Unit) {
    when {
        saving != null -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(progress = { saving }, Modifier.size(24.dp), strokeWidth = 3.dp)
        }
        isSaved -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.CheckCircle, "Saved to this phone", tint = MaterialTheme.colorScheme.primary)
        }
        else -> IconButton(onClick = onSave) { Icon(Icons.Filled.Download, "Save to this phone") }
    }
}

@Composable
private fun Thumbnail(url: String, modifier: Modifier) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, url) { value = ClipFiles.thumbnail(url) }
    Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        val b = bitmap
        if (b != null) Image(b.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Icon(Icons.Filled.Videocam, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PlayerScreen(all: CarClips, clip: ClipInfo, saving: Float?, isSaved: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
                Text(
                    "${dayLabel(dayOf(clip))}, ${timeOf(clip)} · ${clip.side.replaceFirstChar(Char::uppercase)}",
                    color = Color.White, style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setVideoURI(Uri.parse(all.videoUrl(clip.id)))
                            setOnPreparedListener { start() }
                        }
                    },
                    onRelease = { it.stopPlayback() },
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                FilledTonalButton(onClick = {
                    LinkServer.clipAction(clip.id, if (clip.locked) Msg.ClipOp.UNLOCK else Msg.ClipOp.LOCK)
                }) {
                    Icon(if (clip.locked) Icons.Filled.Lock else Icons.Filled.LockOpen, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (clip.locked) "Locked" else "Lock")
                }
                FilledTonalButton(
                    onClick = { ClipFiles.save(context, clip, all.videoUrl(clip.id)) },
                    enabled = saving == null && !isSaved,
                ) {
                    Icon(if (isSaved) Icons.Filled.CheckCircle else Icons.Filled.Download, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            saving != null -> "${(saving * 100).toInt()}%"
                            isSaved -> "Saved"
                            else -> "Save"
                        },
                    )
                }
                FilledTonalButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this clip from the car?") },
            text = { Text(if (isSaved) "The copy saved on this phone stays." else "It hasn't been saved to this phone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    LinkServer.clipAction(clip.id, Msg.ClipOp.DELETE)
                    onClose()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Message(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(24.dp),
    )
}

private fun dayOf(clip: ClipInfo): LocalDate = Instant.ofEpochMilli(clip.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()

private fun dayLabel(day: LocalDate): String = when (day) {
    LocalDate.now() -> "Today"
    LocalDate.now().minusDays(1) -> "Yesterday"
    else -> day.format(DayFormat)
}

private fun timeOf(clip: ClipInfo): String =
    Instant.ofEpochMilli(clip.startMillis).atZone(ZoneId.systemDefault()).format(TimeFormat).lowercase()

private fun duration(ms: Long): String = "%d:%02d".format(ms / 60_000, (ms / 1000) % 60)

private fun size(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1e9)
    else -> "%.0f MB".format(bytes / 1e6)
}
