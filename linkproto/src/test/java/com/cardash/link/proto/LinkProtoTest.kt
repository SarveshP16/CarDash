package com.cardash.link.proto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkProtoTest {
    private fun roundTrip(msg: Msg) = assertEquals(msg, Msg.decode(Msg.encode(msg)))

    @Test fun `every message round-trips`() {
        val place = LinkPlace("Westfield", "Westfield, Church St", "ChIJabc", 1200)
        listOf(
            Msg.Challenge("abcd", "Pixel"), Msg.Auth("mac", "CarDash"), Msg.Pair("012345", "CarDash"),
            Msg.Paired("tok"), Msg.Ok, Msg.Denied("nope"), Msg.Ping, Msg.Pong,
            Msg.NowPlaying("Song", "Artist", "YouTube Music", true, "k1", "AAAA"),
            Msg.NowPlaying("Song", "Artist", "YouTube Music", false, "k1", null),
            Msg.NothingPlaying, Msg.Navigate(place), Msg.Navigate(LinkPlace("-33.8,151.0", "-33.8,151.0")),
            Msg.Suggest(3, "bunn"), Msg.SaveFavourite(place), Msg.RemoveFavourite(place), Msg.SetHome(place), Msg.EndRoute,
            Msg.Command(Msg.Action.RESUME), Msg.Suggestions(3, listOf(place)),
            Msg.Places(null, listOf(place), emptyList()), Msg.Places(place, emptyList(), listOf(place)),
            Msg.Route("Westfield"), Msg.Route(null),
            Msg.ListClips, Msg.ClipAction(42, Msg.ClipOp.DELETE),
            Msg.Clips(47820, "k", listOf(ClipInfo(42, "back", 1_760_000_000_000, 180_000, 225_000_000, true)), 1, 8),
            Msg.Clips(47820, "k", emptyList(), 0, 0),
        ).forEach(::roundTrip)
    }

    @Test fun `unknown or broken lines are ignored`() {
        assertNull(Msg.decode("""{"t":"future-thing"}"""))
        assertNull(Msg.decode("not json"))
        assertNull(Msg.decode("""{"t":"cmd","action":"EJECT"}"""))
    }

    @Test fun `mac depends on token and nonce`() {
        val a = LinkProtocol.mac("token", "nonce")
        assertTrue(LinkProtocol.sameMac(a, LinkProtocol.mac("token", "nonce")))
        assertFalse(LinkProtocol.sameMac(a, LinkProtocol.mac("other", "nonce")))
        assertFalse(LinkProtocol.sameMac(a, LinkProtocol.mac("token", "nonce2")))
        assertEquals(6, LinkProtocol.pairCode().length)
    }

    @Test fun `shared maps text splits into name, details and link`() {
        val s = SharedPlace.parseText("Bunnings Warehouse Parramatta\nCnr Church St, Parramatta NSW\nhttps://maps.app.goo.gl/AbC123")
        assertEquals("Bunnings Warehouse Parramatta", s.name)
        assertEquals("Cnr Church St, Parramatta NSW", s.details)
        assertEquals("https://maps.app.goo.gl/AbC123", s.url)
        assertTrue(SharedPlace.isShortLink(s.url!!))
        assertEquals(SharedPlace.Shared("Gym", null, "https://maps.app.goo.gl/x"), SharedPlace.parseText("https://maps.app.goo.gl/x", "Gym"))
    }

    @Test fun `full maps url gives the place position, not the map centre`() {
        val url = "https://www.google.com/maps/place/Bunnings+Warehouse+Parramatta/@-33.8100,151.0000,17z/data=!3m1!4b1!4m6!3m5!1s0x0:0x1!8m2!3d-33.8123456!4d151.0012345!16s"
        assertEquals(LinkPlace("Bunnings Warehouse Parramatta", "-33.8123456,151.0012345"), SharedPlace.fromUrl(url))
        assertEquals(
            LinkPlace("Home", "-33.81,151.02"),
            SharedPlace.fromUrl("https://maps.google.com/maps?q=-33.81,151.02", "Home"),
        )
        assertEquals(LinkPlace("Sydney Opera House", "Sydney Opera House"), SharedPlace.fromUrl("https://maps.google.com/?q=Sydney+Opera+House"))
        assertNull(SharedPlace.fromUrl("https://example.com/"))
    }

    @Test fun `name-only link uses the shared address text`() {
        val shared = SharedPlace.parseText("Joe's Cafe\n12 High St, Penrith\nhttps://maps.app.goo.gl/q")
        val place = SharedPlace.place(shared, "https://www.google.com/maps/place/Joe's+Cafe/data=!4m2")
        assertEquals(LinkPlace("Joe's Cafe", "Joe's Cafe, 12 High St, Penrith"), place)
        // Plain text, no link at all.
        assertEquals(LinkPlace("Penrith station", "Penrith station"), SharedPlace.place(SharedPlace.parseText("Penrith station"), null))
    }

    @Test fun `byte ranges for video seeking`() {
        assertNull(HttpRange.parse(null, 1000))
        assertEquals(0L..999L, HttpRange.parse("bytes=0-", 1000))
        assertEquals(500L..999L, HttpRange.parse("bytes=500-", 1000))
        assertEquals(10L..19L, HttpRange.parse("bytes=10-19", 1000))
        assertEquals(990L..999L, HttpRange.parse("bytes=990-5000", 1000))
        assertEquals(900L..999L, HttpRange.parse("bytes=-100", 1000))
        assertEquals(LongRange.EMPTY, HttpRange.parse("bytes=1000-", 1000))
        assertNull(HttpRange.parse("bytes=0-1,5-6", 1000))
        assertNull(HttpRange.parse("items=0-1", 1000))
    }
}
