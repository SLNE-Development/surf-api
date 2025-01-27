@file:JvmName("UtilBukkit")

package dev.slne.surf.surfapi.bukkit.api.util

import dev.slne.surf.surfapi.core.api.util.getCallerClass
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

fun key(name: String): NamespacedKey { // TODO: Verify if this works
    return NamespacedKey(getCallingPlugin(), name)
}

fun getCallingPlugin(depth: Int = 1): JavaPlugin {
    val caller = getCallerClass(depth) ?: error("Cannot determine caller class")
    return JavaPlugin.getProvidingPlugin(caller)
}

fun forEachPlayer(action: (player: Player) -> Unit) {
    Bukkit.getOnlinePlayers().forEach(action)
}

infix fun Location.distanceSqt(other: Location): Double = distanceSquared(other)

fun Iterable<UUID>.toPlayers() = mapNotNull { Bukkit.getPlayer(it) }
fun Player.seesLocation(location: Location): Boolean {
    val sameWorld = world == location.world
    val chunkSent = isChunkSent(Chunk.getChunkKey(location))

    println("sameWorld: $sameWorld, chunkSent: $chunkSent")

    return this.world == location.world && this.isChunkSent(Chunk.getChunkKey(location))
}
