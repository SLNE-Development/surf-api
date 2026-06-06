package dev.slne.surf.api.paper.location

import com.destroystokyo.paper.MaterialSetTag
import com.github.shynixn.mccoroutine.folia.globalRegionDispatcher
import dev.slne.surf.api.core.util.random
import dev.slne.surf.api.paper.util.doInChunkAsync
import dev.slne.surf.api.paper.util.namespacedKey
import kotlinx.coroutines.withContext
import org.bukkit.*
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.annotations.Range
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

object SafeLocationFinder {

    private val UNSAFE_BLOCKS = MaterialSetTag(namespacedKey("unsafe_blocks"))
        .add(Material.WATER, Material.LAVA)
        .add(MaterialSetTag.SIGNS, MaterialSetTag.WALL_SIGNS)
        .add(Material.CHEST, Material.ENDER_CHEST, Material.TRAPPED_CHEST).add(Tag.COPPER_CHESTS)
        .add(Tag.BEDS)
        .add(Tag.LEAVES)
        .add(Material.BEDROCK)
        .add(Material.POWDER_SNOW)
        .add(Tag.AIR)
        .add(Material.POINTED_DRIPSTONE)
        .add(Material.CACTUS)
        .add(Material.MAGMA_BLOCK)
        .add(Tag.CAMPFIRES)
        .add(Tag.FIRE)
        .lock()

    /**
     * Blocks that are safe to be teleported into
     */
    private val SAFE_OCCUPATION_BLOCKS = MaterialSetTag(namespacedKey("safe_occupation_blocks"))
        .add(Material.SHORT_GRASS, Material.TALL_GRASS)
        .add(Material.FERN, Material.LARGE_FERN)
        .add(Material.DEAD_BUSH)
        .add(Material.BROWN_MUSHROOM, Material.RED_MUSHROOM)
        .add(Tag.FLOWERS)
        .lock()

    const val SEARCH_RADIUS = 2

    private val plugin get() = JavaPlugin.getProvidingPlugin(javaClass)

    suspend fun findSafeRandomLocation(world: World, maxAttempts: Int = 10): Location? {
        val (center, radius) = withContext(plugin.globalRegionDispatcher) {
            world.worldBorder.center to world.worldBorder.size / 2
        }

        return findSafeRandomLocation(center, radius.toInt(), maxAttempts)
    }

    suspend fun findSafeRandomLocation(around: Location, radius: Int, maxAttempts: Int = 10): Location? {
        require(radius > 0) { "Radius must be greater than 0" }
        require(maxAttempts > 0) { "Max attempts must be greater than 0" }

        val around = around.clone()

        repeat(maxAttempts) {
            val randomLocation = around.clone().add(
                random.nextInt(-radius, radius + 1).toDouble(),
                0.0,
                random.nextInt(-radius, radius + 1).toDouble()
            )

            val safeLocation = findSafeGroundLocation(randomLocation)
            if (safeLocation != null) {
                return safeLocation
            }
        }

        return null
    }

    suspend fun findSafeGroundLocation(location: Location): Location? {
        val location = location.clone()
        val world = location.world ?: return null

        val insideWorldBorder = withContext(plugin.globalRegionDispatcher) {
            world.worldBorder.isInside(location)
        }

        if (!insideWorldBorder) return null

        val snapshot = world.doInChunkAsync(location, Chunk::getChunkSnapshot)
        return findSafeLocationNear(location, snapshot, world.minHeight, world.maxHeight)
    }

    fun findSafeLocationNear(location: Location, chunk: ChunkSnapshot, minY: Int, maxY: Int): Location? {
        val location = location.clone()
        val world = location.world ?: return null
        val chunkX = location.blockX and 0xF
        val chunkZ = location.blockZ and 0xF

        fun blockCenter(block: Int): Double = block + 0.5

        for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (dz in -SEARCH_RADIUS..SEARCH_RADIUS) {
                val x = chunkX + dx
                val z = chunkZ + dz

                if (x !in 0 until 16 || z !in 0 until 16) continue

                val y = getY(world, chunk, minY, maxY, x, z) ?: continue

                return Location(
                    world,
                    blockCenter(floor(location.x).toInt() + dx),
                    y.toDouble(),
                    blockCenter(floor(location.z).toInt() + dz)
                )
            }
        }

        return null
    }

    private fun getY(world: World, chunk: ChunkSnapshot, minY: Int, maxY: Int, x: Int, z: Int): Int? {
        val highestY = min(chunk.getHighestBlockYAt(x, z), maxY)

        if (world.environment == World.Environment.NETHER) {
            val coordinates = (minY + 1 until highestY)
            return coordinates.find { y -> isSafeLocation(chunk, x, y, z) }
        } else {
            val y = max(minY + 1, highestY) + 1
            return y.takeIf { isSafeLocation(chunk, x, y, z) }
        }
    }

    fun isSafeLocation(
        chunk: ChunkSnapshot,
        x: @Range(from = 0, to = 15) Int,
        y: Int,
        z: @Range(from = 0, to = 15) Int
    ): Boolean {
        val blockType = chunk.getBlockType(x, y - 1, z)
        val bodyBlockType = chunk.getBlockType(x, y, z)
        val headBlockType = chunk.getBlockType(x, y + 1, z)

        return isBlockSafeForStanding(blockType)
                && isBlockSafeForOccupation(bodyBlockType)
                && isBlockSafeForOccupation(headBlockType)
    }

    fun isBlockSafeForStanding(blockType: Material): Boolean {
        return !UNSAFE_BLOCKS.isTagged(blockType)
    }

    fun isBlockSafeForOccupation(blockType: Material): Boolean {
        return SAFE_OCCUPATION_BLOCKS.isTagged(blockType)
    }
}