package com.cardash.app.camera

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log

/**
 * Loop recording for Movies/CarDash: once unlocked clips pass the size limit, the oldest are
 * deleted. Clips locked by an impact (name ends in "_locked") are never deleted automatically.
 */
object DashcamStorage {
    private const val TAG = "DashcamStorage"
    private const val FOLDER = "Movies/CarDash/"
    private const val LOCKED_SUFFIX = "_locked"

    data class Clip(val id: Long, val sizeBytes: Long, val dateAdded: Long, val locked: Boolean)

    /** Pure: which unlocked clips to delete (oldest first) to bring unlocked total under [limitBytes]. */
    fun toDelete(clips: List<Clip>, limitBytes: Long): List<Clip> {
        val unlocked = clips.filter { !it.locked }.sortedBy { it.dateAdded }
        var total = unlocked.sumOf { it.sizeBytes }
        val out = mutableListOf<Clip>()
        for (clip in unlocked) {
            if (total <= limitBytes) break
            out += clip
            total -= clip.sizeBytes
        }
        return out
    }

    fun cleanup(context: Context, limitBytes: Long) {
        val resolver = context.contentResolver
        val clips = runCatching { queryClips(context) }.getOrElse {
            Log.w(TAG, "Couldn't list clips", it)
            return
        }
        for (clip in toDelete(clips, limitBytes)) {
            val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, clip.id)
            runCatching { resolver.delete(uri, null, null) }.onFailure { Log.w(TAG, "Couldn't delete $uri", it) }
        }
    }

    /** Renames a finished clip so cleanup skips it. */
    fun lock(context: Context, uri: Uri) {
        runCatching {
            val name = context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: return
            if (name.contains(LOCKED_SUFFIX)) return
            val locked = name.substringBeforeLast(".mp4") + LOCKED_SUFFIX + ".mp4"
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, locked) }, null, null)
        }.onFailure { Log.w(TAG, "Couldn't lock $uri", it) }
    }

    private fun queryClips(context: Context): List<Clip> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.DISPLAY_NAME,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val clips = mutableListOf<Clip>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection, arrayOf(FOLDER, "CarDash_%"), null,
        )?.use { c ->
            while (c.moveToNext()) {
                clips += Clip(c.getLong(0), c.getLong(1), c.getLong(2), c.getString(3).contains(LOCKED_SUFFIX))
            }
        }
        return clips
    }
}
