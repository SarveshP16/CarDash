package com.cardash.app.nav

/**
 * A saved place: Home or a favourite. [address] is what's routed to (text, or coordinates);
 * [placeId] is set when it was saved from a search suggestion, for an exact in-app route.
 */
data class Destination(val label: String, val address: String, val placeId: String? = null) {
    val isSet: Boolean get() = address.isNotBlank()

    /** As a search suggestion, so RouteStarter can route by place ID. */
    fun asSuggestion(): PlacesSearch.Suggestion? =
        // Saved addresses already start with the name; don't repeat it in "name, address".
        placeId?.let { PlacesSearch.Suggestion(it, label, address.removePrefix("$label, "), null) }

    companion object {
        // One destination per line as "label<TAB>address[<TAB>placeId]"; tabs and newlines are stripped on input.
        fun encodeList(list: List<Destination>): String =
            list.joinToString("\n") { d ->
                listOfNotNull(clean(d.label), clean(d.address), d.placeId?.let(::clean)).joinToString("\t")
            }

        fun decodeList(raw: String): List<Destination> =
            raw.lines().filter { it.isNotBlank() }.map { line ->
                val parts = line.split('\t', limit = 3)
                Destination(parts[0], parts.getOrElse(1) { "" }, parts.getOrNull(2)?.ifBlank { null })
            }

        private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').trim()
    }
}
