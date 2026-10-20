package com.cardash.link

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.cardash.link.media.PhoneMedia
import com.cardash.link.media.PhoneTrack
import com.cardash.link.proto.ClipInfo
import com.cardash.link.proto.LinkConnection
import com.cardash.link.proto.LinkPlace
import com.cardash.link.proto.LinkProtocol
import com.cardash.link.proto.Msg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

/** The car's dashcam clips, and where to fetch their videos and thumbnails. */
data class CarClips(val baseUrl: String, val items: List<ClipInfo>, val usedBytes: Long, val limitBytes: Long) {
    fun videoUrl(id: Long) = "$baseUrl/clip/$id"
    fun thumbUrl(id: Long) = "$baseUrl/thumb/$id"
}

sealed interface LinkStatus {
    data object NotPaired : LinkStatus
    /** Paired; the car connects when it joins this phone's hotspot. */
    data class Waiting(val carName: String) : LinkStatus
    data class Connected(val carName: String) : LinkStatus
}

/**
 * The phone end of the link. Listens on the hotspot for the car phone, checks it's the paired car,
 * then keeps both sides in sync: music → car, places/route ← car, and anything sent from here.
 */
object LinkServer {
    private const val TAG = "LinkServer"
    private const val PAIR_WINDOW_MS = 5 * 60_000L
    private const val MAX_PAIR_ATTEMPTS = 5
    /** A place shared while the car is away is still sent if it connects within this time. */
    private const val PENDING_MAX_AGE_MS = 30 * 60_000L
    private const val ART_SIZE = 240

    private const val K_TOKEN = "token"
    private const val K_CAR = "carName"
    private const val K_PLACES = "places"

    private lateinit var app: Context
    private lateinit var prefs: SharedPreferences
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** All writes go through one thread, so messages keep their order. */
    private val writes = Dispatchers.IO.limitedParallelism(1)

    private val _status = MutableStateFlow<LinkStatus>(LinkStatus.NotPaired)
    val status: StateFlow<LinkStatus> = _status.asStateFlow()

    private val _places = MutableStateFlow<Msg.Places?>(null)
    /** Home, favourites and recents as the car has them (cached for when it's away). */
    val places: StateFlow<Msg.Places?> = _places.asStateFlow()

    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route.asStateFlow()

    private val _pairCode = MutableStateFlow<String?>(null)
    val pairCode: StateFlow<String?> = _pairCode.asStateFlow()

    private val _pending = MutableStateFlow<LinkPlace?>(null)
    /** Shared while the car wasn't connected; sent as soon as it connects. */
    val pending: StateFlow<LinkPlace?> = _pending.asStateFlow()
    private var pendingAt = 0L

    private val _clips = MutableStateFlow<CarClips?>(null)
    /** Null until asked for (and while the car isn't connected). */
    val clips: StateFlow<CarClips?> = _clips.asStateFlow()

    private val _suggestions = MutableStateFlow<List<LinkPlace>>(emptyList())
    val suggestions: StateFlow<List<LinkPlace>> = _suggestions.asStateFlow()
    private var suggestId = 0

    private var pairAttempts = 0
    private var server: ServerSocket? = null
    private var serverJob: Job? = null
    private val pendingLogins = AtomicInteger(0)
    private const val MAX_PENDING_LOGINS = 4
    @Volatile private var conn: LinkConnection? = null
    private var lastArtKey: String? = null

    fun init(context: Context) {
        app = context.applicationContext
        prefs = app.getSharedPreferences("link", Context.MODE_PRIVATE)
        _places.value = prefs.getString(K_PLACES, null)?.let(Msg::decode) as? Msg.Places
        _status.value = idleStatus()
        scope.launch {
            PhoneMedia.track.collect { sendTrack(it) }
        }
    }

    private fun idleStatus(): LinkStatus =
        if (prefs.getString(K_TOKEN, null) == null) LinkStatus.NotPaired
        else LinkStatus.Waiting(prefs.getString(K_CAR, null) ?: "CarDash")

    val isConnected: Boolean get() = conn != null

    /** Starts listening (idempotent). Called by [LinkService]. */
    fun start() {
        if (serverJob?.isActive == true) return
        serverJob = scope.launch {
            while (isActive) {
                try {
                    ServerSocket().use { ss ->
                        ss.reuseAddress = true
                        ss.bind(InetSocketAddress(LinkProtocol.PORT))
                        server = ss
                        Log.i(TAG, "Listening on ${LinkProtocol.PORT}")
                        while (isActive) {
                            val socket = ss.accept()
                            launch { handle(socket) }
                        }
                    }
                } catch (e: IOException) {
                    if (!isActive) break
                    Log.w(TAG, "Server socket failed, retrying", e)
                    delay(3_000)
                }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
        runCatching { server?.close() }
        conn?.close()
    }

    private fun handle(socket: Socket) {
        // Only devices on a local network (the hotspot) may connect, never one from the internet.
        val address = socket.inetAddress
        if (address == null || !(address.isSiteLocalAddress || address.isLinkLocalAddress || address.isLoopbackAddress)) {
            socket.close()
            return
        }
        // Only a few connections may be logging in at once, so a device on the hotspot can't tie the link up.
        if (pendingLogins.incrementAndGet() > MAX_PENDING_LOGINS) {
            pendingLogins.decrementAndGet()
            socket.close()
            return
        }
        val c = runCatching { LinkConnection(socket) }.getOrElse {
            pendingLogins.decrementAndGet()
            socket.close()
            return
        }
        try {
            val carName = try {
                c.timeout(LinkProtocol.HANDSHAKE_TIMEOUT_MS)
                handshake(c)
            } finally {
                pendingLogins.decrementAndGet()
            } ?: return
            c.timeout(LinkProtocol.TIMEOUT_MS)
            conn?.close()
            conn = c
            lastArtKey = null
            _status.value = LinkStatus.Connected(carName)
            Log.i(TAG, "Car connected from ${c.remoteAddress}")
            onConnected()
            while (true) {
                val msg = c.read() ?: break
                onMessage(msg, c)
            }
        } catch (e: IOException) {
            Log.i(TAG, "Link closed: ${e.message}")
        } finally {
            c.close()
            if (conn === c) {
                conn = null
                _route.value = null
                _clips.value = null
                _status.value = idleStatus()
            }
        }
    }

    /** Returns the car's name when it proved it's the paired car, or just paired; null otherwise. */
    private fun handshake(c: LinkConnection): String? {
        val nonce = LinkProtocol.randomHex(16)
        c.send(Msg.Challenge(nonce, phoneName()))
        return when (val m = c.read()) {
            is Msg.Auth -> {
                val token = prefs.getString(K_TOKEN, null)
                if (token == null || !LinkProtocol.sameMac(m.mac, LinkProtocol.carProof(token, nonce, m.carNonce))) {
                    c.send(Msg.Denied("This car isn't paired with ${phoneName()}. Pair again."))
                    null
                } else {
                    // Prove this is the paired phone too, so nothing else can pose as it to the car.
                    c.send(Msg.Ok(LinkProtocol.phoneProof(token, nonce, m.carNonce)))
                    m.carName.ifBlank { "CarDash" }
                }
            }
            is Msg.Pair -> synchronized(this) {
                val code = _pairCode.value
                when {
                    code == null -> { c.send(Msg.Denied("Tap \"Pair with car\" in CarDash Link first")); null }
                    pairAttempts >= MAX_PAIR_ATTEMPTS -> { c.send(Msg.Denied("Too many wrong codes. Start pairing again.")); null }
                    m.code.trim() != code -> {
                        pairAttempts++
                        c.send(Msg.Denied("Wrong code"))
                        null
                    }
                    else -> {
                        val token = LinkProtocol.randomHex(32)
                        val name = m.carName.ifBlank { "CarDash" }
                        prefs.edit().putString(K_TOKEN, token).putString(K_CAR, name).apply()
                        _pairCode.value = null
                        // The proof shows the car it's talking to the phone that displayed the code.
                        c.send(Msg.Paired(token, LinkProtocol.phoneProof(code, nonce, m.carNonce)))
                        name
                    }
                }
            }
            else -> null
        }
    }

    private fun onConnected() {
        sendTrack(PhoneMedia.track.value)
        val p = _pending.value
        if (p != null) {
            if (System.currentTimeMillis() - pendingAt < PENDING_MAX_AGE_MS) send(Msg.Navigate(p))
            _pending.value = null
        }
    }

    private fun onMessage(msg: Msg, c: LinkConnection) {
        when (msg) {
            Msg.Ping -> send(Msg.Pong)
            is Msg.Command -> scope.launch(Dispatchers.Main) {
                when (msg.action) {
                    Msg.Action.PLAY -> PhoneMedia.play()
                    Msg.Action.PAUSE -> PhoneMedia.pause()
                    Msg.Action.TOGGLE -> PhoneMedia.toggle()
                    Msg.Action.NEXT -> PhoneMedia.next()
                    Msg.Action.PREVIOUS -> PhoneMedia.previous()
                    Msg.Action.RESUME -> PhoneMedia.resumeWhenCarAudioReady()
                }
            }
            is Msg.Places -> {
                _places.value = msg
                prefs.edit().putString(K_PLACES, Msg.encode(msg)).apply()
            }
            is Msg.Route -> _route.value = msg.destination
            is Msg.Suggestions -> if (msg.id == suggestId) _suggestions.value = msg.items
            is Msg.Clips -> _clips.value = CarClips(
                "http://${c.remoteAddress}:${msg.port}/${msg.key}", msg.items, msg.usedBytes, msg.limitBytes,
            )
            else -> Unit
        }
    }

    private fun send(msg: Msg): Boolean {
        val c = conn ?: return false
        scope.launch(writes) {
            try {
                c.send(msg)
            } catch (e: IOException) {
                c.close()
            }
        }
        return true
    }

    private fun sendTrack(track: PhoneTrack?) {
        if (conn == null) return
        scope.launch(writes) {
            if (track == null) {
                send(Msg.NothingPlaying)
                return@launch
            }
            // Album art only travels when the song changes.
            val key = "${track.title}|${track.artist}"
            val art = if (key != lastArtKey) track.art?.let(::encodeArt) else null
            lastArtKey = key
            send(Msg.NowPlaying(track.title, track.artist, track.app, track.playing, key, art))
        }
    }

    private fun encodeArt(bitmap: Bitmap): String? = runCatching {
        val scale = ART_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
        val small = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else bitmap
        val out = ByteArrayOutputStream()
        small.compress(Bitmap.CompressFormat.JPEG, 80, out)
        Base64.getEncoder().encodeToString(out.toByteArray())
    }.getOrNull()

    // ---- Called from the UI and the share sheet ----

    /** Sends [place] to the car now, or queues it until the car connects. True if sent now. */
    fun navigate(place: LinkPlace): Boolean {
        if (send(Msg.Navigate(place))) return true
        _pending.value = place
        pendingAt = System.currentTimeMillis()
        return false
    }

    fun cancelPending() {
        _pending.value = null
    }

    fun suggest(query: String) {
        suggestId++
        if (query.isBlank()) {
            _suggestions.value = emptyList()
            return
        }
        send(Msg.Suggest(suggestId, query.trim()))
    }

    fun saveFavourite(place: LinkPlace) = send(Msg.SaveFavourite(place))
    fun removeFavourite(place: LinkPlace) = send(Msg.RemoveFavourite(place))
    fun setHome(place: LinkPlace) = send(Msg.SetHome(place))
    fun listClips() = send(Msg.ListClips)
    fun clipAction(id: Long, op: Msg.ClipOp) = send(Msg.ClipAction(id, op))
    fun endRoute() = send(Msg.EndRoute)

    /** Shows a code to type into CarDash; valid for 5 minutes. */
    fun startPairing(): String {
        val code = LinkProtocol.pairCode()
        synchronized(this) {
            _pairCode.value = code
            pairAttempts = 0
        }
        scope.launch {
            delay(PAIR_WINDOW_MS)
            if (_pairCode.value == code) _pairCode.value = null
        }
        return code
    }

    fun cancelPairing() {
        _pairCode.value = null
    }

    fun forgetCar() {
        prefs.edit().remove(K_TOKEN).remove(K_CAR).remove(K_PLACES).apply()
        _places.value = null
        conn?.close()
        _status.value = LinkStatus.NotPaired
    }

    private fun phoneName(): String =
        Settings.Global.getString(app.contentResolver, Settings.Global.DEVICE_NAME) ?: Build.MODEL
}

