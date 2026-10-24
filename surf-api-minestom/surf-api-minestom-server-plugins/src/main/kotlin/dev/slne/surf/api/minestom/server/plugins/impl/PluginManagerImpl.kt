package dev.slne.surf.api.minestom.server.plugins.impl

import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.minestom.server.impl.command.PluginCommands
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import kotlinx.coroutines.*
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import java.nio.file.Path
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.createDirectories
import kotlin.io.path.div

/**
 * Loads, enables and disables the plugins of the server.
 *
 * Lifecycle functions run to completion on the calling thread. A plugin that throws from `onLoad`
 * is cleaned up and removed, and plugins that require it are not loaded. A plugin that throws from
 * `onEnable` is disabled and removed, and plugins that require it are disabled without being
 * enabled. [load] and [enable] return the plugins that failed, so that the caller can decide
 * whether to continue. [plugins] and [plugin] may be called from any thread.
 *
 * While a lifecycle function runs, and in a plugin's coroutine scope, the thread context class
 * loader is the plugin's class loader.
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
     * Creates every loadable plugin of [candidates] in dependency order and calls its `onLoad`.
     * Plugins that cannot be loaded are logged.
     *
     * @return the reason for each plugin that could not be loaded, keyed by plugin ID, including
     * rejected candidates, in the order they failed; empty when every plugin loaded
     */
    fun load(candidates: List<PluginCandidate>): Map<String, String> {
        val order = PluginLoadOrder.order(candidates)
        val failures = linkedMapOf<String, String>()
        order.rejected.forEach { (id, reason) ->
            LOGGER.error("Not loading the plugin {}, because {}", id, reason)
            failures[id] = reason
        }

        val failed = mutableSetOf<String>()
        val loaders = mutableMapOf<String, PluginClassLoader>()

        for (candidate in order.ordered) {
            val meta = candidate.meta
            val failedDependency = meta.dependencies.firstOrNull { !it.optional && it.id in failed }
            if (failedDependency != null) {
                LOGGER.error("Not loading the plugin {}, because its dependency {} failed", meta.id, failedDependency.id)
                failed += meta.id
                failures[meta.id] = "its dependency ${failedDependency.id} failed"
                continue
            }

            val loadedPlugin = try {
                create(candidate, loaders)
            } catch (failure: Throwable) {
                LOGGER.error("Could not create the plugin {} from {}", meta.id, candidate.source, failure)
                failed += meta.id
                failures[meta.id] = "it could not be created: ${describe(failure)}"
                continue
            }

            LOGGER.info("Loading {} {}", meta.name, meta.version)
            val loadFailure = runLifecycle(loadedPlugin, "load") { loadedPlugin.plugin.onLoad() }
            if (loadFailure != null) {
                cleanUp(loadedPlugin)
                loaders -= meta.id
                failed += meta.id
                failures[meta.id] = "onLoad failed: ${describe(loadFailure)}"
                continue
            }

            synchronized(loaded) { loaded += loadedPlugin }
        }

        return failures
    }

    /**
     * Calls `onEnable` of every loaded plugin. A plugin that fails, or whose required dependency
     * failed, is disabled, which includes calling its `onDisable`.
     *
     * @return the reason for each plugin that could not be enabled, keyed by plugin ID, in the
     * order they failed; empty when every plugin was enabled
     */
    fun enable(): Map<String, String> {
        val failed = mutableSetOf<String>()
        val failures = linkedMapOf<String, String>()

        for (loadedPlugin in synchronized(loaded) { loaded.toList() }) {
            val meta = loadedPlugin.plugin.meta
            val failedDependency = meta.dependencies.firstOrNull { !it.optional && it.id in failed }
            if (failedDependency != null) {
                LOGGER.error("Not enabling the plugin {}, because its dependency {} failed", meta.id, failedDependency.id)
                failed += meta.id
                failures[meta.id] = "its dependency ${failedDependency.id} failed"
                disable(loadedPlugin)
                continue
            }

            LOGGER.info("Enabling {} {}", meta.name, meta.version)

            val enableFailure = runLifecycle(loadedPlugin, "enable") { loadedPlugin.plugin.onEnable() }
            if (enableFailure != null) {
                failed += meta.id
                failures[meta.id] = "onEnable failed: ${describe(enableFailure)}"
                disable(loadedPlugin)
            }
        }

        return failures
    }

    /**
     * Disables every plugin in reverse load order.
     */
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

        val logger = ComponentLogger.logger(meta.name)
        val eventNode = EventNode.all("plugin-${meta.id}")
        var scope: CoroutineScope? = null

        try {
            // Created before the plugin, so that it exists while the plugin initializes
            val dataDirectory = (directory / meta.id).createDirectories()

            // The context must exist during construction so property initializers can use it; for an
            // `object` that is during class initialization.
            val plugin = SurfMinestomPlugin.constructing({ plugin ->
                val pluginScope = CoroutineScope(
                    SupervisorJob() +
                            Dispatchers.Default +
                            CoroutineName("plugin-${meta.id}") +
                            SurfMinestomPlugin.contextElement(plugin) +
                            contextClassLoaderElement(classLoader) +
                            CoroutineExceptionHandler { _, failure ->
                                logger.error("Unhandled exception in a coroutine of {}", meta.id, failure)
                            }
                )
                check(scope == null) { "${meta.main} constructed more than one plugin" }
                scope = pluginScope

                SurfMinestomPlugin.Context(
                    meta = meta,
                    eventNode = eventNode,
                    dataDirectory = dataDirectory,
                    logger = logger,
                    scope = pluginScope,
                )
            }) {
                val mainClass = Class.forName(meta.main, false, classLoader ?: serverClassLoader)
                require(SurfMinestomPlugin::class.java.isAssignableFrom(mainClass)) {
                    "${meta.main} does not extend ${SurfMinestomPlugin::class.java.name}"
                }
                instantiate(mainClass)
            }

            val pluginScope = checkNotNull(scope) { "${meta.main} was not constructed by the server" }
            parentNode.addChild(eventNode)

            return LoadedPlugin(plugin, classLoader, pluginScope)
        } catch (failure: Throwable) {
            scope?.cancel()
            loaders -= meta.id
            closeQuietly(classLoader, meta.id)
            throw failure
        }
    }

    /**
     * Returns the `object` instance of [mainClass], or a new instance from its no-argument
     * constructor.
     */
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

    /**
     * Runs a lifecycle function of the plugin and returns what it threw, or `null` when it
     * completed.
     */
    private fun runLifecycle(
        loadedPlugin: LoadedPlugin,
        phase: String,
        block: suspend () -> Unit,
    ): Throwable? = try {
        val context = SurfMinestomPlugin.contextElement(loadedPlugin.plugin) +
                contextClassLoaderElement(loadedPlugin.classLoader)
        runBlocking(context) { block() }
        null
    } catch (failure: Throwable) {
        LOGGER.error("Could not {} the plugin {}", phase, loadedPlugin.plugin.meta.id, failure)
        failure
    }

    private fun describe(failure: Throwable): String =
        failure.message?.let { "${failure.javaClass.simpleName}: $it" } ?: failure.javaClass.simpleName

    private fun cleanUp(loadedPlugin: LoadedPlugin) {
        val plugin = loadedPlugin.plugin

        PluginCommands.unregisterAll(plugin)
        parentNode.removeChild(plugin.eventNode)
        loadedPlugin.scope.cancel()
        closeQuietly(loadedPlugin.classLoader, plugin.meta.id)
    }

    private fun closeQuietly(classLoader: PluginClassLoader?, pluginId: String) {
        try {
            classLoader?.close()
        } catch (failure: Exception) {
            LOGGER.warn("Could not close the class loader of {}", pluginId, failure)
        }
    }

    /**
     * Makes [classLoader] the thread context class loader of the thread a coroutine runs on and
     * restores the previous one afterwards. Empty for a plugin without a class loader of its own.
     */
    private fun contextClassLoaderElement(classLoader: ClassLoader?): CoroutineContext =
        if (classLoader == null) EmptyCoroutineContext else ContextClassLoaderElement(classLoader)

    private class ContextClassLoaderElement(
        private val classLoader: ClassLoader,
    ) : AbstractCoroutineContextElement(Key), ThreadContextElement<ClassLoader?> {
        override fun updateThreadContext(context: CoroutineContext): ClassLoader? {
            val thread = Thread.currentThread()
            val previous = thread.contextClassLoader
            thread.contextClassLoader = classLoader
            return previous
        }

        override fun restoreThreadContext(context: CoroutineContext, oldState: ClassLoader?) {
            Thread.currentThread().contextClassLoader = oldState
        }

        companion object Key : CoroutineContext.Key<ContextClassLoaderElement>
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
