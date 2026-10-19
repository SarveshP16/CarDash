package com.cardash.app.media

import android.content.Context
import com.cardash.app.apps.LaunchableApps
import com.cardash.app.settings.SettingsStore

/** Which app the now-playing card opens: the one picked in Settings, else YouTube Music if installed. */
object MusicApp {
    private const val YT_MUSIC_SUFFIX = "youtube.music"
    private const val STOCK_YT_MUSIC = "com.google.android.apps.youtube.music"

    fun resolve(context: Context): String? {
        val installed = LaunchableApps.packages(context)
        SettingsStore.current.musicPackage?.let { if (it in installed) return it }
        return pickYouTubeMusic(installed)
    }

    /**
     * Patched builds (Morphe, ReVanced…) rename the package but keep the "youtube.music" ending,
     * e.g. app.morphe.android.apps.youtube.music. Prefer a patched one over the stock app.
     */
    fun pickYouTubeMusic(packages: List<String>): String? {
        val candidates = packages.filter { it.endsWith(YT_MUSIC_SUFFIX) }
        return candidates.firstOrNull { it != STOCK_YT_MUSIC } ?: candidates.firstOrNull()
    }
}
