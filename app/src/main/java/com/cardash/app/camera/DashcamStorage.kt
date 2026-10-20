package com.cardash.app.camera

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.cardash.link.proto.ClipInfo
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    // ---- For CarDash Link (browsing clips from the main phone) ----

    /** Finished clips, newest first. Clips still being recorded are pending in MediaStore and not listed. */
    fun listForLink(context: Context): List<ClipInfo> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.Video.VideoColumns.DURATION,
        )
        val out = mutableListOf<ClipInfo>()
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, SELECTION, arrayOf(FOLDER, "CarDash_%"), null,
        )?.use { c ->
            while (c.moveToNext()) {
                val name = c.getString(3)
                out += ClipInfo(
                    id = c.getLong(0),
                    side = if (name.startsWith("CarDash_front")) "front" else "back",
                    startMillis = startFromName(name) ?: (c.getLong(2) * 1000),
                    durationMs = c.getLong(4),
                    sizeBytes = c.getLong(1),
                    locked = name.contains(LOCKED_SUFFIX),
                )
            }
        }
        return out.sortedByDescending { it.startMillis }
    }

    fun uri(id: Long): Uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

    /** Locks or unlocks a clip by renaming it. Returns false if it's gone or isn't a dashcam clip. */
    fun setLocked(context: Context, id: Long, locked: Boolean): Boolean {
        if (locked) {
            lock(context, uri(id))
            return true
        }
        return runCatching {
            val name = displayName(context, id) ?: return false
            context.contentResolver.update(
                uri(id),
                ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name.replace(LOCKED_SUFFIX, "")) },
                null, null,
            ) > 0
        }.getOrDefault(false)
    }

    fun delete(context: Context, id: Long): Boolean {
        // Only ever touch our own dashcam clips, whatever ID the phone sends.
        if (displayName(context, id)?.startsWith("CarDash_") != true) return false
        return runCatching { context.contentResolver.delete(uri(id), null, null) > 0 }.getOrDefault(false)
    }

    /** The clip's file name, or null if [id] isn't one of the dashcam clips. */
    fun displayName(context: Context, id: Long): String? = context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
        "${MediaStore.MediaColumns._ID} = ? AND $SELECTION", arrayOf(id.toString(), FOLDER, "CarDash_%"), null,
    )?.use { if (it.moveToFirst()) it.getString(0) else null }

    /** "CarDash_back_20261019_214500_locked.mp4" → when that clip started, in the phone's time zone. */
    fun startFromName(name: String): Long? = runCatching {
        val stamp = Regex("""_(\d{8}_\d{6})""").find(name)?.groupValues?.get(1) ?: return null
        LocalDateTime.parse(stamp, NAME_TIME).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()

    private val NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private const val SELECTION =
        "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"

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
