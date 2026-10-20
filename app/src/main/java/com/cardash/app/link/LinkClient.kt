package com.cardash.app.link

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast
import com.cardash.app.camera.DashcamStorage
import com.cardash.app.car.CarRoutines
import com.cardash.app.location.LocationRepository
import com.cardash.app.nav.Destination
import com.cardash.app.nav.InAppNavigator
import com.cardash.app.nav.NavState
import com.cardash.app.nav.PlacesSearch
import com.cardash.app.nav.RouteStarter
import com.cardash.app.settings.Favourites
import com.cardash.app.settings.Settings
import com.cardash.app.settings.SettingsStore
import com.cardash.link.proto.LinkConnection
import com.cardash.link.proto.LinkPlace
import com.cardash.link.proto.LinkProtocol
import com.cardash.link.proto.Msg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.Base64

sealed interface LinkState {
    /** [note]: why, when pairing just failed or the phone forgot this car. */
    data class NotPaired(val note: String? = null) : LinkState
    data class Searching(val note: String) : LinkState
    data object Connecting : LinkState
    data class Connected(val phoneName: String) : LinkState
}

/** What's playing on the main phone. */
data class RemoteTrack(val title: String, val artist: String, val app: String, val playing: Boolean, val art: Bitmap?)

/**
 * The car end of CarDash Link. The main phone hosts the hotspot this phone joins, so the phone is
 * always the Wi-Fi gateway: connect there, prove we're the paired car, then stay in sync until
 * the hotspot goes away. Retries quietly in the background.
 */
object LinkClient {
    private const val TAG = "LinkClient"
    private const val CAR_NAME = "CarDash"
    private const val CONNECT_TIMEOUT_MS = 4_000
    private const val RETRY_MS = 5_000L
    /** On a Wi-Fi without the phone (home), try less often. */
    private const val NOT_FOUND_RETRY_MS = 20_000L
    /** Music counts as "was playing" at Car Off if it played this recently (the car's Bluetooth audio drops first). */
    private const val RECENTLY_PLAYING_MS = 90_000L
    /** Car On resumes phone music only if the link comes up within this time. */
    private const val RESUME_WINDOW_MS = 3 * 60_000L
    private const val PREFS = "cardash_link"
    private const val K_RESUME = "phoneWasPlaying"

    private val _state = MutableStateFlow<LinkState>(LinkState.NotPaired())
    val state: StateFlow<LinkState> = _state.asStateFlow()

    private val _track = MutableStateFlow<RemoteTrack?>(null)
    val track: StateFlow<RemoteTrack?> = _track.asStateFlow()

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val main = MainScope()
    /** One writer thread keeps messages in order. */
    private val writes = Dispatchers.IO.limitedParallelism(1)
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private var job: Job? = null

    @Volatile private var conn: LinkConnection? = null
    @Volatile private var pairCode: String? = null
    private var artKey: String? = null
    private var art: Bitmap? = null
    @Volatile private var lastPlayingAt = 0L
    @Volatile private var resumeUntil = 0L
    private var suggestSession = PlacesSearch.newSession()
    private var suggestSessionAt = 0L

    val isConnected: Boolean get() = conn != null

    fun start(context: Context) {
        if (job != null) return
        app = context.applicationContext
        job = scope.launch { loop() }
        // Keep the phone's copy of Home, favourites and recents current.
        scope.launch {
            SettingsStore.settings.map(::placesMessage).distinctUntilChanged().collect { send(it) }
        }
        scope.launch {
            InAppNavigator.state
                .map { (it as? NavState.Guiding)?.destination?.ifBlank { "your destination" } }
                .distinctUntilChanged()
                .collect { send(Msg.Route(it)) }
        }
    }

    // ---- Settings → Phone link ----

    /** Pairs with the code CarDash Link shows; the car phone must be on the main phone's hotspot. */
    fun pair(code: String) {
        pairCode = code.filter(Char::isDigit)
        conn?.close()
        _state.value = LinkState.Connecting
        wake.trySend(Unit)
    }

    fun unpair() {
        SettingsStore.update { it.copy(linkToken = null, linkPhoneName = null) }
        conn?.close()
        _state.value = LinkState.NotPaired()
    }

    // ---- Music on the main phone ----

    fun command(action: Msg.Action) {
        send(Msg.Command(action))
    }

    /** Car Off: remember whether phone music was playing, and pause it. True if the phone got the message. */
    fun onCarOff(context: Context, s: Settings): Boolean {
        val wasPlaying = _track.value?.playing == true || System.currentTimeMillis() - lastPlayingAt < RECENTLY_PLAYING_MS
        prefs(context).edit().putBoolean(K_RESUME, wasPlaying).apply()
        resumeUntil = 0
        return s.pauseMediaOnCarOff && send(Msg.Command(Msg.Action.PAUSE))
    }

    /** Car On: resume phone music (once the car's audio is connected) if it was playing at Car Off. */
    fun onCarOn(context: Context, s: Settings) {
        val prefs = prefs(context)
        val wasPlaying = prefs.getBoolean(K_RESUME, false)
        prefs.edit().remove(K_RESUME).apply()
        if (!s.resumeMusicOnCarOn || !wasPlaying) return
        if (send(Msg.Command(Msg.Action.RESUME))) return
        resumeUntil = System.currentTimeMillis() + RESUME_WINDOW_MS
        wake.trySend(Unit)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Connection ----

    private suspend fun loop() {
        while (scope.isActive) {
            val token = SettingsStore.current.linkToken
            val code = pairCode
            if (token == null && code == null) {
                if (_state.value !is LinkState.NotPaired) _state.value = LinkState.NotPaired()
                sleep(RETRY_MS)
                continue
            }
            val gateway = wifiGateway()
            if (gateway == null) {
                if (code != null) {
                    pairCode = null
                    _state.value = LinkState.NotPaired("Join your main phone's hotspot first, then pair")
                } else {
                    _state.value = LinkState.Searching("Waiting for your phone's hotspot")
                }
                sleep(RETRY_MS)
                continue
            }
            var retry = RETRY_MS
            try {
                session(gateway.first, gateway.second, token, code)
            } catch (e: IOException) {
                Log.i(TAG, "Link failed: ${e.message}")
                if (code != null) {
                    pairCode = null
                    _state.value = LinkState.NotPaired("Couldn't reach CarDash Link. Is this phone on its hotspot?")
                } else if (_state.value !is LinkState.Connected) {
                    _state.value = LinkState.Searching("CarDash Link not found on this Wi-Fi")
                    retry = NOT_FOUND_RETRY_MS
                }
            } finally {
                conn = null
                _track.value = null
                if (_state.value is LinkState.Connected) _state.value = LinkState.Searching("Reconnecting to your phone")
            }
            sleep(retry)
        }
    }

    /** Waits [ms], or less if something (pairing, Car On) wants a connection attempt now. */
    private suspend fun sleep(ms: Long) {
        withTimeoutOrNull(ms) { wake.receive() }
    }

    private fun wifiGateway(): Pair<Network, InetAddress>? {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        @Suppress("DEPRECATION")
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue
            val gateway = cm.getLinkProperties(network)?.routes
                ?.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway ?: continue
            return network to gateway
        }
        return null
    }

    private suspend fun session(network: Network, gateway: InetAddress, token: String?, code: String?) {
        if (code == null) _state.value = LinkState.Connecting
        val socket = network.socketFactory.createSocket()
        socket.connect(InetSocketAddress(gateway, LinkProtocol.PORT), CONNECT_TIMEOUT_MS)
        val c = LinkConnection(socket)
        try {
            val challenge = c.read() as? Msg.Challenge ?: throw IOException("No challenge from the phone")
            if (code != null) {
                pairCode = null
                c.send(Msg.Pair(code, CAR_NAME))
                when (val reply = c.read()) {
                    is Msg.Paired -> SettingsStore.update {
                        it.copy(linkToken = reply.token, linkPhoneName = challenge.phoneName)
                    }
                    is Msg.Denied -> {
                        _state.value = LinkState.NotPaired(reply.reason)
                        return
                    }
                    else -> throw IOException("Unexpected reply to pairing")
                }
            } else {
                c.send(Msg.Auth(LinkProtocol.mac(token!!, challenge.nonce), CAR_NAME))
                when (val reply = c.read()) {
                    Msg.Ok -> if (SettingsStore.current.linkPhoneName != challenge.phoneName) {
                        SettingsStore.update { it.copy(linkPhoneName = challenge.phoneName) }
                    }
                    is Msg.Denied -> {
                        // The phone forgot this car (or was reset): pairing must be redone.
                        SettingsStore.update { it.copy(linkToken = null, linkPhoneName = null) }
                        _state.value = LinkState.NotPaired(reply.reason)
                        return
                    }
                    else -> throw IOException("Unexpected reply to auth")
                }
            }

            conn = c
            _state.value = LinkState.Connected(challenge.phoneName)
            Log.i(TAG, "Connected to ${challenge.phoneName}")
            send(placesMessage(SettingsStore.current))
            send(Msg.Route((InAppNavigator.state.value as? NavState.Guiding)?.destination?.ifBlank { "your destination" }))
            if (System.currentTimeMillis() < resumeUntil) {
                resumeUntil = 0
                send(Msg.Command(Msg.Action.RESUME))
            }
            val pinger = scope.launch {
                while (isActive) {
                    delay(LinkProtocol.PING_MS)
                    send(Msg.Ping)
                }
            }
            try {
                while (true) {
                    val msg = c.read() ?: break
                    onMessage(msg)
                }
            } finally {
                pinger.cancel()
                // Clip videos are only reachable while the link is up.
                ClipServer.stop()
            }
        } finally {
            c.close()
        }
    }

    private fun send(msg: Msg): Boolean {
        val c = conn ?: return false
        scope.launch(writes) {
            try {
                c.send(msg)
            } catch (_: IOException) {
                c.close()
            }
        }
        return true
    }

    private suspend fun onMessage(msg: Msg) {
        when (msg) {
            is Msg.NowPlaying -> {
                if (msg.art != null) {
                    art = runCatching {
                        val bytes = Base64.getDecoder().decode(msg.art)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                    artKey = msg.artKey
                } else if (msg.artKey != artKey) {
                    art = null
                    artKey = msg.artKey
                }
                if (msg.playing) lastPlayingAt = System.currentTimeMillis()
                _track.value = RemoteTrack(msg.title, msg.artist, msg.app, msg.playing, art)
            }
            Msg.NothingPlaying -> _track.value = null
            is Msg.Navigate -> withContext(Dispatchers.Main) { navigate(msg.place) }
            is Msg.Suggest -> scope.launch { suggest(msg) }
            is Msg.SaveFavourite -> withContext(Dispatchers.Main) { Favourites.add(msg.place.toDestination()) }
            is Msg.RemoveFavourite -> withContext(Dispatchers.Main) {
                val p = msg.place
                SettingsStore.current.favourites
                    .firstOrNull { (p.placeId != null && it.placeId == p.placeId) || it.address.equals(p.address, true) }
                    ?.let(Favourites::remove)
            }
            Msg.ListClips -> sendClips()
            is Msg.ClipAction -> {
                val done = when (msg.action) {
                    Msg.ClipOp.LOCK -> DashcamStorage.setLocked(app, msg.id, true)
                    Msg.ClipOp.UNLOCK -> DashcamStorage.setLocked(app, msg.id, false)
                    Msg.ClipOp.DELETE -> DashcamStorage.delete(app, msg.id).also { if (it) ClipServer.forget(msg.id) }
                }
                if (!done) Log.w(TAG, "Clip ${msg.action} failed for ${msg.id}")
                sendClips()
            }
            is Msg.SetHome -> withContext(Dispatchers.Main) {
                SettingsStore.update { it.copy(home = Destination("Home", msg.place.address, msg.place.placeId)) }
            }
            Msg.EndRoute -> withContext(Dispatchers.Main) { InAppNavigator.stop() }
            else -> Unit
        }
    }

    private fun navigate(place: LinkPlace) {
        val picked = place.placeId?.let {
            PlacesSearch.Suggestion(it, place.name, place.address.removePrefix("${place.name}, "), null)
        }
        CarRoutines.showDashboard(app)
        Toast.makeText(app, "From your phone: ${place.name}", Toast.LENGTH_SHORT).show()
        RouteStarter.go(app, main, place.address, picked, label = place.name)
    }

    /** The dashcam clip list, starting the video server the phone streams from. */
    private fun sendClips() {
        ClipServer.start(app)
        val clips = runCatching { DashcamStorage.listForLink(app) }.getOrElse {
            Log.w(TAG, "Couldn't list clips", it)
            emptyList()
        }
        val s = SettingsStore.current
        send(
            Msg.Clips(
                ClipServer.PORT, ClipServer.key, clips,
                usedBytes = clips.sumOf { it.sizeBytes },
                limitBytes = if (s.dashcamLoop) s.dashcamLimitGb * 1_000_000_000L else 0,
            ),
        )
    }

    /** Place search runs here, on the car phone's API key, so the main phone needs no key of its own. */
    private suspend fun suggest(msg: Msg.Suggest) {
        val now = System.currentTimeMillis()
        // Keystrokes a few minutes apart are separate searches.
        if (now - suggestSessionAt > 3 * 60_000L) suggestSession = PlacesSearch.newSession()
        suggestSessionAt = now
        val results = PlacesSearch.suggest(app, msg.query, LocationRepository.fix.value, suggestSession).orEmpty()
        send(Msg.Suggestions(msg.id, results.map { LinkPlace(it.name, it.fullText, it.placeId, it.distanceMeters) }))
    }

    private fun placesMessage(s: Settings) = Msg.Places(
        home = s.home.takeIf { it.isSet }?.toLink(),
        favourites = s.favourites.map { it.toLink() },
        recents = s.recents.map { it.toLink() },
    )

    private fun Destination.toLink() = LinkPlace(label, address, placeId)
    private fun LinkPlace.toDestination() = Destination(name, address, placeId)
}
