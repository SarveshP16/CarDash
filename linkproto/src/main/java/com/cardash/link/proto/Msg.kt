package com.cardash.link.proto

import org.json.JSONArray
import org.json.JSONObject

/** A place as both apps exchange it. [address] is what gets routed to: text, or "lat,lng". */
data class LinkPlace(
    val name: String,
    val address: String,
    val placeId: String? = null,
    val distanceMeters: Int? = null,
)

/**
 * A dashcam clip on the car phone. [startMillis] is when recording began; [side] is "front" or "back".
 * The video itself is fetched over HTTP from the car (see [Msg.Clips]), not sent over the link.
 */
data class ClipInfo(
    val id: Long,
    val side: String,
    val startMillis: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val locked: Boolean,
)

/**
 * Everything sent over the link, one JSON object per line. The phone is the server (it hosts the
 * hotspot); the car connects, answers a challenge, then both sides send whenever something changes.
 */
sealed interface Msg {
    // Handshake
    /** Phone → car, first thing on every connection. */
    data class Challenge(val nonce: String, val phoneName: String) : Msg
    /**
     * Car → phone: [mac] is [LinkProtocol.carProof] over both nonces with the token from pairing.
     * [carNonce] is the car's own challenge, which the phone must answer in [Ok].
     */
    data class Auth(val mac: String, val carName: String, val carNonce: String) : Msg
    /** Car → phone: the code shown on the phone, to pair for the first time. */
    data class Pair(val code: String, val carName: String, val carNonce: String) : Msg
    /** Phone → car: the new token, and [proof] that the phone knows the code ([LinkProtocol.phoneProof]). */
    data class Paired(val token: String, val proof: String) : Msg
    /** Phone → car: [proof] that the phone holds the token too ([LinkProtocol.phoneProof]). */
    data class Ok(val proof: String) : Msg
    data class Denied(val reason: String) : Msg
    data object Ping : Msg
    data object Pong : Msg

    // Phone → car
    /** [art] (base64 JPEG) is only sent when [artKey] changes; the car keeps the last one. */
    data class NowPlaying(
        val title: String,
        val artist: String,
        val app: String,
        val playing: Boolean,
        val artKey: String,
        val art: String?,
    ) : Msg
    data object NothingPlaying : Msg
    data class Navigate(val place: LinkPlace) : Msg
    data class Suggest(val id: Int, val query: String) : Msg
    data class SaveFavourite(val place: LinkPlace) : Msg
    data class RemoveFavourite(val place: LinkPlace) : Msg
    data class SetHome(val place: LinkPlace) : Msg
    data object EndRoute : Msg
    /** Asks the car for its dashcam clips; it answers with [Clips]. */
    data object ListClips : Msg
    data class ClipAction(val id: Long, val action: ClipOp) : Msg

    // Car → phone
    data class Command(val action: Action) : Msg
    data class Suggestions(val id: Int, val items: List<LinkPlace>) : Msg
    data class Places(val home: LinkPlace?, val favourites: List<LinkPlace>, val recents: List<LinkPlace>) : Msg
    /** [destination] null = not navigating. */
    data class Route(val destination: String?) : Msg
    /**
     * Newest first. Each video and thumbnail is served by the car at
     * http://<car address>:[port]/[key]/clip/<id> and .../thumb/<id>, only while the link is up.
     */
    data class Clips(val port: Int, val key: String, val items: List<ClipInfo>, val usedBytes: Long, val limitBytes: Long) : Msg

    enum class Action {
        PLAY, PAUSE, TOGGLE, NEXT, PREVIOUS,
        /** Car On: play once the car's audio (the Bluetooth AUX receiver) is connected. */
        RESUME,
    }

    enum class ClipOp { LOCK, UNLOCK, DELETE }

    companion object {
        fun encode(msg: Msg): String = JSONObject().apply {
            when (msg) {
                is Challenge -> put("t", "challenge").put("nonce", msg.nonce).put("phone", msg.phoneName)
                is Auth -> put("t", "auth").put("mac", msg.mac).put("car", msg.carName).put("nonce", msg.carNonce)
                is Pair -> put("t", "pair").put("code", msg.code).put("car", msg.carName).put("nonce", msg.carNonce)
                is Paired -> put("t", "paired").put("token", msg.token).put("proof", msg.proof)
                is Ok -> put("t", "ok").put("proof", msg.proof)
                is Denied -> put("t", "denied").put("reason", msg.reason)
                Ping -> put("t", "ping")
                Pong -> put("t", "pong")
                is NowPlaying -> put("t", "playing").put("title", msg.title).put("artist", msg.artist)
                    .put("app", msg.app).put("playing", msg.playing).put("artKey", msg.artKey)
                    .apply { msg.art?.let { put("art", it) } }
                NothingPlaying -> put("t", "nothing")
                is Navigate -> put("t", "navigate").put("place", place(msg.place))
                is Suggest -> put("t", "suggest").put("id", msg.id).put("query", msg.query)
                is SaveFavourite -> put("t", "fav+").put("place", place(msg.place))
                is RemoveFavourite -> put("t", "fav-").put("place", place(msg.place))
                is SetHome -> put("t", "home").put("place", place(msg.place))
                EndRoute -> put("t", "endRoute")
                ListClips -> put("t", "listClips")
                is ClipAction -> put("t", "clipAction").put("id", msg.id).put("action", msg.action.name)
                is Clips -> put("t", "clips").put("port", msg.port).put("key", msg.key)
                    .put("used", msg.usedBytes).put("limit", msg.limitBytes)
                    .put("items", JSONArray().apply { msg.items.forEach { put(clip(it)) } })
                is Command -> put("t", "cmd").put("action", msg.action.name)
                is Suggestions -> put("t", "suggestions").put("id", msg.id).put("items", places(msg.items))
                is Places -> put("t", "places").put("favourites", places(msg.favourites))
                    .put("recents", places(msg.recents)).apply { msg.home?.let { put("home", place(it)) } }
                is Route -> put("t", "route").apply { msg.destination?.let { put("destination", it) } }
            }
        }.toString()

        /** Null for anything unknown or malformed (e.g. from a newer version), which is ignored. */
        fun decode(line: String): Msg? = runCatching {
            val o = JSONObject(line)
            when (o.getString("t")) {
                "challenge" -> Challenge(o.getString("nonce"), o.optString("phone"))
                "auth" -> Auth(o.getString("mac"), o.optString("car"), o.getString("nonce"))
                "pair" -> Pair(o.getString("code"), o.optString("car"), o.getString("nonce"))
                "paired" -> Paired(o.getString("token"), o.getString("proof"))
                "ok" -> Ok(o.getString("proof"))
                "denied" -> Denied(o.optString("reason"))
                "ping" -> Ping
                "pong" -> Pong
                "playing" -> NowPlaying(
                    o.optString("title"), o.optString("artist"), o.optString("app"), o.optBoolean("playing"),
                    o.optString("artKey"), o.optString("art").ifEmpty { null },
                )
                "nothing" -> NothingPlaying
                "navigate" -> Navigate(place(o.getJSONObject("place")))
                "suggest" -> Suggest(o.getInt("id"), o.getString("query"))
                "fav+" -> SaveFavourite(place(o.getJSONObject("place")))
                "fav-" -> RemoveFavourite(place(o.getJSONObject("place")))
                "home" -> SetHome(place(o.getJSONObject("place")))
                "endRoute" -> EndRoute
                "listClips" -> ListClips
                "clipAction" -> ClipAction(o.getLong("id"), ClipOp.valueOf(o.getString("action")))
                "clips" -> o.getJSONArray("items").let { a ->
                    Clips(
                        o.getInt("port"), o.getString("key"), (0 until a.length()).map { clip(a.getJSONObject(it)) },
                        o.optLong("used"), o.optLong("limit"),
                    )
                }
                "cmd" -> Command(Action.valueOf(o.getString("action")))
                "suggestions" -> Suggestions(o.getInt("id"), places(o.getJSONArray("items")))
                "places" -> Places(
                    o.optJSONObject("home")?.let(::place),
                    places(o.getJSONArray("favourites")),
                    places(o.getJSONArray("recents")),
                )
                "route" -> Route(o.optString("destination").ifEmpty { null })
                else -> null
            }
        }.getOrNull()

        private fun place(p: LinkPlace) = JSONObject().put("name", p.name).put("address", p.address).apply {
            p.placeId?.let { put("placeId", it) }
            p.distanceMeters?.let { put("distance", it) }
        }

        private fun place(o: JSONObject) = LinkPlace(
            o.getString("name"),
            o.getString("address"),
            o.optString("placeId").ifEmpty { null },
            if (o.has("distance")) o.getInt("distance") else null,
        )

        private fun clip(c: ClipInfo) = JSONObject().put("id", c.id).put("side", c.side).put("start", c.startMillis)
            .put("duration", c.durationMs).put("size", c.sizeBytes).put("locked", c.locked)

        private fun clip(o: JSONObject) = ClipInfo(
            o.getLong("id"), o.getString("side"), o.getLong("start"),
            o.optLong("duration"), o.optLong("size"), o.optBoolean("locked"),
        )

        private fun places(list: List<LinkPlace>) = JSONArray().apply { list.forEach { put(place(it)) } }
        private fun places(a: JSONArray) = (0 until a.length()).map { place(a.getJSONObject(it)) }
    }
}
