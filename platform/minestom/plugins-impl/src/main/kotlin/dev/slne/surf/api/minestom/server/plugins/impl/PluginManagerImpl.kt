package dev.slne.surf.api.minestom.server.plugins.impl

import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.plugin.PluginMeta
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.minestom.server.impl.command.PluginCommands
import dev.slne.surf.api.spring.PluginPresence
import dev.slne.surf.api.spring.context.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ConfigurableApplicationContext
import java.lang.reflect.Modifier
import java.nio.file.Path
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.io.path.createDirectories
import kotlin.io.path.div

/**
 * Loads, enables and disables the plugins of the server.
 *
 * Every plugin gets a Spring context below [root], built by a [SurfContextManager] in dependency
 * order. The load phase creates the plugin, builds its context and runs the load hooks of its beans.
 * The enable phase attaches the plugin's [EventNode] beans to its event node, registers its commands
 * and event bus listeners, and runs the enable hooks. The disable phase runs the disable hooks,
 * removes everything the enable phase registered and closes the context. A plugin's class loader is
 * closed once no plugin imports from its context anymore.
 *
 * Phases run to completion on the calling thread. A plugin that fails to load is cleaned up and
 * removed, and plugins that require it are not loaded. A plugin that fails to enable is disabled and
 * removed, and plugins that require it are disabled without being enabled. [load] and [enable]
 * return the plugins that failed, so that the caller can decide whether to continue. [plugins] and
 * [plugin] may be called from any thread.
 *
 * While a phase runs, and in a plugin's coroutine scope, the thread context class loader is the
 * plugin's class loader.
 *
 * @param root surf-api's root context, the ancestor of every plugin context
 */
internal class PluginManagerImpl(
    private val directory: Path,
    private val parentNode: EventNode<Event>,
    private val serverClassLoader: ClassLoader,
    root: ConfigurableApplicationContext,
) : PluginManager {
    private val loaded = mutableListOf<LoadedPlugin>()
    private val contexts = SurfContextManager(root, PluginPresence(::isLoaded))

    /**
     * Class loaders of disabled plugins whose contexts stay open because other plugins still import
     * from them.
     */
    private val released = mutableListOf<Pair<String, PluginClassLoader?>>()

    override val plugins: Collection<SurfMinestomPlugin>
        get() = synchronized(loaded) { loaded.map { it.plugin } }

    override fun plugin(id: String): SurfMinestomPlugin? =
        synchronized(loaded) { loaded.firstOrNull { it.plugin.meta.id == id }?.plugin }

    /**
     * Creates every loadable plugin of [candidates] in dependency order, builds its context and runs
     * the load hooks of its beans. Plugins that cannot be loaded are logged.
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
            val reason = loadOne(candidate, failed, loaders) ?: continue
            failed += candidate.meta.id
            failures[candidate.meta.id] = reason
        }

        return failures
    }

    /**
     * Creates the plugin of [candidate], builds its context and runs the load hooks of its beans,
     * unless a required dependency is among [failed].
     *
     * @return why the plugin could not be loaded, or `null` when it loaded
     */
    private fun loadOne(
        candidate: PluginCandidate,
        failed: Set<String>,
        loaders: MutableMap<String, PluginClassLoader>,
    ): String? {
        val meta = candidate.meta
        val failedDependency = meta.dependencies.firstOrNull { !it.optional && it.id in failed }
        if (failedDependency != null) {
            LOGGER.error("Not loading the plugin {}, because its dependency {} failed", meta.id, failedDependency.id)
            return "its dependency ${failedDependency.id} failed"
        }

        val loadedPlugin = try {
            create(candidate, loaders)
        } catch (failure: Throwable) {
            LOGGER.error("Could not create the plugin {} from {}", meta.id, candidate.source, failure)
            return "it could not be created: ${describe(failure)}"
        }

        LOGGER.info("Loading {} {}", meta.name, meta.version)
        val loadFailure = runLifecycle(loadedPlugin, "load") { startContext(candidate, loadedPlugin).load() }
        if (loadFailure != null) {
            cleanUp(loadedPlugin)
            loaders -= meta.id
            return "onLoad failed: ${describe(loadFailure)}"
        }

        synchronized(loaded) { loaded += loadedPlugin }
        return null
    }

    /**
     * Builds the context of [loadedPlugin] and records it on the plugin before its load hooks run.
     */
    private fun startContext(candidate: PluginCandidate, loadedPlugin: LoadedPlugin): PluginContext {
        val descriptor = descriptorOf(candidate, loadedPlugin)
        val pluginContext = contexts.start(descriptor, loadedPlugin.scope) { context ->
            context.registerBeanDefinition(PLUGIN_INSTANCE_BEAN, instanceDefinition(loadedPlugin.plugin))
        }
        loadedPlugin.context = pluginContext
        return pluginContext
    }

    /**
     * Registers the event nodes, commands and event bus listeners of every loaded plugin and runs
     * the enable hooks of its beans. A plugin that fails, or whose required dependency failed, is
     * disabled, which includes running its disable hooks.
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

            val enableFailure = runLifecycle(loadedPlugin, "enable") {
                val pluginContext = checkNotNull(loadedPlugin.context) { "${meta.id} has no context" }
                // Before the hooks, so that a hook can rely on the plugin's listeners and commands
                loadedPlugin.registrations.register(pluginContext.beanContexts)
                pluginContext.enable()
            }
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
        contexts.closeAll()
        closeReleasedClassLoaders()
    }

    private fun disable(loadedPlugin: LoadedPlugin) {
        val meta = loadedPlugin.plugin.meta
        LOGGER.info("Disabling {} {}", meta.name, meta.version)

        runLifecycle(loadedPlugin, "disable") { loadedPlugin.context?.disable() }
        cleanUp(loadedPlugin)
        synchronized(loaded) { loaded -= loadedPlugin }
    }

    private fun create(
        candidate: PluginCandidate,
        loaders: MutableMap<String, PluginClassLoader>,
    ): LoadedPlugin {
        val meta = candidate.meta
        val classLoader = candidate.jar?.let { jar -> createClassLoader(candidate, jar, loaders) }

        val logger = ComponentLogger.logger(meta.name)
        val eventNode = EventNode.all("plugin-${meta.id}")
        var scope: CoroutineScope? = null

        try {
            // Created before the plugin, so that it exists while the plugin initializes
            val dataDirectory = (directory / meta.id).createDirectories()

            // The context must exist during construction so property initializers can use it; for an
            // `object` that is during class initialization.
            val plugin = constructPlugin(meta, classLoader) { plugin ->
                val pluginScope = surfCoroutineScope(
                    owner = meta.id,
                    classLoader = classLoader ?: serverClassLoader,
                    dispatcher = Dispatchers.Default,
                    elements = SurfMinestomPlugin.contextElement(plugin),
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
            }

            val pluginScope = checkNotNull(scope) { "${meta.main} was not constructed by the server" }
            parentNode.addChild(eventNode)

            return LoadedPlugin(plugin, classLoader, pluginScope, PluginRegistrations(eventNode))
        } catch (failure: Throwable) {
            scope?.cancel()
            loaders -= meta.id
            closeQuietly(classLoader, meta.id)
            throw failure
        }
    }

    /**
     * Creates the class loader of the plugin jar [jar] and registers it in [loaders], so that the
     * plugins loaded after it can depend on it.
     */
    private fun createClassLoader(
        candidate: PluginCandidate,
        jar: Path,
        loaders: MutableMap<String, PluginClassLoader>,
    ): PluginClassLoader {
        val id = candidate.meta.id
        return PluginClassLoader(id, jar, serverClassLoader).also { loader ->
            loader.dependencies = transitiveDependencies(id, candidate, loaders)
            loaders[id] = loader
        }
    }

    /**
     * Instantiates the main class of [meta] from [classLoader], or from the server's class loader
     * for a plugin without a jar of its own; [context] supplies the plugin's context while it is
     * constructed.
     */
    private fun constructPlugin(
        meta: PluginMeta,
        classLoader: PluginClassLoader?,
        context: (SurfMinestomPlugin) -> SurfMinestomPlugin.Context,
    ): SurfMinestomPlugin = SurfMinestomPlugin.constructing(context) {
        val mainClass = Class.forName(meta.main, false, classLoader ?: serverClassLoader)
        require(SurfMinestomPlugin::class.java.isAssignableFrom(mainClass)) {
            "${meta.main} does not extend ${SurfMinestomPlugin::class.java.name}"
        }
        instantiate(mainClass)
    }

    /**
     * Returns the `object` instance of [mainClass], or a new instance from its no-argument
     * constructor.
     */
    private fun instantiate(mainClass: Class<*>): SurfMinestomPlugin {
        val objectInstance = mainClass.declaredFields.firstOrNull { field ->
            field.name == "INSTANCE" &&
                    Modifier.isStatic(field.modifiers) &&
                    field.type == mainClass
        }

        val instance = objectInstance?.get(null) ?: mainClass.getDeclaredConstructor().newInstance()
        return instance as SurfMinestomPlugin
    }

    private fun descriptorOf(candidate: PluginCandidate, loadedPlugin: LoadedPlugin) = PluginDescriptor(
        name = candidate.meta.id,
        classLoader = loadedPlugin.classLoader ?: serverClassLoader,
        source = candidate.jar ?: codeSourceOf(loadedPlugin.plugin.javaClass),
        basePackages = listOf(loadedPlugin.plugin.javaClass.packageName),
        dependencies = candidate.meta.dependencies.map { it.id },
        dataDirectory = loadedPlugin.plugin.dataDirectory,
    )

    /**
     * The jar or classes directory [type] was loaded from, which is where a plugin without a jar of
     * its own, such as one placed on the class path by the server's bootstrap, keeps its classes;
     * `null` if the class loader does not say.
     */
    private fun codeSourceOf(type: Class<*>): Path? = runCatching {
        Path.of(type.protectionDomain.codeSource.location.toURI())
    }.getOrNull()

    /**
     * A primary definition of [plugin] under its concrete type.
     *
     * The manager creates the instance, but the context manages it like any other singleton: it
     * injects `@Autowired` members and runs the `Aware` callbacks, `@PostConstruct` and every
     * `BeanPostProcessor` on it, and calls `@PreDestroy` and `DisposableBean.destroy` when it closes.
     * Only the inferred `close()` or `shutdown()` call is suppressed.
     */
    private fun instanceDefinition(plugin: SurfMinestomPlugin) = RootBeanDefinition(plugin.javaClass).apply {
        setInstanceSupplier { plugin }
        isPrimary = true
        destroyMethodName = ""
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
        val id = plugin.meta.id

        try {
            loadedPlugin.registrations.unregister()
        } catch (failure: Throwable) {
            LOGGER.error("Could not unregister the listeners of the plugin {}", id, failure)
        }
        PluginCommands.unregisterAll(plugin)
        parentNode.removeChild(plugin.eventNode)
        // The context owns the scope once it exists and cancels it after the disable hooks.
        if (loadedPlugin.context == null) loadedPlugin.scope.cancel()

        try {
            contexts.close(id)
        } catch (failure: Throwable) {
            LOGGER.error("Could not close the context of the plugin {}", id, failure)
        }
        released += id to loadedPlugin.classLoader
        closeReleasedClassLoaders()
    }

    /**
     * Closes the class loaders of disabled plugins whose contexts are closed. A context stays open
     * while a plugin that imports from it is open, and its beans need the class loader until then.
     */
    private fun closeReleasedClassLoaders() {
        released.removeAll { (id, classLoader) ->
            val closed = contexts.find(id) == null
            if (closed) closeQuietly(classLoader, id)
            closed
        }
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

    private class LoadedPlugin(
        val plugin: SurfMinestomPlugin,
        val classLoader: PluginClassLoader?,
        /**
         * The plugin's coroutine scope, handed to its context in [PluginManagerImpl.load], which
         * owns and cancels it from then on.
         */
        val scope: CoroutineScope,
        val registrations: PluginRegistrations,
    ) {
        /**
         * Set once the plugin's context is built, which is before its load hooks run.
         */
        var context: PluginContext? = null
    }

    private companion object {
        val LOGGER = ComponentLogger.logger("PluginManager")

        const val PLUGIN_INSTANCE_BEAN = "surfPluginInstance"
    }
}
