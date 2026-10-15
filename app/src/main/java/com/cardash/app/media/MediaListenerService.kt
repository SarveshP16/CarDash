package com.cardash.app.media

import android.service.notification.NotificationListenerService

/**
 * Exists only so the app may read other apps' media sessions (Spotify, YouTube Music, podcasts…).
 * `setup.sh` enables it with `cmd notification allow_listener`.
 */
class MediaListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        NowPlayingRepository.start(this)
    }
}
