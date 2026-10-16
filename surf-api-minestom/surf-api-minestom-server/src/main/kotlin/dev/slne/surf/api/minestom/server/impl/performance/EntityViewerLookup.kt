package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.coordinate.ChunkRange
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import java.util.function.Consumer

/** Entity lookup helpers used by [dev.slne.surf.api.minestom.server.mixin.EntityTrackerMixin]. */
object EntityViewerLookup {

    /**
     * Relative cost of one tracked-entity check (set-iteration step, id-to-entry map lookup,
     * position read, two coordinate comparisons) against one chunk-bucket lookup (chunk-index
     * computation, hash-map probe). ViewerLookupBenchmark measured the crossover near 1400
     * candidates against 4225 bucket probes, so 3 keeps the direct scan to cases where it wins.
     */
    private const val DIRECT_SCAN_COST_FACTOR = 3L

    /** Resolves the tracker-recorded position of an entity, or null if it is not tracked. */
    fun interface EntityPositionResolver {
        fun resolve(entity: Entity): Point?
    }

    /**
     * Decides whether scanning all tracked candidates beats probing every chunk bucket in a
     * square range of [chunkRange].
     */
    @JvmStatic
    fun useDirectScan(candidateCount: Int, chunkRange: Int): Boolean {
        if (chunkRange <= 0) return false
        val side = 2L * chunkRange + 1L
        return candidateCount * DIRECT_SCAN_COST_FACTOR < side * side
    }

    /**
     * Visits every candidate whose resolved position lies within the square chunk range around
     * `(centerChunkX, centerChunkZ)`, using the same inclusive bounds as
     * [ChunkRange.chunksInRange].
     */
    @Suppress("ConvertTwoComparisonsToRangeCheck")
    @JvmStatic
    fun <T : Entity> directScan(
        candidates: Set<T>,
        resolver: EntityPositionResolver,
        centerChunkX: Int,
        centerChunkZ: Int,
        chunkRange: Int,
        query: Consumer<T>,
    ) {
        val minX = centerChunkX - chunkRange
        val maxX = centerChunkX + chunkRange
        val minZ = centerChunkZ - chunkRange
        val maxZ = centerChunkZ + chunkRange

        for (candidate in candidates) {
            val position = resolver.resolve(candidate) ?: continue

            val chunkX = position.chunkX()
            if (chunkX < minX || chunkX > maxX) continue

            val chunkZ = position.chunkZ()
            if (chunkZ < minZ || chunkZ > maxZ) continue

            query.accept(candidate)
        }
    }

    /**
     * Visits the same chunk set as [ChunkRange.chunksInRange] in plain row-major order, without
     * the spiral's sqrt/div/mod arithmetic. Only for order-independent lookups.
     */
    @JvmStatic
    fun chunksInRangeRectangular(
        chunkX: Int,
        chunkZ: Int,
        chunkRange: Int,
        consumer: ChunkRange.ChunkConsumer,
    ) {
        val minX = chunkX - chunkRange
        val maxX = chunkX + chunkRange
        val minZ = chunkZ - chunkRange
        val maxZ = chunkZ + chunkRange
        for (x in minX..maxX) {
            for (z in minZ..maxZ) {
                consumer.accept(x, z)
            }
        }
    }
}
