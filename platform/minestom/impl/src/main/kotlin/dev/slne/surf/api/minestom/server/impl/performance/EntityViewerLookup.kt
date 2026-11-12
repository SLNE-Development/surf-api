package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.coordinate.ChunkRange
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import java.util.function.Consumer

/**
 * Entity lookup helpers used by [dev.slne.surf.api.minestom.server.mixin.EntityTrackerMixin].
 */
object EntityViewerLookup {

    /**
     * Cost of one tracked-entity check relative to one chunk-bucket probe. Benchmarks put the
     * crossover near 1400 candidates against 4225 probes, so a factor of 3 restricts the direct
     * scan to cases where it wins.
     */
    private const val DIRECT_SCAN_COST_FACTOR = 3L

    /**
     * Resolves the tracker-recorded position of an entity, or `null` if it is not tracked.
     */
    fun interface EntityPositionResolver {
        fun resolve(entity: Entity): Point?
    }

    /**
     * Whether scanning all [candidateCount] tracked candidates is cheaper than probing every chunk
     * bucket in the square of [chunkRange].
     */
    @JvmStatic
    fun useDirectScan(candidateCount: Int, chunkRange: Int): Boolean {
        if (chunkRange <= 0) return false
        val side = 2L * chunkRange + 1L
        return candidateCount * DIRECT_SCAN_COST_FACTOR < side * side
    }

    /**
     * Passes every candidate whose resolved position lies within [chunkRange] chunks of
     * `(centerChunkX, centerChunkZ)` to [query]. The bounds are inclusive, as in
     * [ChunkRange.chunksInRange]. Candidates the [resolver] cannot place are skipped.
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
     * Visits the same chunks as [ChunkRange.chunksInRange] in row-major instead of spiral order.
     * Only for lookups that do not depend on visiting order.
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
