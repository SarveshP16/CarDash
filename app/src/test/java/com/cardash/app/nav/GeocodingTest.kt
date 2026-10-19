package com.cardash.app.nav

import com.cardash.app.location.Fix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeocodingTest {
    private val stores = listOf(
        Triple("Bunnings Brisbane", -27.47, 153.02),
        Triple("Bunnings Ashfield", -33.89, 151.12),
        Triple("Bunnings Alexandria", -33.91, 151.19),
    )

    @Test fun `picks the result nearest the car`() {
        val nearAshfield = Fix(lat = -33.88, lon = 151.10, speedKmh = null)
        assertEquals("Bunnings Ashfield", Geocoding.nearest(stores, nearAshfield))
    }

    @Test fun `without a fix takes the first result`() {
        assertEquals("Bunnings Brisbane", Geocoding.nearest(stores, null))
    }

    @Test fun `no results gives null`() {
        assertNull(Geocoding.nearest(emptyList<Triple<String, Double, Double>>(), null))
    }
}
