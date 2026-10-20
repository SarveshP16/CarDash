package com.cardash.link.clips

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import com.cardash.link.proto.ClipInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Thumbnails and "save to this phone" for the car's dashcam clips. */
object ClipFiles {
    private const val TAG = "ClipFiles"
    private const val FOLDER = "Movies/CarDash"
    private val NAME_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val thumbs = LruCache<String, Bitmap>(80)

    private val _progress = MutableStateFlow<Map<Long, Float>>(emptyMap())
    /** Clips being saved, 0..1. */
    val progress: StateFlow<Map<Long, Float>> = _progress.asStateFlow()

    private val _saved = MutableStateFlow<Set<String>>(emptySet())
    /** File names already saved to this phone. */
    val saved: StateFlow<Set<String>> = _saved.asStateFlow()

    fun fileName(clip: ClipInfo): String {
        val time = Instant.ofEpochMilli(clip.startMillis).atZone(ZoneId.systemDefault()).format(NAME_TIME)
        return "CarDash_${clip.side}_$time.mp4"
    }

    suspend fun thumbnail(url: String): Bitmap? {
        thumbs.get(url)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 5_000
                c.readTimeout = 10_000
                try {
                    if (c.responseCode != 200) null else c.inputStream.use(BitmapFactory::decodeStream)
                } finally {
                    c.disconnect()
                }
            }.getOrNull()?.also { thumbs.put(url, it) }
        }
    }

    /** Which clips are already in this phone's Movies/CarDash. */
    fun refreshSaved(context: Context) {
        scope.launch {
            val names = mutableSetOf<String>()
            runCatching {
                context.contentResolver.query(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                    "${MediaStore.MediaColumns.RELATIVE_PATH} = ?", arrayOf("$FOLDER/"), null,
                )?.use { c -> while (c.moveToNext()) names += c.getString(0) }
            }
            _saved.value = names
        }
    }

    /** Copies the clip into this phone's Movies/CarDash (visible in Photos), with progress. */
    fun save(context: Context, clip: ClipInfo, url: String) {
        if (clip.id in _progress.value) return
        val app = context.applicationContext
        _progress.update { it + (clip.id to 0f) }
        scope.launch {
            val name = fileName(clip)
            val resolver = app.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH, FOLDER)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val target = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            val ok = target != null && runCatching {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 5_000
                c.readTimeout = 30_000
                try {
                    check(c.responseCode == 200) { "HTTP ${c.responseCode}" }
                    val total = c.contentLengthLong.takeIf { it > 0 } ?: clip.sizeBytes
                    resolver.openOutputStream(target)!!.use { out ->
                        c.inputStream.use { input ->
                            val buffer = ByteArray(256 * 1024)
                            var done = 0L
                            var lastShown = 0f
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                out.write(buffer, 0, n)
                                done += n
                                val p = done.toFloat() / total
                                if (p - lastShown >= 0.01f) {
                                    lastShown = p
                                    _progress.update { it + (clip.id to p) }
                                }
                            }
                            check(done >= total) { "Download cut short" }
                        }
                    }
                } finally {
                    c.disconnect()
                }
                resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            }.onFailure { Log.w(TAG, "Save failed", it) }.isSuccess
            if (!ok && target != null) runCatching { resolver.delete(target, null, null) }
            _progress.update { it - clip.id }
            if (ok) _saved.update { it + name }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    app,
                    if (ok) "Saved to Movies/CarDash" else "Couldn't save the clip. Is the car still connected?",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }
}
