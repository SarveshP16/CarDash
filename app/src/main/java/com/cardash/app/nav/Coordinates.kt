package com.cardash.app.nav

/**
 * Recognises a pasted location so it can be routed to directly, without a place search:
 * "-33.8688, 151.2093", "-33.8688 151.2093", or a Google Maps link containing "@lat,lng",
 * "q=lat,lng" or "ll=lat,lng".
 */
object Coordinates {
    data class LatLon(val lat: Double, val lon: Double) {
        /** Short form for labels and app hand-offs. */
        override fun toString() = "%.6f,%.6f".format(java.util.Locale.ROOT, lat, lon)
    }

    // Decimals required, so "12 34" or a postcode pair isn't mistaken for coordinates.
    private const val NUM = """[-+]?\d{1,3}\.\d+"""
    private val plain = Regex("""^\s*($NUM)\s*[,\s]\s*($NUM)\s*$""")
    private val inLink = Regex("""(?:@|[?&](?:q|ll|query|destination)=)($NUM)(?:,|%2C)\s*($NUM)""", RegexOption.IGNORE_CASE)

    fun parse(text: String): LatLon? {
        val match = plain.find(text) ?: inLink.find(text) ?: return null
        val lat = match.groupValues[1].toDoubleOrNull() ?: return null
        val lon = match.groupValues[2].toDoubleOrNull() ?: return null
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
        return LatLon(lat, lon)
    }
}
