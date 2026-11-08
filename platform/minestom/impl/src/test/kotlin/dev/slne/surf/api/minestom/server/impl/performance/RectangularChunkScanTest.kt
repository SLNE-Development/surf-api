package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.coordinate.ChunkRange
import net.minestom.server.coordinate.CoordConversion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class RectangularChunkScanTest {

    @Test
    fun `rectangular scan visits exactly the spiral chunk set, each chunk once`() {
        for (range in listOf(0, 1, 8, 32)) {
            for ((centerX, centerZ) in listOf(0 to 0, -5 to 3, -1000 to -1000, 123 to -456)) {
                val spiral = HashSet<Long>()
                ChunkRange.chunksInRange(centerX, centerZ, range) { x, z ->
                    spiral.add(CoordConversion.chunkIndex(x, z))
                }

                val rectangular = HashSet<Long>()
                var visits = 0
                EntityViewerLookup.chunksInRangeRectangular(centerX, centerZ, range) { x, z ->
                    visits++
                    assertTrue(
                        rectangular.add(CoordConversion.chunkIndex(x, z)),
                        "chunk ($x, $z) visited twice",
                    )
                }

                assertEquals(ChunkRange.chunksCount(range), visits)
                assertEquals(spiral, rectangular, "range $range around ($centerX, $centerZ)")
            }
        }
    }

    @Test
    fun `direct scan is never chosen for single-chunk lookups`() {
        assertFalse(EntityViewerLookup.useDirectScan(0, 0))
        assertFalse(EntityViewerLookup.useDirectScan(1, 0))
        assertFalse(EntityViewerLookup.useDirectScan(10_000, 0))
    }

    @Test
    fun `direct scan is chosen only while candidate checks are cheaper than bucket probes`() {
        // Range 32 is 4225 buckets; with factor 3 the break-even is 1409 candidates.
        assertTrue(EntityViewerLookup.useDirectScan(1, 32))
        assertTrue(EntityViewerLookup.useDirectScan(1000, 32))
        assertTrue(EntityViewerLookup.useDirectScan(1408, 32))
        assertFalse(EntityViewerLookup.useDirectScan(1409, 32))
        assertFalse(EntityViewerLookup.useDirectScan(5000, 32))

        // Range 1 is 9 buckets.
        assertTrue(EntityViewerLookup.useDirectScan(2, 1))
        assertFalse(EntityViewerLookup.useDirectScan(3, 1))

        // Large values must not overflow.
        assertFalse(EntityViewerLookup.useDirectScan(Int.MAX_VALUE, 1))
        assertTrue(EntityViewerLookup.useDirectScan(Int.MAX_VALUE, 100_000))
    }
}
