package dev.slne.surf.surfapi.bukkit.api.util

import dev.slne.surf.surfapi.core.api.util.getCallerClass
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

fun key(name: String): NamespacedKey { // TODO: Verify if this works
    return NamespacedKey(getCallingPlugin(), name)
}

fun getCallingPlugin(): JavaPlugin {
    val caller = getCallerClass(1) ?: error("Cannot determine caller class")
    return JavaPlugin.getProvidingPlugin(caller)
}