package com.cardash.app.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val isPlaying: Boolean,
    val packageName: String,
)

/** Tracks the active media session of any player and exposes play/pause/skip. */
object NowPlayingRepository {
    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private var controller: MediaController? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        onSessionsChanged(controllers.orEmpty())
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        // Another player may have started or stopped, so re-pick the session.
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    fun start(context: Context) {
        val app = context.applicationContext
        _hasAccess.value = NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)
        if (!_hasAccess.value || manager != null) return
        val m = app.getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(app, MediaListenerService::class.java)
        runCatching {
            m.addOnActiveSessionsChangedListener(sessionsListener, component, main)
            manager = m
            listenerComponent = component
            refresh()
        }
    }

    private var listenerComponent: ComponentName? = null

    private fun refresh() {
        val m = manager ?: return
        val component = listenerComponent ?: return
        onSessionsChanged(runCatching { m.getActiveSessions(component) }.getOrDefault(emptyList()))
    }

    private fun onSessionsChanged(controllers: List<MediaController>) {
        val best = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(controllerCallback)
            controller = best
            best?.registerCallback(controllerCallback, main)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val meta = c?.metadata
        _nowPlaying.value = if (c == null || meta == null) null else NowPlaying(
            title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART),
            isPlaying = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            packageName = c.packageName,
        )
    }

    fun playPause() {
        val c = controller ?: return
        if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
        else c.transportControls.play()
    }

    fun pause() { controller?.transportControls?.pause() }
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }
}
