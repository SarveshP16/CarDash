package com.cardash.link

import com.cardash.link.proto.LinkPlace
import com.cardash.link.proto.SharedPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/** Turns a Google Maps share into a place, following maps.app.goo.gl short links to the full URL. */
object MapsLinks {
    private const val MAX_HOPS = 8

    suspend fun placeFromShare(text: String?, subject: String?): LinkPlace? = withContext(Dispatchers.IO) {
        val shared = SharedPlace.parseText(text, subject)
        val url = shared.url?.let { if (SharedPlace.isShortLink(it)) follow(it) else it }
        SharedPlace.place(shared, url)
    }

    /** The URL a short link ends up at (or the last one reached if the network fails part way). */
    fun follow(start: String): String {
        var current = start
        repeat(MAX_HOPS) {
            val c = runCatching { URL(current).openConnection() as HttpURLConnection }.getOrNull() ?: return current
            try {
                c.instanceFollowRedirects = false
                c.connectTimeout = 6_000
                c.readTimeout = 6_000
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 16) CarDashLink")
                val location = c.getHeaderField("Location")
                if (c.responseCode !in 300..399 || location == null) return unwrapConsent(current)
                current = URL(URL(current), location).toString()
            } catch (_: Exception) {
                return unwrapConsent(current)
            } finally {
                c.disconnect()
            }
        }
        return unwrapConsent(current)
    }

    /** Google's cookie-consent page carries the real Maps URL in its "continue" parameter. */
    private fun unwrapConsent(url: String): String {
        if (!url.contains("consent.google.")) return url
        val cont = Regex("""[?&]continue=([^&]+)""").find(url)?.groupValues?.get(1) ?: return url
        return URLDecoder.decode(cont, "UTF-8")
    }
}
