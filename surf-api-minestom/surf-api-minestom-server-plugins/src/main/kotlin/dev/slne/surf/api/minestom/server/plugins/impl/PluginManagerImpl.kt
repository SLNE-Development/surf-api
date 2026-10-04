package dev.slne.surf.api.minestom.server.plugins.impl

import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.minestom.server.impl.command.PluginCommands
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Loads, enables and disables the plugins of the server.
 */
@OptIn(InternalSurfApi::class)
internal class PluginManagerImpl(
    private val directory: Path,
    private val parentNode: EventNode<Event>,
    private val serverClassLoader: ClassLoader,
) : PluginManager {
    private val loaded = mutableListOf<LoadedPlugin>()

    override val plugins: Collection<SurfMinestomPlugin>
        get() = synchronized(loaded) { loaded.map { it.plugin } }

    override fun plugin(id: String): SurfMinestomPlugin? =
        synchronized(loaded) { loaded.firstOrNull { it.plugin.meta.id == id }?.plugin }

    /**
     * Creates every plugin in [candidates] that can be loaded and calls their `onLoad`.
     */
    fun load(candidates: List<PluginCandidate>) {
        val order = PluginLoadOrder.order(candidates)
        order.rejected.forEach { (id, reason) ->
            LOGGER.error("Not loading the plugin {}, because {}", id, reason)
        }

        val failed = mutableSetOf<String>()
        val loaders = mutableMapOf<String, PluginClassLoader>()

        for (candidate in order.ordered) {
            val meta = candidate.meta
            val failedDependency = meta.dependencies.firstOrNull { !it.optional && it.id in failed }
            if (failedDependency != null) {
                LOGGER.error("Not loading the plugin {}, because its dependency {} failed", meta.id, failedDependency.id)
                failed += meta.id
                continue
            }

            val loadedPlugin = try {
                create(candidate, loaders)
            } catch (failure: Throwable) {
                LOGGER.error("Could not create the plugin {} from {}", meta.id, candidate.source, failure)
                failed += meta.id
                continue
            }

            LOGGER.info("Loading {} {}", meta.name, meta.version)
            if (!runLifecycle(loadedPlugin, "load") { loadedPlugin.plugin.onLoad() }) {
                cleanUp(loadedPlugin)
                failed += meta.id
                continue
            }

            synchronized(loaded) { loaded += loadedPlugin }
        }
    }

    /** Calls `onEnable` of every loaded plugin, disabling those that fail. */
    fun enable() {
        for (loadedPlugin in synchronized(loaded) { loaded.toList() }) {
            val meta = loadedPlugin.plugin.meta
            LOGGER.info("Enabling {} {}", meta.name, meta.version)

            if (!runLifecycle(loadedPlugin, "enable") { loadedPlugin.plugin.onEnable() }) {
                disable(loadedPlugin)
            }
        }
    }

    /** Disables every plugin, in reverse order of loading. */
    fun disableAll() {
        for (loadedPlugin in synchronized(loaded) { loaded.toList() }.asReversed()) {
            disable(loadedPlugin)
        }
    }

    private fun disable(loadedPlugin: LoadedPlugin) {
        val meta = loadedPlugin.plugin.meta
        LOGGER.info("Disabling {} {}", meta.name, meta.version)

        runLifecycle(loadedPlugin, "disable") { loadedPlugin.plugin.onDisable() }
        cleanUp(loadedPlugin)
        synchronized(loaded) { loaded -= loadedPlugin }
    }

    private fun create(
        candidate: PluginCandidate,
        loaders: MutableMap<String, PluginClassLoader>,
    ): LoadedPlugin {
        val meta = candidate.meta
        val classLoader = candidate.jar?.let { jar ->
            PluginClassLoader(meta.id, jar, serverClassLoader).also { loader ->
                loader.dependencies = transitiveDependencies(meta.id, candidate, loaders)
                loaders[meta.id] = loader
            }
        }

        val mainClass = Class.forName(meta.main, true, classLoader ?: serverClassLoader)
        require(SurfMinestomPlugin::class.java.isAssignableFrom(mainClass)) {
            "${meta.main} does not extend ${SurfMinestomPlugin::class.java.name}"
        }

        val plugin = instantiate(mainClass)

        val logger = ComponentLogger.logger(meta.name)
        val eventNode = EventNode.all("plugin-${meta.id}")
        val scope = CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Default +
                    CoroutineName("plugin-${meta.id}") +
                    SurfMinestomPlugin.contextElement(plugin) +
                    CoroutineExceptionHandler { _, failure ->
                        logger.error("Unhandled exception in a coroutine of {}", meta.id, failure)
                    }
        )

        plugin.initialize(
            SurfMinestomPlugin.Context(
                meta = meta,
                eventNode = eventNode,
                dataDirectory = directory / meta.id,
                logger = logger,
                scope = scope,
            )
        )
        parentNode.addChild(eventNode)

        return LoadedPlugin(plugin, classLoader, scope)
    }

    /** The `object` instance of [mainClass], or a new instance from its no-argument constructor. */
    private fun instantiate(mainClass: Class<*>): SurfMinestomPlugin {
        val objectInstance = mainClass.declaredFields.firstOrNull { field ->
            field.name == "INSTANCE" &&
                    java.lang.reflect.Modifier.isStatic(field.modifiers) &&
                    field.type == mainClass
        }

        val instance = objectInstance?.get(null) ?: mainClass.getDeclaredConstructor().newInstance()
        return instance as SurfMinestomPlugin
    }

    private fun transitiveDependencies(
        id: String,
        candidate: PluginCandidate,
        loaders: Map<String, PluginClassLoader>,
    ): List<PluginClassLoader> {
        val result = linkedSetOf<PluginClassLoader>()
        val pending = ArrayDeque(candidate.meta.dependencies.map { it.id })
        while (pending.isNotEmpty()) {
            val dependency = loaders[pending.removeFirst()] ?: continue
            if (dependency.pluginId != id && result.add(dependency)) {
                pending += dependency.dependencies.map { it.pluginId }
            }
        }
        return result.toList()
    }

    /** Runs a lifecycle function of the plugin, reporting whether it completed. */
    private fun runLifecycle(
        loadedPlugin: LoadedPlugin,
        phase: String,
        block: suspend () -> Unit,
    ): Boolean = try {
        runBlocking(SurfMinestomPlugin.contextElement(loadedPlugin.plugin)) { block() }
        true
    } catch (failure: Throwable) {
        LOGGER.error("Could not {} the plugin {}", phase, loadedPlugin.plugin.meta.id, failure)
        false
    }

    private fun cleanUp(loadedPlugin: LoadedPlugin) {
        val plugin = loadedPlugin.plugin

        PluginCommands.unregisterAll(plugin)
        parentNode.removeChild(plugin.eventNode)
        loadedPlugin.scope.cancel()

        try {
            loadedPlugin.classLoader?.close()
        } catch (failure: Exception) {
            LOGGER.warn("Could not close the class loader of {}", plugin.meta.id, failure)
        }
    }

    private class LoadedPlugin(
        val plugin: SurfMinestomPlugin,
        val classLoader: PluginClassLoader?,
        val scope: CoroutineScope,
    )

    private companion object {
        val LOGGER = ComponentLogger.logger("PluginManager")
    }
}
