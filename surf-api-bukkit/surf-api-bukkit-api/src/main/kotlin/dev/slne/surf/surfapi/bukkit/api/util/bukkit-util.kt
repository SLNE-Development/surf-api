@file:JvmName("UtilBukkit")

package dev.slne.surf.surfapi.bukkit.api.util

import com.github.shynixn.mccoroutine.folia.SuspendingPlugin
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi
import dev.slne.surf.surfapi.core.api.util.getCallerClass
import dev.slne.surf.surfapi.core.api.util.mutableLong2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.Position
import it.unimi.dsi.fastutil.objects.ObjectList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.spongepowered.math.vector.Vector3d
import org.spongepowered.math.vector.Vector3i
import java.util.*

/**
 * Creates a [NamespacedKey] using the calling plugin and the given name.
 *
 * @param name The key name.
 * @return The created [NamespacedKey].
 * @throws IllegalStateException If the calling plugin cannot be determined.
 */
fun key(name: String): NamespacedKey { // TODO: Verify if this works
    return NamespacedKey(getCallingPlugin(), name)
}

/**
 * Retrieves the [JavaPlugin] that called this method.
 *
 * @param depth The stack trace depth to determine the caller class. Default is 1.
 * @return The calling [JavaPlugin].
 * @throws IllegalStateException If the caller class cannot be determined.
 */
fun getCallingPlugin(depth: Int = 1): JavaPlugin {
    val caller = getCallerClass(depth) ?: error("Cannot determine caller class")
    return JavaPlugin.getProvidingPlugin(caller)
}

/**
 * Iterates over all online players and performs the given action.
 *
 * @param action The action to perform on each player.
 */
fun forEachPlayer(action: (player: Player) -> Unit) {
    Bukkit.getOnlinePlayers().forEach(action)
}

@Deprecated(
    message = "Use the overload with an explicit plugin parameter; this version relies on inefficient stacktrace inspection.",
    replaceWith = ReplaceWith("forEachPlayerInRegion(plugin, action, concurrent)")
)
suspend fun forEachPlayerInRegion(
    action: suspend (player: Player) -> Unit,
    concurrent: Boolean = false,
) = forEachPlayerInRegion(
    plugin = getCallingSuspendingPlugin(),
    action = action,
    concurrent = concurrent,
)

/**
 * Executes a suspendable action on each online player, optionally concurrently.
 *
 * @param action The suspendable action to perform on each player.
 * @param concurrent If `true`, actions will run concurrently; otherwise, sequentially. Default is `false`.
 */
suspend fun forEachPlayerInRegion(
    plugin: SuspendingPlugin,
    action: suspend (player: Player) -> Unit,
    concurrent: Boolean = false,
) {
    if (concurrent) {
        coroutineScope {
            Bukkit.getOnlinePlayers()
                .map {
                    async {
                        withContext(plugin.entityDispatcher(it)) {
                            action(it)
                        }
                    }
                }.awaitAll()
        }
    } else {
        for (player in Bukkit.getOnlinePlayers()) {
            withContext(plugin.entityDispatcher(player)) {
                action(player)
            }
        }
    }
}

/**
 * Computes the squared distance between this location and another.
 *
 * @receiver The starting location.
 * @param other The target location.
 * @return The squared distance between the two locations.
 */
infix fun Location.distanceSqt(other: Location): Double = distanceSquared(other)

/**
 * Gets the chunk X coordinate of this location.
 */
val Location.chunkX get() = blockX shr 4

/**
 * Gets the chunk X coordinate of this position.
 */
val Position.chunkX get() = blockX() shr 4

/**
 * Gets the chunk Z coordinate of this position.
 */
val Position.chunkZ get() = blockZ() shr 4

/**
 * Gets the chunk Z coordinate of this location.
 */
val Location.chunkZ get() = blockZ shr 4

/**
 * Gets the chunk key of this location.
 */
val Location.chunkKey get() = Chunk.getChunkKey(this)

fun Location.toVector3d(): Vector3d {
    return Vector3d(
        this.x,
        this.y,
        this.z
    )
}

/**
 * Converts an iterable of UUIDs to a list of online [Player] instances.
 *
 * @receiver The collection of UUIDs.
 * @return A list of [Player] instances corresponding to the UUIDs, excluding offline players.
 */
fun Iterable<UUID>.toPlayers() = mapNotNull { Bukkit.getPlayer(it) }

fun Iterable<UUID>.toOfflinePlayers() = mapNotNull { Bukkit.getOfflinePlayer(it) }

/**
 * Converts a sequence of UUIDs to a sequence of online [Player] instances.
 *
 * @receiver The sequence of UUIDs.
 * @return A list of [Player] instances corresponding to the UUIDs, excluding offline players.
 */
fun Sequence<UUID>.toPlayers() = mapNotNull { Bukkit.getPlayer(it) }

fun Sequence<UUID>.toOfflinePlayers() = mapNotNull { Bukkit.getOfflinePlayer(it) }

/**
 * Checks if the player can see the specified location.
 *
 * @receiver The player.
 * @param location The location to check.
 * @return `true` if the player can see the location, `false` otherwise.
 */
fun Player.isChunkVisible(location: Location): Boolean {
    return this.world == location.world && this.isChunkSent(Chunk.getChunkKey(location))
}

fun Player.isChunkVisible(world: World, chunkX: Int, chunkZ: Int): Boolean {
    if (this.world != world) return false
    return this.isChunkSent(Chunk.getChunkKey(chunkX, chunkZ))
}

/**
 * Retrieves the coroutine dispatcher for this entity.
 *
 * @receiver The entity.
 * @param plugin The suspending plugin instance. Defaults to the calling suspending plugin.
 * @return The entity's coroutine dispatcher.
 */
@Deprecated("Use 'plugin.entityDispatcher(this)' directly instead of relying on this helper, as it uses inefficient stacktrace inspection.")
fun Entity.dispatcher(
    plugin: SuspendingPlugin = getCallingSuspendingPlugin(),
) = plugin.entityDispatcher(this)

/**
 * Retrieves the coroutine dispatcher for this location.
 *
 * @receiver The location.
 * @param plugin The suspending plugin instance. Defaults to the calling suspending plugin.
 * @return The region's coroutine dispatcher.
 */
@Deprecated("Use 'plugin.regionDispatcher(this)' directly instead of relying on this helper, as it uses inefficient stacktrace inspection.")
fun Location.dispatcher(
    plugin: SuspendingPlugin = getCallingSuspendingPlugin(),
) = plugin.regionDispatcher(this)

/**
 * Retrieves the calling suspending plugin.
 *
 * @return The calling [SuspendingPlugin].
 * @throws IllegalStateException If the calling plugin cannot be determined.
 */
private fun getCallingSuspendingPlugin() = getCallingPlugin(2) as? SuspendingPlugin
    ?: error("Cannot determine plugin")


fun getXFromChunkKey(key: Long): Int {
    return (key and 0xFFFF_FFFFL).toInt()
}

fun getZFromChunkKey(key: Long): Int {
    return (key ushr 32).toInt()
}

fun ChunkSnapshot.getHighestBlockYAtBlockCoordinates(
    blockX: Int,
    blockZ: Int,
): Int {
    return getHighestBlockYAt(blockX and 15, blockZ and 15)
}

suspend fun Collection<Vector3i>.computeHighestYBlock(world: World): ObjectList<Vector3i> {
    val byChunk = mutableLong2ObjectMapOf<ObjectList<Vector3i>>(size / 4 + 1)
    for (point in this) {
        val key = Chunk.getChunkKey(point.x() shr 4, point.z() shr 4)
        val list = byChunk.computeIfAbsent(key) { mutableObjectListOf() }
        list.add(point)
    }

    val snapshots = mutableLong2ObjectMapOf<ChunkSnapshot>(byChunk.size)
    coroutineScope {
        byChunk.keys.map { key ->
            async {
                val snapshot =
                    world.getChunkAtAsync(getXFromChunkKey(key), getZFromChunkKey(key))
                        .await()
                        .getChunkSnapshot(true, false, false, false)
                snapshots.put(key, snapshot)
            }
        }.awaitAll()
    }


    val result = mutableObjectListOf<Vector3i>(size)
    val it = byChunk.long2ObjectEntrySet().fastIterator()
    while (it.hasNext()) {
        val entry = it.next()
        val key = entry.longKey
        val pointsInChunk = entry.value
        val snapshot = snapshots[key] ?: error("ChunkSnapshot for key $key not found")
        for (point in pointsInChunk) {
            val y = snapshot.getHighestBlockYAtBlockCoordinates(point.x(), point.z())
            result.add(Vector3i(point.x(), y, point.z()))
        }
    }

    return result
}

suspend fun World.getBlockAtAsync(pos: BlockPosition): Block {
    val chunkX = pos.blockX() shr 4
    val chunkZ = pos.blockZ() shr 4
    val plugin = JavaPlugin.getProvidingPlugin(SurfBukkitApi::class.java)
    val chunk = getChunkAtAsync(chunkX, chunkZ).await()

    return withContext(plugin.regionDispatcher(this, chunkX, chunkZ)) {
        chunk.getBlock(
            pos.blockX() and 15,
            pos.blockY(),
            pos.blockZ() and 15
        )
    }
}

/**
 * Constructs a human-readable string representing the location, including coordinates and optionally
 * rotation data.
 *
 * @param showRotation Determines whether to include rotation values (yaw and pitch) in the output string.
 */
fun Location.readableString(showRotation: Boolean) = buildString {
    append(world?.name ?: "null")
    append(":(")
    append("%.2f".format(x))
    append(", ")
    append("%.2f".format(y))
    append(", ")
    append("%.2f".format(z))
    if (showRotation) {
        append(") [")
        append("%.2f".format(yaw))
        append(", ")
        append("%.2f".format(pitch))
        append("]")
    } else {
        append(")")
    }
}

typealias BukkitSound = Sound