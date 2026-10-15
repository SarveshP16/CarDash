package com.cardash.app.nav

/** A one-tap navigation button: a label and the address/place query handed to Maps or Waze. */
data class Destination(val label: String, val address: String) {
    val isSet: Boolean get() = address.isNotBlank()

    companion object {
        // One destination per line as "label<TAB>address"; tabs and newlines are stripped on input.
        fun encodeList(list: List<Destination>): String =
            list.joinToString("\n") { "${clean(it.label)}\t${clean(it.address)}" }

        fun decodeList(raw: String): List<Destination> =
            raw.lines().filter { it.isNotBlank() }.map { line ->
                val parts = line.split('\t', limit = 2)
                Destination(parts[0], parts.getOrElse(1) { "" })
            }

        private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').trim()
    }
}
