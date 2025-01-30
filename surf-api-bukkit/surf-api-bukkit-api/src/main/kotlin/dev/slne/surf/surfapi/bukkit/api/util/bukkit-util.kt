@file:JvmName("UtilBukkit")

package dev.slne.surf.surfapi.bukkit.api.util

import com.github.shynixn.mccoroutine.folia.SuspendingPlugin
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.regionDispatcher
import dev.slne.surf.surfapi.core.api.util.getCallerClass
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
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

/**
 * Executes a suspendable action on each online player, optionally concurrently.
 *
 * @param action The suspendable action to perform on each player.
 * @param concurrent If `true`, actions will run concurrently; otherwise, sequentially. Default is `false`.
 */
suspend fun forEachPlayerInRegion(
    action: suspend (player: Player) -> Unit,
    concurrent: Boolean = false,
) {
    if (concurrent) {
        coroutineScope {
            Bukkit.getOnlinePlayers()
                .map {
                    async {
                        withContext(it.dispatcher(getCallingSuspendingPlugin())) {
                            action(it)
                        }
                    }
                }.awaitAll()
        }
    } else {
        for (player in Bukkit.getOnlinePlayers()) {
            withContext(player.dispatcher(getCallingSuspendingPlugin())) {
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
 * Gets the chunk Z coordinate of this location.
 */
val Location.chunkZ get() = blockZ shr 4

/**
 * Gets the chunk key of this location.
 */
val Location.chunkKey get() = Chunk.getChunkKey(this)

/**
 * Converts an iterable of UUIDs to a list of online [Player] instances.
 *
 * @receiver The collection of UUIDs.
 * @return A list of [Player] instances corresponding to the UUIDs, excluding offline players.
 */
fun Iterable<UUID>.toPlayers() = mapNotNull { Bukkit.getPlayer(it) }

/**
 * Converts a sequence of UUIDs to a sequence of online [Player] instances.
 *
 * @receiver The sequence of UUIDs.
 * @return A list of [Player] instances corresponding to the UUIDs, excluding offline players.
 */
fun Sequence<UUID>.toPlayers() = mapNotNull { Bukkit.getPlayer(it) }

/**
 * Checks if the player can see the specified location.
 *
 * @receiver The player.
 * @param location The location to check.
 * @return `true` if the player can see the location, `false` otherwise.
 */
fun Player.seesLocation(location: Location): Boolean {
    val sameWorld = world == location.world
    val chunkSent = isChunkSent(Chunk.getChunkKey(location))

    println("sameWorld: $sameWorld, chunkSent: $chunkSent")

    return this.world == location.world && this.isChunkSent(Chunk.getChunkKey(location))
}

/**
 * Checks if the player can see the specified location based on chunk visibility.
 *
 * @receiver The player.
 * @param location The location to check.
 * @return `true` if the player can see the chunk containing the location, `false` otherwise.
 */
fun Player.seesLocation2(location: Location): Boolean =
    this.world == location.world && location.world.getPlayersSeeingChunk(
        location.chunkX,
        location.chunkZ
    ).contains(this)

/**
 * Retrieves the coroutine dispatcher for this entity.
 *
 * @receiver The entity.
 * @param plugin The suspending plugin instance. Defaults to the calling suspending plugin.
 * @return The entity's coroutine dispatcher.
 */
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