package com.cardash.app.camera

import com.cardash.app.camera.DashcamStorage.Clip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashcamTest {
    private fun clip(id: Long, mb: Long, date: Long, locked: Boolean = false) = Clip(id, mb * 1_000_000, date, locked)

    @Test fun `nothing deleted under the limit`() {
        val clips = listOf(clip(1, 300, 1), clip(2, 300, 2))
        assertEquals(emptyList<Clip>(), DashcamStorage.toDelete(clips, 1_000_000_000))
    }

    @Test fun `oldest unlocked clips go first until under the limit`() {
        val clips = listOf(clip(3, 400, 3), clip(1, 400, 1), clip(2, 400, 2))
        assertEquals(listOf(1L), DashcamStorage.toDelete(clips, 1_000_000_000).map { it.id })
    }

    @Test fun `locked clips are never deleted and don't count`() {
        val clips = listOf(clip(1, 900, 1, locked = true), clip(2, 400, 2), clip(3, 400, 3))
        assertEquals(emptyList<Long>(), DashcamStorage.toDelete(clips, 1_000_000_000).map { it.id })
    }

    @Test fun `impact threshold`() {
        assertFalse(ImpactDetector.isImpact(1.0f)) // resting
        assertFalse(ImpactDetector.isImpact(1.8f)) // hard braking / pothole
        assertTrue(ImpactDetector.isImpact(2.5f))
    }
}
