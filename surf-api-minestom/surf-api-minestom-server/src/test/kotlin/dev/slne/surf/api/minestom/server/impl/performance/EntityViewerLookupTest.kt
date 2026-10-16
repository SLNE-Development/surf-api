package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.coordinate.CoordConversion
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.EntityTracker
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests that [EntityViewerLookup.directScan] visits exactly the entities that
 * [EntityTracker.nearbyEntitiesByChunkRange] visits. Unit tests run without mixins, so the tracker
 * used here is the unmodified bucket scan.
 */
@EnvTest
class EntityViewerLookupTest {

    @Test
    fun `every range matches the bucket scan on and around the boundary`(env: Env) {
        for (range in listOf(0, 1, 8, 32)) {
            for ((centerX, centerZ) in listOf(0 to 0, 37 to -12)) {
                val fixture = TrackerFixture()

                fixture.spawn(centerX, centerZ)
                // On each edge and corner of the range square.
                for (dx in intArrayOf(-range, 0, range)) {
                    for (dz in intArrayOf(-range, 0, range)) {
                        fixture.spawn(centerX + dx, centerZ + dz)
                    }
                }
                // One chunk outside each edge and corner.
                val outside = range + 1
                for (dx in intArrayOf(-outside, 0, outside)) {
                    for (dz in intArrayOf(-outside, 0, outside)) {
                        if (dx != 0 || dz != 0) fixture.spawn(centerX + dx, centerZ + dz)
                    }
                }

                fixture.assertEquivalent(centerX, centerZ, range)

                val onCorner = fixture.spawn(centerX + range, centerZ + range)
                val outsideCorner = fixture.spawn(centerX + range + 1, centerZ + range + 1)
                fixture.assertEquivalent(centerX, centerZ, range)
                val nearby = fixture.directNearby(centerX, centerZ, range)
                assertTrue(onCorner.entityId in nearby)
                assertFalse(outsideCorner.entityId in nearby)
            }
        }
    }

    @Test
    fun `negative chunk coordinates match the bucket scan`(env: Env) {
        val fixture = TrackerFixture()
        for (range in listOf(0, 1, 8, 32)) {
            val centerX = -40
            val centerZ = -33
            fixture.spawn(centerX, centerZ)
            fixture.spawn(centerX - range, centerZ - range)
            fixture.spawn(centerX + range, centerZ - range)
            fixture.spawn(centerX - range - 1, centerZ)
            fixture.spawn(centerX, centerZ + range + 1)
            fixture.assertEquivalent(centerX, centerZ, range)
        }
    }

    @Test
    fun `many entities in one chunk match the bucket scan`(env: Env) {
        val fixture = TrackerFixture()
        repeat(64) { fixture.spawn(3, -2) }
        fixture.spawn(4, -2)
        for (range in listOf(0, 1, 8)) {
            fixture.assertEquivalent(3, -2, range)
            fixture.assertEquivalent(2, -2, range)
        }
    }

    @Test
    fun `entities distributed over thousands of chunks match the bucket scan`(env: Env) {
        val fixture = TrackerFixture()
        for (chunkX in -40..40 step 2) {
            for (chunkZ in -40..40 step 2) {
                fixture.spawn(chunkX, chunkZ)
            }
        }
        fixture.assertEquivalent(0, 0, 32)
        fixture.assertEquivalent(7, -5, 32)
        fixture.assertEquivalent(-39, 39, 8)
        fixture.assertEquivalent(60, 60, 32)
    }

    @Test
    fun `registration and removal stay in sync with the bucket scan`(env: Env) {
        val fixture = TrackerFixture()
        val entities = (0 until 20).map { fixture.spawn(it, it) }
        fixture.assertEquivalent(5, 5, 8)

        entities.filterIndexed { index, _ -> index % 2 == 0 }.forEach(fixture::remove)
        fixture.assertEquivalent(5, 5, 8)

        fixture.spawn(5, 5)
        fixture.assertEquivalent(5, 5, 8)
        fixture.assertEquivalent(5, 5, 0)
    }

    @Test
    fun `movement across chunk boundaries stays in sync with the bucket scan`(env: Env) {
        val fixture = TrackerFixture()
        val walker = fixture.spawn(8, 0)
        fixture.spawn(0, 0)

        fixture.assertEquivalent(0, 0, 8)
        assertTrue(walker.entityId in fixture.directNearby(0, 0, 8))

        fixture.move(walker, 9, 0)
        fixture.assertEquivalent(0, 0, 8)
        assertFalse(walker.entityId in fixture.directNearby(0, 0, 8))

        fixture.move(walker, -8, 0)
        fixture.assertEquivalent(0, 0, 8)
        assertTrue(walker.entityId in fixture.directNearby(0, 0, 8))
    }

    /**
     * A vanilla tracker plus the registered and moved positions that the resolver replays.
     */
    private class TrackerFixture {
        private val tracker = EntityTracker.newTracker()
        private val recorded = HashMap<Int, Point>()
        private val resolver =
            EntityViewerLookup.EntityPositionResolver { entity -> recorded[entity.entityId] }

        fun spawn(chunkX: Int, chunkZ: Int): Entity {
            val entity = Entity(EntityType.ZOMBIE)
            val point = chunkCenter(chunkX, chunkZ)
            tracker.register(entity, point, EntityTracker.Target.ENTITIES, null)
            recorded[entity.entityId] = point
            return entity
        }

        fun move(entity: Entity, chunkX: Int, chunkZ: Int) {
            val point = chunkCenter(chunkX, chunkZ)
            tracker.move(entity, point, EntityTracker.Target.ENTITIES, null)
            recorded[entity.entityId] = point
        }

        fun remove(entity: Entity) {
            tracker.unregister(entity, EntityTracker.Target.ENTITIES, null)
            recorded.remove(entity.entityId)
        }

        fun vanillaNearby(centerChunkX: Int, centerChunkZ: Int, range: Int): Set<Int> {
            val ids = LinkedHashSet<Int>()
            tracker.nearbyEntitiesByChunkRange(
                chunkCenter(centerChunkX, centerChunkZ),
                range,
                EntityTracker.Target.ENTITIES,
            ) { entity -> assertTrue(ids.add(entity.entityId), "bucket scan visited an entity twice") }
            return ids
        }

        fun directNearby(centerChunkX: Int, centerChunkZ: Int, range: Int): Set<Int> {
            val ids = LinkedHashSet<Int>()
            EntityViewerLookup.directScan(
                tracker.entities(EntityTracker.Target.ENTITIES),
                resolver,
                centerChunkX,
                centerChunkZ,
                range,
            ) { entity -> assertTrue(ids.add(entity.entityId), "direct scan visited an entity twice") }
            return ids
        }

        fun assertEquivalent(centerChunkX: Int, centerChunkZ: Int, range: Int) {
            assertEquals(
                vanillaNearby(centerChunkX, centerChunkZ, range),
                directNearby(centerChunkX, centerChunkZ, range),
                "direct scan diverged from the bucket scan at ($centerChunkX, $centerChunkZ) range $range",
            )
        }

        private fun chunkCenter(chunkX: Int, chunkZ: Int): Point =
            Pos(chunkX * 16.0 + 8.0, 42.0, chunkZ * 16.0 + 8.0).also { point ->
                check(point.chunkX() == chunkX && point.chunkZ() == chunkZ)
                check(CoordConversion.chunkIndex(point) == CoordConversion.chunkIndex(chunkX, chunkZ))
            }
    }
}
