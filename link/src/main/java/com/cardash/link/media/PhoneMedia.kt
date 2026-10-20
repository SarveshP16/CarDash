package com.cardash.link.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log
import android.view.KeyEvent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhoneTrack(
    val title: String,
    val artist: String,
    val app: String,
    val playing: Boolean,
    val art: Bitmap?,
)

/** Exists only so the app may read and control other apps' media sessions. */
class LinkListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        PhoneMedia.start(this)
    }
}

/** This phone's active player (YouTube Music etc.), mirrored to the car and controlled from it. */
object PhoneMedia {
    private const val TAG = "PhoneMedia"
    /** How long Car On waits for the car's Bluetooth audio before giving up on resuming. */
    private const val CAR_AUDIO_WAIT_MS = 90_000L

    private val _track = MutableStateFlow<PhoneTrack?>(null)
    val track: StateFlow<PhoneTrack?> = _track.asStateFlow()

    private val _hasAccess = MutableStateFlow(false)
    val hasAccess: StateFlow<Boolean> = _hasAccess.asStateFlow()

    private val main = Handler(Looper.getMainLooper())
    private var manager: MediaSessionManager? = null
    private var component: ComponentName? = null
    private var controller: MediaController? = null
    private lateinit var app: Context

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { onSessions(it.orEmpty()) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
        override fun onSessionDestroyed() = refresh()
    }

    fun start(context: Context) {
        app = context.applicationContext
        _hasAccess.value = NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)
        if (!_hasAccess.value || manager != null) return
        val m = app.getSystemService(MediaSessionManager::class.java)
        val c = ComponentName(app, LinkListenerService::class.java)
        runCatching {
            m.addOnActiveSessionsChangedListener(sessionsListener, c, main)
            manager = m
            component = c
            refresh()
        }.onFailure { Log.w(TAG, "No media session access", it) }
    }

    private fun refresh() {
        val m = manager ?: return
        val c = component ?: return
        onSessions(runCatching { m.getActiveSessions(c) }.getOrDefault(emptyList()))
    }

    private fun onSessions(controllers: List<MediaController>) {
        val best = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull { !it.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank() }
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, main)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val meta = c?.metadata
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
        _track.value = if (c == null || title.isNullOrBlank()) null else PhoneTrack(
            title = title,
            artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            app = appLabel(c.packageName),
            playing = c.playbackState?.state == PlaybackState.STATE_PLAYING,
            art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART),
        )
    }

    private fun appLabel(pkg: String): String = runCatching {
        val pm = app.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))).toString()
    }.getOrDefault(pkg)

    private val isPlaying get() = controller?.playbackState?.state == PlaybackState.STATE_PLAYING

    fun play() {
        if (isPlaying) return
        val c = controller
        if (c != null) {
            c.transportControls.play()
            return
        }
        // No session (the player was closed): Android hands PLAY to the app that played last.
        val audio = app.getSystemService(AudioManager::class.java)
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
    }

    fun pause() { controller?.transportControls?.pause() }
    fun toggle() = if (isPlaying) pause() else play()
    fun next() { controller?.transportControls?.skipToNext() }
    fun previous() { controller?.transportControls?.skipToPrevious() }

    private var waiting: AudioDeviceCallback? = null
    private val giveUp = Runnable { stopWaiting() }

    /**
     * Car On: play once the car's Bluetooth audio (the AUX receiver) has connected, so music never
     * starts from the phone's own speaker. Gives up after 90 s.
     */
    fun resumeWhenCarAudioReady() {
        val audio = app.getSystemService(AudioManager::class.java)
        stopWaiting()
        if (audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any(::isCarAudio)) {
            main.postDelayed({ play() }, 1_000)
            return
        }
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
                if (added.none(::isCarAudio)) return
                stopWaiting()
                // Let the Bluetooth audio route settle before playing.
                main.postDelayed({ play() }, 2_500)
            }
        }
        waiting = cb
        audio.registerAudioDeviceCallback(cb, main)
        main.postDelayed(giveUp, CAR_AUDIO_WAIT_MS)
    }

    private fun stopWaiting() {
        main.removeCallbacks(giveUp)
        waiting?.let { app.getSystemService(AudioManager::class.java).unregisterAudioDeviceCallback(it) }
        waiting = null
    }

    private fun isCarAudio(d: AudioDeviceInfo) = d.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        d.type == AudioDeviceInfo.TYPE_BLE_HEADSET || d.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
}
