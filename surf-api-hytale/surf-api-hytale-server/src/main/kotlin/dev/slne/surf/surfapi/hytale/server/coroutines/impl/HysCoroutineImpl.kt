package dev.slne.surf.surfapi.hytale.server.coroutines.impl

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import dev.slne.surf.surfapi.hytale.api.coroutines.CoroutineSession
import dev.slne.surf.surfapi.hytale.api.coroutines.HysCoroutine
import java.util.concurrent.ConcurrentHashMap

class HysCoroutineImpl : HysCoroutine {
    private val items = ConcurrentHashMap<JavaPlugin, CoroutineSessionImpl>()

    override fun getCoroutineSession(plugin: JavaPlugin): CoroutineSession {
        if (!items.containsKey(plugin)) {
            startCoroutineSession(plugin)
        }

        return items[plugin]!!
    }

    override fun disable(plugin: JavaPlugin) {
        if (!items.containsKey(plugin)) {
            return
        }

        items.remove(plugin)!!.dispose()
    }

    private fun startCoroutineSession(plugin: JavaPlugin) {
        if (!plugin.isEnabled) {
            throw RuntimeException("Plugin ${plugin.name} attempted to start a new coroutine session while being disabled!")
        }

        if (items.containsKey(plugin)) {
            return
        }

        items[plugin] = CoroutineSessionImpl(plugin)
    }
}