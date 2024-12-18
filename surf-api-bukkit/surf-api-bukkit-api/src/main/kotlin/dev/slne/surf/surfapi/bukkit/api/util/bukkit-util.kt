package dev.slne.surf.surfapi.bukkit.api.util

import dev.slne.surf.surfapi.core.api.util.getCallerClass
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin

fun key(name: String): NamespacedKey { // TODO: Verify if this works
    return NamespacedKey(getCallingPlugin(), name)
}

fun getCallingPlugin(): JavaPlugin {
    val caller = getCallerClass(1) ?: error("Cannot determine caller class")
    return JavaPlugin.getProvidingPlugin(caller)
}

fun forEachPlayer(action: (player: Player) -> Unit) {
    Bukkit.getOnlinePlayers().forEach(action)
}

infix fun Location.distanceSqt(other: Location): Double = distanceSquared(other)
