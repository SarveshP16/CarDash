package com.cardash.app.settings

import com.cardash.app.nav.Destination

/** Add/remove saved places; the same place (by ID, else by address) is never saved twice. */
object Favourites {
    fun add(place: Destination): Boolean {
        val existing = SettingsStore.current.favourites
        val duplicate = existing.any {
            (place.placeId != null && it.placeId == place.placeId) || it.address.equals(place.address, ignoreCase = true)
        }
        if (duplicate || !place.isSet) return false
        SettingsStore.update { it.copy(favourites = it.favourites + place) }
        return true
    }

    fun remove(place: Destination) {
        SettingsStore.update { s -> s.copy(favourites = s.favourites.filterNot { it == place }) }
    }
}

/** The last few places routed to, newest first; the same place is only listed once. */
object Recents {
    private const val MAX = 8

    fun add(place: Destination) {
        if (!place.isSet) return
        SettingsStore.update { s ->
            val others = s.recents.filterNot {
                (place.placeId != null && it.placeId == place.placeId) || it.address.equals(place.address, ignoreCase = true)
            }
            s.copy(recents = (listOf(place) + others).take(MAX))
        }
    }
}
