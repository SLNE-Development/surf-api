package dev.slne.surf.api.spring.context

import dev.slne.surf.api.spring.BootstrapComponent
import dev.slne.surf.api.spring.PluginPresence
import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.io.Serial

/**
 * Builds and tracks the contexts of every plugin that runs on surf-api.
 *
 * Each plugin gets its own [AnnotationConfigApplicationContext] whose parent is an import context
 * holding exactly the exports of the plugin's direct dependencies, below surf-api's [root]
 * context. Plugins therefore see surf-api's own beans and their dependencies' public API, but never
 * another plugin's internals or the exports of a dependency's dependencies.
 *
 * On Paper a plugin is first [bootstrap]ped: a small context with only the plugin's
 * [SurfBootstrapLifecycle] and [BootstrapComponent] beans is built below [bootstrapRoot] in the
 * bootstrap phase, when the plugin cannot see every class it will use yet. [start] later builds the
 * full context, whose import context also references the plugin's own bootstrap beans, so the full
 * context can inject them. Platforms without a bootstrap phase call [start] directly.
 *
 * A plugin has to be started after the plugins it depends on; the platform guarantees that through
 * its load order. No context registers a JVM shutdown hook: the platform closes them through
 * [close] and [closeAll] in its own shutdown.
 *
 * All functions are safe to call from any thread.
 *
 * @property bootstrapRoot the parent of every bootstrap context; on platforms without a bootstrap
 * phase, surf-api's only root context
 * @property presence answers [dev.slne.surf.api.spring.ConditionalOnPlugin] in every plugin context
 */
class SurfContextManager(
    private val bootstrapRoot: ConfigurableApplicationContext,
    private val presence: PluginPresence,
) {

    private val log = LogFactory.getLog(SurfContextManager::class.java)
    private val lock = Any()
    private val reserved = HashSet<String>()
    private val bootstrapped = HashMap<String, PluginBootstrapContext>()
    private val started = LinkedHashMap<String, PluginContext>()
    private val aliases = HashMap<String, String>()
    private val importers = HashMap<String, MutableSet<String>>()
    private val pendingClose = HashSet<String>()

    @Volatile
    private var root: ConfigurableApplicationContext = bootstrapRoot

    /**
     * Makes [root] the parent of every import context built from now on.
     *
     * On Paper surf-api's full root context is built in its load phase, below the root bootstrap
     * context the plugins' bootstrap contexts already sit on.
     *
     * @throws IllegalArgumentException if [root] does not descend from [bootstrapRoot]
     */
    fun attachRoot(root: ConfigurableApplicationContext) {
        require(generateSequence<ApplicationContext>(root) { it.parent }.any { it === bootstrapRoot }) {
            "The root context must descend from the root bootstrap context"
        }

        this.root = root
    }

    /**
     * Builds the bootstrap context of the plugin described by [descriptor].
     *
     * It contains only the components of the plugin that implement [SurfBootstrapLifecycle] or carry
     * [BootstrapComponent]; their dependencies must come from surf-api's root bootstrap context or be
     * such components themselves. Run its hooks through [PluginBootstrapContext.bootstrap], then
     * call [start] for the same plugin.
     *
     * @throws IllegalArgumentException if the plugin shares surf-api's class loader and its packages
     * overlap [SurfRootContext.SURF_API_PACKAGE]
     * @throws IllegalStateException if the plugin already has a context
     * @throws PluginContextException if the context cannot be built; nothing of it stays open
     */
    fun bootstrap(
        descriptor: PluginDescriptor,
        configure: (AnnotationConfigApplicationContext) -> Unit = {},
    ): PluginBootstrapContext {
        checkOutsideRootScan(descriptor)
        reserve(descriptor.name)

        try {
            val managed = withContextClassLoader(descriptor.classLoader) {
                buildPluginManagedContext(
                    descriptor,
                    bootstrapRoot,
                    "surf bootstrap context of '${descriptor.name}'",
                    ScanSelection.BOOTSTRAP,
                    configure,
                )
            }
            val bootstrap = PluginBootstrapContext(descriptor, managed)
            synchronized(lock) { bootstrapped[descriptor.name] = bootstrap }
            return bootstrap
        } catch (exception: Throwable) {
            release(descriptor.name)
            throw PluginContextException(
                "Failed to build the bootstrap context of plugin '${descriptor.name}'",
                exception,
            )
        }
    }

    /**
     * Builds and refreshes the full context of the plugin described by [descriptor].
     *
     * If the plugin was [bootstrap]ped, the full context leaves out the bootstrap beans and injects
     * them through its import context instead. [coroutineScope] becomes the context's primary
     * `CoroutineScope` bean and is owned by the returned [PluginContext] from then on; it is
     * cancelled if the context cannot be built. [configure] runs before the refresh and may register
     * further beans or singletons, such as the platform's plugin instance.
     *
     * @throws IllegalArgumentException if the plugin shares surf-api's class loader and its packages
     * overlap [SurfRootContext.SURF_API_PACKAGE]
     * @throws IllegalStateException if the plugin already has a full context
     * @throws PluginContextException if the context cannot be built; nothing of it stays open,
     * including its bootstrap context
     */
    fun start(
        descriptor: PluginDescriptor,
        coroutineScope: CoroutineScope,
        configure: (AnnotationConfigApplicationContext) -> Unit = {},
    ): PluginContext {
        checkOutsideRootScan(descriptor)
        val bootstrap = synchronized(lock) {
            check(descriptor.name !in started) { "Plugin '${descriptor.name}' already has a context" }
            bootstrapped.remove(descriptor.name)
        }
        if (bootstrap == null) reserve(descriptor.name)

        try {
            val pluginContext = withContextClassLoader(descriptor.classLoader) {
                buildPluginContext(descriptor, bootstrap?.managed, coroutineScope, configure)
            }
            register(pluginContext)
            return pluginContext
        } catch (exception: Throwable) {
            coroutineScope.cancel("The context of plugin '${descriptor.name}' could not be built")

            if (bootstrap != null) {
                closeLogging(descriptor.name) {
                    closeContexts(bootstrap.managed.context)
                }
            }

            release(descriptor.name)
            throw PluginContextException(
                "Failed to build the context of plugin '${descriptor.name}'",
                exception,
            )
        }
    }

    /**
     * The full context of the plugin named [name] or registered under that alias, or `null` if it
     * has none.
     */
    fun find(name: String): PluginContext? = synchronized(lock) { started[resolve(name)] }

    /**
     * Closes the contexts of the plugin named [name], if it has any.
     *
     * While plugins that import from it are still open, the contexts stay open, so those plugins
     * never hold destroyed beans; they close as soon as the last of them closes. The plugin's
     * coroutine scope is cancelled right away either way.
     */
    fun close(name: String) {
        val (bootstrapOnly, closable) = synchronized(lock) { detachLocked(name) }

        // The plugin is gone even while its contexts stay open for its importers.
        find(name)?.coroutineScope?.cancel("Plugin '$name' is closed")

        if (bootstrapOnly != null) {
            closeLogging(name) {
                closeContexts(bootstrapOnly.managed.context)
            }
        }

        for (pluginContext in closable) {
            closeLogging(pluginContext.descriptor.name, pluginContext::close)
        }
    }

    /**
     * Closes every plugin context, last started first, so that consumers close before their
     * providers. The root contexts are left to the caller.
     */
    fun closeAll() {
        val (contexts, bootstraps) = synchronized(lock) {
            val contexts = started.values.toList()
            val bootstraps = bootstrapped.values.toList()
            started.clear()
            bootstrapped.clear()
            reserved.clear()
            aliases.clear()
            importers.clear()
            pendingClose.clear()
            contexts to bootstraps
        }

        for (pluginContext in contexts.asReversed()) {
            closeLogging(pluginContext.descriptor.name, pluginContext::close)
        }

        for (bootstrapContext in bootstraps) {
            closeLogging(bootstrapContext.descriptor.name) {
                closeContexts(bootstrapContext.context)
            }
        }
    }

    /**
     * Detaches the plugin named [name] and returns what [close] has to close.
     *
     * A plugin that never started is dropped and its bootstrap context returned. A started plugin
     * is queued for closing and stays registered while open plugins still import from it. The
     * returned full contexts are those of the queued plugins that no open plugin imports from
     * anymore, which are removed from the manager's state.
     */
    private fun detachLocked(name: String): ClosableContexts {
        assert(Thread.holdsLock(lock)) { "detachLocked must be called under lock" }

        val bootstrapOnly = bootstrapped.remove(name)
        val closable = when {
            bootstrapOnly != null -> emptyList<PluginContext>().also { reserved -= name }
            name !in started -> emptyList()
            else -> {
                pendingClose += name
                collectClosableLocked()
            }
        }

        return ClosableContexts(bootstrapOnly, closable)
    }

    private fun buildPluginContext(
        descriptor: PluginDescriptor,
        bootstrap: ManagedContext?,
        coroutineScope: CoroutineScope,
        configure: (AnnotationConfigApplicationContext) -> Unit,
    ): PluginContext {
        val dependencies = synchronized(lock) {
            descriptor.dependencies.mapNotNull { started[resolve(it)] }.distinct()
        }
        val ownBootstrapBeans = bootstrap
            ?.let { collectReferences(descriptor.name, it.context, descriptor.classLoader) }
            .orEmpty()
        val imports = dependencies.flatMap(PluginContext::exports) + ownBootstrapBeans
        val importContext = createImportContext(descriptor.name, root, imports)

        val selection =
            if (bootstrap != null) ScanSelection.WITHOUT_BOOTSTRAP else ScanSelection.ALL
        val displayName = "surf plugin context of '${descriptor.name}'"
        val managed = try {
            buildPluginManagedContext(
                descriptor,
                importContext,
                displayName,
                selection
            ) { context ->
                registerCoroutineScope(context, coroutineScope, primary = true)
                root.getBeansOfType<ContextContributor>().values.forEach {
                    it.contribute(context, descriptor)
                }
                configure(context)
            }
        } catch (exception: Throwable) {
            importContext.close()
            throw exception
        }

        val ownExports = try {
            listOfNotNull(bootstrap, managed).flatMap {
                collectExports(descriptor.name, it.context, descriptor.classLoader)
            }
        } catch (exception: Throwable) {
            closeContexts(managed.context, importContext)
            throw exception
        }

        return PluginContext(
            descriptor,
            bootstrap,
            importContext,
            managed,
            ownExports,
            coroutineScope
        )
    }

    private fun buildPluginManagedContext(
        descriptor: PluginDescriptor,
        parent: ApplicationContext,
        displayName: String,
        selection: ScanSelection,
        configure: (AnnotationConfigApplicationContext) -> Unit,
    ): ManagedContext {
        return buildManagedContext(
            descriptor.name,
            descriptor.classLoader,
            parent,
            displayName,
        ) { context ->
            context.beanFactory.registerSingleton(PluginPresence.BEAN_NAME, presence)
            scanComponents(
                context,
                descriptor.classLoader,
                descriptor.basePackages,
                selection,
                descriptor.source
            )
            configure(context)
        }
    }

    private fun register(pluginContext: PluginContext) {
        val descriptor = pluginContext.descriptor
        synchronized(lock) {
            started[descriptor.name] = pluginContext
            descriptor.aliases.forEach { aliases[it] = descriptor.name }
            descriptor.dependencies
                .map(::resolve)
                .filter(started::containsKey)
                .forEach { provider -> importers.getOrPut(provider, ::HashSet) += descriptor.name }
        }
    }

    /**
     * Removes every pending plugin that no open plugin imports from anymore, and returns them in
     * the order they have to close: consumers before their providers.
     */
    private fun collectClosableLocked(): List<PluginContext> {
        assert(Thread.holdsLock(lock)) { "collectClosableLocked must be called under lock" }

        val closable = mutableListOf<PluginContext>()
        while (true) {
            val next = pendingClose.firstOrNull { importers[it].isNullOrEmpty() } ?: break
            pendingClose -= next
            val pluginContext = started.remove(next) ?: continue
            reserved -= next
            importers -= next
            importers.values.forEach { it -= next }
            aliases.values.removeAll { it == next }
            closable += pluginContext
        }

        return closable
    }

    /**
     * Rejects a plugin whose components surf-api's root contexts would scan as well, or that would
     * scan surf-api's own components.
     *
     * Only a plugin that shares surf-api's class loader can collide, such as a plugin shaded into a
     * Minestom server; a plugin with a class loader of its own is invisible to the root scan.
     */
    private fun checkOutsideRootScan(descriptor: PluginDescriptor) {
        val rootClassLoader = checkNotNull(bootstrapRoot.classLoader) {
            "The root bootstrap context has no class loader"
        }
        checkOutsideRootScan(descriptor, rootClassLoader)
    }

    private fun reserve(name: String) = synchronized(lock) {
        check(reserved.add(name)) { "Plugin '$name' already has a context" }
    }

    private fun release(name: String) = synchronized(lock) { reserved -= name }

    private fun resolve(name: String) = aliases[name] ?: name

    private inline fun closeLogging(name: String, close: () -> Unit) {
        try {
            close()
        } catch (exception: Throwable) {
            log.error("Failed to close the context of plugin '$name'", exception)
        }
    }
}

/**
 * What [SurfContextManager.close] closes for one plugin, collected under its lock.
 */
private data class ClosableContexts(
    val bootstrapOnly: PluginBootstrapContext?,
    val closable: List<PluginContext>,
)

/**
 * Builds and refreshes a context below [parent] for the owner named [owner], with a lifecycle
 * driver installed before the refresh.
 *
 * [prepare] registers the context's beans. The context is closed again if anything fails.
 */
internal fun buildManagedContext(
    owner: String,
    classLoader: ClassLoader,
    parent: ApplicationContext?,
    displayName: String,
    prepare: (AnnotationConfigApplicationContext) -> Unit,
): ManagedContext {
    val hooks = LifecycleHookRunner(owner)
    val context = AnnotationConfigApplicationContext(PluginBeanFactory())

    context.parent = parent
    context.classLoader = classLoader
    context.setResourceLoader(PluginResourceLoader(classLoader))
    context.displayName = displayName

    hooks.install(context.beanFactory)

    try {
        prepare(context)
        context.refresh()
    } catch (exception: Throwable) {
        context.close()
        throw exception
    }

    return ManagedContext(context, hooks)
}

/**
 * Thrown when a plugin's context cannot be built.
 */
class PluginContextException(message: String, cause: Throwable) : RuntimeException(message, cause) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 834711852878229440L
    }
}

/**
 * Rejects [descriptor] if it shares surf-api's [rootClassLoader] and its packages overlap
 * [SurfRootContext.SURF_API_PACKAGE]: surf-api's root scan would pick up its components as well, or
 * its own scan would pick up surf-api's.
 *
 * A plugin with a class loader of its own is invisible to the root scan and always passes.
 */
internal fun checkOutsideRootScan(descriptor: PluginDescriptor, rootClassLoader: ClassLoader) {
    if (descriptor.classLoader !== rootClassLoader) return

    val overlapping = descriptor.basePackages.filter(::overlapsRootScan)
    require(overlapping.isEmpty()) {
        "'${descriptor.name}' shares surf-api's class loader, and its packages $overlapping overlap " +
                "${SurfRootContext.SURF_API_PACKAGE}, which surf-api's root context scans; move it to a package of its own"
    }
}

private fun overlapsRootScan(basePackage: String): Boolean {
    val rootPackage = SurfRootContext.SURF_API_PACKAGE
    return basePackage.isEmpty() ||
            basePackage == rootPackage ||
            basePackage.startsWith("$rootPackage.") ||
            rootPackage.startsWith("$basePackage.")
}
