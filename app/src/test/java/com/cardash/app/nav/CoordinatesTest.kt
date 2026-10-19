package com.cardash.app.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoordinatesTest {
    private val opera = Coordinates.LatLon(-33.8568, 151.2153)

    @Test fun `comma separated`() = assertEquals(opera, Coordinates.parse("-33.8568, 151.2153"))

    @Test fun `space separated`() = assertEquals(opera, Coordinates.parse("  -33.8568 151.2153 "))

    @Test fun `google maps place link`() =
        assertEquals(opera, Coordinates.parse("https://www.google.com/maps/place/Sydney+Opera+House/@-33.8568,151.2153,17z"))

    @Test fun `query link`() = assertEquals(opera, Coordinates.parse("https://maps.google.com/?q=-33.8568,151.2153"))

    @Test fun `addresses are not coordinates`() {
        assertNull(Coordinates.parse("10 George St Sydney"))
        assertNull(Coordinates.parse("Bunnings"))
        assertNull(Coordinates.parse("2000 2150"))
    }

    @Test fun `out of range is rejected`() = assertNull(Coordinates.parse("95.0, 151.2"))

    @Test fun `hand-off form`() = assertEquals("-33.856800,151.215300", opera.toString())
}
