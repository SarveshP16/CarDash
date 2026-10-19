package com.cardash.app.car

import android.content.Context

/** Remembers (across reboots) whether music was playing when the car turned off. */
object ResumeState {
    private const val PREFS = "cardash_state"
    private const val KEY = "music.wasPlayingAtCarOff"

    fun save(context: Context, wasPlaying: Boolean) {
        prefs(context).edit().putBoolean(KEY, wasPlaying).apply()
    }

    /** Returns the saved flag and clears it, so music resumes at most once per drive. */
    fun consume(context: Context): Boolean {
        val wasPlaying = prefs(context).getBoolean(KEY, false)
        prefs(context).edit().remove(KEY).apply()
        return wasPlaying
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
