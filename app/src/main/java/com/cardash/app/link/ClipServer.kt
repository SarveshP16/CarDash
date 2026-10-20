package com.cardash.app.link

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import android.util.Size
import com.cardash.app.camera.DashcamStorage
import com.cardash.link.proto.HttpRange
import com.cardash.link.proto.LinkProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Serves dashcam videos and thumbnails to CarDash Link over plain HTTP, so the phone can stream
 * (with seeking) or save a clip. Runs only while the link is connected, and every URL carries a
 * random key that's handed over the authenticated link and changes each time.
 */
object ClipServer {
    private const val TAG = "ClipServer"
    const val PORT = 47820
    private const val BUFFER = 64 * 1024

    private var scope: CoroutineScope? = null
    private var server: ServerSocket? = null
    @Volatile var key: String = ""
        private set
    private val thumbs = LruCache<Long, ByteArray>(60)

    val isRunning: Boolean get() = server != null

    @Synchronized
    fun start(context: Context) {
        if (server != null) return
        val app = context.applicationContext
        key = LinkProtocol.randomHex(16)
        val ss = runCatching { ServerSocket().apply { reuseAddress = true; bind(InetSocketAddress(PORT)) } }
            .getOrElse { Log.w(TAG, "Couldn't open port $PORT", it); return }
        server = ss
        val s = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = s
        s.launch {
            while (isActive) {
                val socket = runCatching { ss.accept() }.getOrNull() ?: break
                launch { runCatching { handle(app, socket) }.onFailure { Log.d(TAG, "Request ended: ${it.message}") } }
            }
        }
    }

    @Synchronized
    fun stop() {
        runCatching { server?.close() }
        server = null
        scope?.cancel()
        scope = null
        key = ""
    }

    private fun handle(context: Context, socket: Socket): Unit = socket.use {
        // Only the phone on the same hotspot; never anything from outside.
        val from = socket.inetAddress
        if (from == null || !(from.isSiteLocalAddress || from.isLinkLocalAddress)) return
        socket.soTimeout = 30_000
        val input = socket.getInputStream().bufferedReader(Charsets.ISO_8859_1)
        val requestLine = input.readLine() ?: return
        val headers = generateSequence { input.readLine()?.takeIf { it.isNotEmpty() } }
            .mapNotNull { line -> line.indexOf(':').takeIf { it > 0 }?.let { line.substring(0, it).trim().lowercase() to line.substring(it + 1).trim() } }
            .toMap()
        val parts = requestLine.split(' ')
        val method = parts.getOrNull(0)
        val path = parts.getOrNull(1)?.trim('/')?.split('/') ?: emptyList()
        val out = socket.getOutputStream()
        // Path: <key>/clip/<id> or <key>/thumb/<id>
        val id = path.getOrNull(2)?.toLongOrNull()
        if ((method != "GET" && method != "HEAD") || path.size != 3 || key.isEmpty() ||
            !LinkProtocol.sameMac(path[0], key) || id == null || DashcamStorage.displayName(context, id) == null
        ) {
            status(out, "404 Not Found")
            return
        }
        when (path[1]) {
            "clip" -> sendClip(context, id, headers["range"], method == "HEAD", out)
            "thumb" -> sendThumb(context, id, out)
            else -> status(out, "404 Not Found")
        }
    }

    private fun sendClip(context: Context, id: Long, rangeHeader: String?, headOnly: Boolean, out: OutputStream) {
        val pfd = context.contentResolver.openFileDescriptor(DashcamStorage.uri(id), "r") ?: return status(out, "404 Not Found")
        pfd.use {
            val size = pfd.statSize
            val range = HttpRange.parse(rangeHeader, size)
            if (range == LongRange.EMPTY) {
                write(out, "HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$size\r\nConnection: close\r\n\r\n")
                return
            }
            val first = range?.first ?: 0L
            val last = range?.last ?: (size - 1)
            val length = last - first + 1
            val head = buildString {
                append(if (range != null) "HTTP/1.1 206 Partial Content\r\n" else "HTTP/1.1 200 OK\r\n")
                append("Content-Type: video/mp4\r\nAccept-Ranges: bytes\r\nContent-Length: $length\r\n")
                if (range != null) append("Content-Range: bytes $first-$last/$size\r\n")
                append("Connection: close\r\n\r\n")
            }
            write(out, head)
            if (headOnly) return
            FileInputStream(pfd.fileDescriptor).use { file ->
                file.channel.position(first)
                val buffer = ByteArray(BUFFER)
                var remaining = length
                while (remaining > 0) {
                    val n = file.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (n < 0) break
                    out.write(buffer, 0, n)
                    remaining -= n
                }
            }
            out.flush()
        }
    }

    private fun sendThumb(context: Context, id: Long, out: OutputStream) {
        val jpeg = thumbs.get(id) ?: runCatching {
            val bitmap = context.contentResolver.loadThumbnail(DashcamStorage.uri(id), Size(320, 180), null)
            ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 75, it) }.toByteArray()
        }.getOrNull()?.also { thumbs.put(id, it) }
        if (jpeg == null) return status(out, "404 Not Found")
        write(out, "HTTP/1.1 200 OK\r\nContent-Type: image/jpeg\r\nContent-Length: ${jpeg.size}\r\nConnection: close\r\n\r\n")
        out.write(jpeg)
        out.flush()
    }

    private fun status(out: OutputStream, status: String) {
        try {
            write(out, "HTTP/1.1 $status\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")
        } catch (_: IOException) {
        }
    }

    private fun write(out: OutputStream, text: String) {
        out.write(text.toByteArray(Charsets.ISO_8859_1))
        out.flush()
    }

    /** Forget a deleted clip's thumbnail. */
    fun forget(id: Long) {
        thumbs.remove(id)
    }
}
