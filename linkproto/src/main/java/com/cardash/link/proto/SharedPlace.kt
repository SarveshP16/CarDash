package com.cardash.link.proto

import java.net.URLDecoder

/**
 * Turns what Google Maps (or anything else) shares into a place to route to. Shared text is
 * usually the place name, maybe its address, then a link: "Bunnings Parramatta\nhttps://maps.app.goo.gl/…".
 */
object SharedPlace {
    private val urlRegex = Regex("""https?://\S+""")
    private const val NUM = """-?\d{1,3}\.\d+"""
    // Data parameters "!3d<lat>!4d<lng>" are the place's own position; "@lat,lng" is only where the map was centred.
    private val dataCoords = Regex("""!3d($NUM)!4d($NUM)""")
    private val atCoords = Regex("""@($NUM),($NUM)""")
    private val queryCoords = Regex("""[?&](?:q|query|ll|destination|daddr)=($NUM)(?:,|%2C)\s*($NUM)""", RegexOption.IGNORE_CASE)
    private val queryText = Regex("""[?&](?:q|query|destination|daddr)=([^&]+)""", RegexOption.IGNORE_CASE)
    private val placeName = Regex("""/maps/place/([^/@?]+)""")

    data class Shared(val name: String?, val details: String?, val url: String?)

    /** Splits shared text into the name (first line), any other text, and the first link. */
    fun parseText(text: String?, subject: String? = null): Shared {
        val all = text.orEmpty()
        val url = urlRegex.find(all)?.value?.trimEnd('.', ',', ')')
        val lines = all.replace(urlRegex, "").lines().map { it.trim().trim(',') }.filter { it.isNotEmpty() }
        val name = lines.firstOrNull() ?: subject?.trim()?.ifEmpty { null }
        val details = lines.drop(1).joinToString(", ").ifEmpty { null }
        return Shared(name, details, url)
    }

    /** Short links (maps.app.goo.gl, goo.gl/maps) must be followed to the full URL before [fromUrl]. */
    fun isShortLink(url: String): Boolean =
        Regex("""^https?://(maps\.app\.goo\.gl|goo\.gl/maps|g\.co/kgs)/""", RegexOption.IGNORE_CASE).containsMatchIn(url)

    /**
     * The place in a full Google Maps (or geo-style) URL. Coordinates win, since they route exactly;
     * otherwise the place name or search text. Null if the URL says nothing usable.
     */
    fun fromUrl(url: String, fallbackName: String? = null): LinkPlace? {
        val name = placeName.find(url)?.groupValues?.get(1)?.let(::decode)
            ?: queryText.find(url)?.groupValues?.get(1)?.let(::decode)?.takeIf { queryCoords.find(url) == null }
            ?: fallbackName
        val coords = dataCoords.find(url) ?: queryCoords.find(url) ?: atCoords.find(url)
        if (coords != null) {
            val (lat, lng) = coords.destructured
            if (lat.toDouble() in -90.0..90.0 && lng.toDouble() in -180.0..180.0) {
                return LinkPlace(name ?: "$lat,$lng", "$lat,$lng")
            }
        }
        return name?.let { LinkPlace(it, it) }
    }

    /** Best place from shared text after any short link was resolved to [resolvedUrl]. */
    fun place(shared: Shared, resolvedUrl: String?): LinkPlace? {
        val textQuery = listOfNotNull(shared.name, shared.details).joinToString(", ").ifEmpty { null }
        resolvedUrl?.let { fromUrl(it, shared.name) }?.let { fromLink ->
            // A link with only a name: the shared text (name + address) is the better search.
            val hasCoords = fromLink.address != fromLink.name
            return if (hasCoords || textQuery == null) fromLink else LinkPlace(shared.name ?: fromLink.name, textQuery)
        }
        return textQuery?.let { LinkPlace(shared.name ?: it, it) }
    }

    private fun decode(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s).trim()
}
