package dev.slne.surf.surfapi.bukkit.api.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal val mainThreadDispatches = mutableMapOf<Plugin, BukkitMainThreadDispatcher>()

@PublishedApi
internal val asyncDispatches = mutableMapOf<Plugin, BukkitAsyncDispatcher>()

class BukkitMainThreadDispatcher(private val plugin: Plugin) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        plugin.server.scheduler.runTask(plugin, block)
    }
}

class BukkitAsyncDispatcher(private val plugin: Plugin) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, block)
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Any.syncDispatcher(): CoroutineDispatcher {
    val plugin = JavaPlugin.getProvidingPlugin(this::class.java)
    return mainThreadDispatches.getOrPut(plugin) { BukkitMainThreadDispatcher(plugin) }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Any.asyncDispatcher(): CoroutineDispatcher {
    val plugin = JavaPlugin.getProvidingPlugin(this::class.java)
    return asyncDispatches.getOrPut(plugin) { BukkitAsyncDispatcher(plugin) }
}