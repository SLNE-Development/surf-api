package dev.slne.surf.api.paper.server.spring

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.core.server.spring.CommandApiCommandBeans
import dev.slne.surf.api.core.server.spring.SurfEventHandlerBeans
import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.paper.coroutine.PaperDispatchersElement
import dev.slne.surf.api.paper.coroutine.PaperSchedulers
import dev.slne.surf.api.paper.coroutine.PluginDispatchers
import dev.slne.surf.api.paper.event.BukkitListener
import dev.slne.surf.api.paper.nms.common.NmsVersionResolver
import dev.slne.surf.api.paper.plugin.SurfPaperBootstrapper
import dev.slne.surf.api.paper.plugin.SurfPaperPlugin
import dev.slne.surf.api.paper.plugin.SurfPaperPluginRuntime
import dev.slne.surf.api.paper.server.hook.papi.PapiExpansionBeans
import dev.slne.surf.api.paper.server.hook.papi.PapiExpansionRegistration
import dev.slne.surf.api.paper.server.hook.papi.SurfPaperPAPIHookImpl
import dev.slne.surf.api.spring.ManualListenerRegistration
import dev.slne.surf.api.spring.PluginPresence
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.*
import dev.slne.surf.api.spring.scheduling.registerTaskScheduler
import io.papermc.paper.ServerBuildInfo
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.Bukkit
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.findAnnotationOnBean
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeanNamesForAnnotation
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * surf-api's Spring runtime on Paper: surf-api's two root contexts and the contexts of every surf
 * plugin.
 *
 * The root bootstrap context is built with the runtime, in surf-api's bootstrap phase, and holds
 * surf-api's bootstrap components, the NMS version selected for the server and the
 * [PaperPluginPresence] that `@ConditionalOnPlugin` reads in the root context; the plugins'
 * bootstrap contexts sit below it. The root context with the rest of surf-api's beans is built in
 * surf-api's load phase by [loadRoot]; the plugins' full contexts sit below it.
 *
 * In a plugin's bootstrap phase it builds the plugin's bootstrap context; in the load phase, after
 * Paper created the plugin instance and every plugin's classes are visible, the full context.
 * Lifecycle hooks run blocking on the thread Paper calls the phase on, which is the main thread for
 * load, enable and disable. Each plugin, and surf-api itself, has [PluginDispatchers] whose
 * lifecycle loop runs the work for that thread inside the blocked phase, so a hook can wait for it.
 * Every plugin context and surf-api's root context hold a `CoroutineScope` and a
 * `PaperDispatchers` bean; the plugin's are primary, so its beans get its own.
 *
 * In the enable phase it registers the plugin's Bukkit listeners, commands, event bus listeners and
 * PlaceholderAPI expansions before the plugin's enable hooks run. A plugin whose load phase failed
 * is never enabled: [enable] throws, and Paper disables it. surf-api's own enable phase registers
 * the Bukkit listener beans of the root context in the same way, for surf-api's plugin.
 */
@Suppress("UnstableApiUsage")
internal class PaperSpringRuntime(private val surfClassLoader: ClassLoader) :
    SurfPaperPluginRuntime {

    private val rootBootstrap = SurfRootContext.create(
        classLoader = surfClassLoader,
        coroutineScope = null,
        bootstrap = true,
        configure = { context ->
            val minecraftVersion = ServerBuildInfo.buildInfo().minecraftVersionId()
            context.beanFactory.registerSingleton(
                NmsVersionResolver.BEAN_NAME,
                NmsVersionResolver.resolve(minecraftVersion),
            )
            context.beanFactory.registerSingleton(PluginPresence.BEAN_NAME, PaperPluginPresence)
        },
    )

    private val contexts = SurfContextManager(rootBootstrap.context, PaperPluginPresence)
    private val plugins = ConcurrentHashMap<String, PaperPluginState>()
    private val log = logger()

    @Volatile
    private var root: SurfRootContext? = null

    @Volatile
    private var rootPlugin: Plugin? = null

    private lateinit var rootDispatchers: PluginDispatchers
    private lateinit var rootScope: CoroutineScope

    /**
     * Creates the dispatchers and the coroutine scope of surf-api's own [plugin]; call it before
     * any other root phase.
     */
    fun attachRootPlugin(plugin: Plugin) {
        rootPlugin = plugin
        rootDispatchers = PluginDispatchers(plugin)
        rootScope = surfCoroutineScope(
            ROOT_OWNER,
            surfClassLoader,
            Dispatchers.Default,
            PaperDispatchersElement(rootDispatchers),
        )
    }

    /**
     * Runs one of surf-api's own lifecycle phases blocking on the calling thread, with work for
     * that thread routed into the phase.
     */
    fun <T> runRootPhase(phase: suspend () -> T): T {
        return runBlocking(PaperDispatchersElement(rootDispatchers)) {
            rootDispatchers.lifecycle(phase)
        }
    }

    /**
     * Runs the bootstrap hooks of surf-api's bootstrap components.
     */
    suspend fun bootstrapRoot(): Unit = rootBootstrap.bootstrap()

    /**
     * Builds surf-api's root context, installs it for surf-api's static entry points and runs the
     * load hooks of both root contexts.
     */
    suspend fun loadRoot() {
        val root = SurfRootContext.create(
            classLoader = surfClassLoader,
            coroutineScope = rootScope,
            parent = rootBootstrap,
            configure = { context ->
                context.beanFactory.registerSingleton(SERVER_BEAN, Bukkit.getServer())
                context.registerBeanDefinition(ROOT_PLUGIN_BEAN, rootPluginDefinition())
                registerDispatchers(context, rootDispatchers, primary = false)
            },
        )

        this.root = root
        contexts.attachRoot(root.context)
        SurfContexts.install(root.context)

        rootBootstrap.load()
        root.load()
    }

    /**
     * Lets surf-api's own dispatchers through to Paper, registers the Bukkit listener beans of the
     * root context for surf-api's plugin and runs the enable hooks of both root contexts.
     */
    suspend fun enableRoot() {
        rootDispatchers.enable()
        rootBootstrap.enable()

        val root = root ?: return
        registerListeners(root.context, attachedRootPlugin())
        root.enable()
    }

    override fun bootstrap(bootstrapper: SurfPaperBootstrapper, context: BootstrapContext) {
        val descriptor = describe(bootstrapper, context)
        val instance = PluginInstanceHolder(descriptor.name)

        val bootstrap = contexts.bootstrap(descriptor) { bootstrapContext ->
            bootstrapContext.beanFactory.registerSingleton(BOOTSTRAP_CONTEXT_BEAN, context)
            bootstrapContext.registerBeanDefinition(PLUGIN_PROXY_BEAN, instance.proxyDefinition())
        }

        try {
            runBlocking { bootstrap.bootstrap() }
        } catch (exception: Throwable) {
            contexts.close(descriptor.name)
            throw exception
        }

        plugins[descriptor.name] = PaperPluginState(descriptor, instance)
    }

    override fun load(plugin: SurfPaperPlugin) {
        val state = stateOf(plugin)
        state.instance.set(plugin)
        val dispatchers = PluginDispatchers(plugin)
        state.dispatchers = dispatchers

        try {
            val scope = surfCoroutineScope(
                plugin.name,
                state.descriptor.classLoader,
                Dispatchers.Default,
                PaperDispatchersElement(dispatchers),
            )
            val pluginContext = contexts.start(state.descriptor, scope) { pluginContext ->
                pluginContext.registerBeanDefinition(
                    PLUGIN_INSTANCE_BEAN,
                    state.instance.instanceDefinition(plugin),
                )
                pluginContext.registerFallbackInstance(
                    PLUGIN_LOGGER_BEAN,
                    ComponentLogger::class.java,
                    plugin.componentLogger,
                )
                registerDispatchers(pluginContext, dispatchers, primary = true)
                registerTaskScheduler(
                    pluginContext,
                    PaperSchedulers.GLOBAL_REGION,
                    dispatchers.main,
                )
            }
            runBlocking { dispatchers.lifecycle { pluginContext.load() } }
        } catch (exception: Throwable) {
            dispatchers.close()
            state.loadFailure = exception
            throw exception
        }
    }

    override fun enable(plugin: SurfPaperPlugin) {
        val state = stateOf(plugin)
        state.loadFailure?.let { failure ->
            throw IllegalStateException(
                "Plugin '${plugin.name}' failed to load and cannot be enabled",
                failure,
            )
        }
        val pluginContext = contextOf(plugin)
        val dispatchers = checkNotNull(state.dispatchers) {
            "Plugin '${plugin.name}' has no dispatchers"
        }

        for (context in pluginContext.beanContexts) {
            registerListeners(context, plugin)
        }

        CommandApiCommandBeans.register(pluginContext.beanContexts)
        SurfEventHandlerBeans.register(pluginContext.beanContexts, state.surfEventListeners)
        PapiExpansionBeans.register(
            pluginContext.context,
            plugin,
            checkNotNull(papiHook()) { "surf-api's root context is not loaded" },
            state.papiExpansions,
        )

        runBlocking {
            dispatchers.lifecycle {
                dispatchers.enable()
                pluginContext.enable()
            }
        }
    }

    override fun disable(plugin: SurfPaperPlugin) {
        val dispatchers = plugins[plugin.name]?.dispatchers

        try {
            contexts.find(plugin.name)
                ?.let { pluginContext -> runBlocking { disable(pluginContext, dispatchers) } }
        } finally {
            dispatchers?.close()

            try {
                // Bukkit listeners are unregistered by Paper itself when it disables the plugin.
                val state = plugins[plugin.name]

                if (state != null) {
                    unregisterBeans(state)
                }
            } finally {
                contexts.close(plugin.name)
                plugins.remove(plugin.name)
            }
        }
    }

    /**
     * Closes every plugin context that is still open, after rejecting the work its dispatchers
     * still hold; the plugins Paper disabled already closed theirs.
     *
     * Paper never disables a plugin that did not enable, such as one whose enable came after a
     * failure of surf-api's own or after the server began to stop.
     */
    fun closePlugins() {
        try {
            for (state in plugins.values) {
                runCatching { state.dispatchers?.run { beginDisable(); close() } }
                    .onFailure {
                        log.atSevere()
                            .withCause(it)
                            .log("Failed to close the dispatchers of %s", state.descriptor.name)
                    }
            }
        } finally {
            plugins.clear()
            contexts.closeAll()
        }
    }

    /**
     * Releases the work surf-api's own dispatchers still hold; the first step of surf-api's
     * disable phase, run inside [runRootPhase].
     */
    fun beginRootDisable(): Unit = rootDispatchers.beginDisable()

    /**
     * Runs the disable hooks of both root contexts, closes them and uninstalls the root context.
     */
    suspend fun shutdownRoot() {
        try {
            root?.disable()
            rootBootstrap.disable()
        } finally {
            rootDispatchers.close()
            root?.close()
            rootBootstrap.close()
            root = null
            SurfContexts.uninstall()
        }
    }

    override fun coroutineScope(plugin: Plugin): CoroutineScope {
        return if (plugin === rootPlugin) rootScope else contextOf(plugin).coroutineScope
    }

    override fun dispatchers(plugin: Plugin): PaperDispatchers {
        return if (plugin === rootPlugin) {
            rootDispatchers
        } else {
            checkNotNull(plugins[plugin.name]?.dispatchers) {
                "Plugin '${plugin.name}' does not run on surf-api or has not loaded yet"
            }
        }
    }

    /**
     * Releases the plugin's held work, then runs its disable hooks, with work for the blocked
     * thread routed into the phase.
     */
    private suspend fun disable(pluginContext: PluginContext, dispatchers: PluginDispatchers?) {
        if (dispatchers == null) return pluginContext.disable()

        dispatchers.lifecycle {
            dispatchers.beginDisable()
            pluginContext.disable()
        }
    }

    private fun describe(
        bootstrapper: SurfPaperBootstrapper,
        context: BootstrapContext,
    ): PluginDescriptor {
        val type = bootstrapper.javaClass
        val meta = context.pluginMeta

        return PluginDescriptor(
            name = meta.name,
            classLoader = type.classLoader,
            source = Path.of(type.protectionDomain.codeSource.location.toURI()),
            basePackages = listOf(type.packageName),
            dependencies = meta.pluginDependencies + meta.pluginSoftDependencies,
            aliases = meta.providedPlugins,
            dataDirectory = context.dataDirectory,
        )
    }

    private fun unregisterBeans(state: PaperPluginState) {
        try {
            SurfEventHandlerBeans.unregister(state.surfEventListeners)
        } finally {
            unregisterPapiExpansions(state)
        }
    }

    private fun unregisterPapiExpansions(state: PaperPluginState) {
        val registration = state.papiExpansions

        try {
            val papiHook = papiHook()

            if (papiHook != null) {
                PapiExpansionBeans.unregister(papiHook, registration)
            }
        } finally {
            val beanFactory = contexts.find(state.descriptor.name)?.context?.beanFactory

            if (beanFactory != null) {
                PapiExpansionBeans.destroyCreatedInstances(beanFactory, registration)
            }
        }
    }

    private fun papiHook(): SurfPaperPAPIHookImpl? {
        return root?.context?.getBean<SurfPaperPAPIHookImpl>()
    }

    /**
     * Registers the Bukkit listener beans of [context] itself, not those of its ancestors, for
     * [plugin], except those marked with [ManualListenerRegistration].
     *
     * @throws IllegalStateException if a bean marked with [BukkitListener] is no [Listener]
     */
    private fun registerListeners(context: ListableBeanFactory, plugin: Plugin) {
        for (name in context.getBeanNamesForAnnotation<BukkitListener>()) {
            check(context.isTypeMatch(name, Listener::class.java)) {
                "Bean '$name' is annotated with @BukkitListener but does not implement Listener"
            }
        }

        val names = context.getBeanNamesForType(Listener::class.java).filter { name ->
            context.findAnnotationOnBean<ManualListenerRegistration>(name) == null
        }

        for (name in names) {
            plugin.server.pluginManager.registerEvents(context.getBean<Listener>(name), plugin)
        }
    }

    private fun attachedRootPlugin(): Plugin {
        return checkNotNull(rootPlugin) { "surf-api's plugin is not attached" }
    }

    private fun stateOf(plugin: SurfPaperPlugin): PaperPluginState {
        return checkNotNull(plugins[plugin.name]) {
            "Plugin '${plugin.name}' was not bootstrapped by surf-api. Its bootstrapper must extend " +
                "SurfPaperBootstrapper and must not have failed."
        }
    }

    /**
     * A fallback bean definition for surf-api's own plugin, so that a plugin context's own instance
     * wins whenever it injects [Plugin].
     */
    private fun rootPluginDefinition(): RootBeanDefinition {
        val plugin = attachedRootPlugin()

        return RootBeanDefinition(Plugin::class.java).apply {
            setInstanceSupplier { plugin }
            isFallback = true
            destroyMethodName = ""
        }
    }

    private fun contextOf(plugin: Plugin): PluginContext {
        return checkNotNull(contexts.find(plugin.name)) {
            "Plugin '${plugin.name}' does not run on surf-api or has not loaded yet"
        }
    }

    private companion object {
        const val BOOTSTRAP_CONTEXT_BEAN = "surfBootstrapContext"
        const val PLUGIN_PROXY_BEAN = "surfPlugin"
        const val PLUGIN_INSTANCE_BEAN = "surfPluginInstance"
        const val PLUGIN_LOGGER_BEAN = "surfPluginLogger"
        const val SERVER_BEAN = "surfBukkitServer"
        const val ROOT_PLUGIN_BEAN = "surfApiPlugin"
        const val DISPATCHERS_BEAN = "surfPaperDispatchers"
        const val ROOT_OWNER = "surf-api"

        /**
         * Registers [dispatchers] as the context's [PaperDispatchers] bean; a [primary] one wins
         * over the root context's.
         */
        fun registerDispatchers(
            context: AnnotationConfigApplicationContext,
            dispatchers: PaperDispatchers,
            primary: Boolean,
        ) {
            val definition = RootBeanDefinition(PaperDispatchers::class.java).apply {
                setInstanceSupplier { dispatchers }
                isPrimary = primary
            }
            context.registerBeanDefinition(DISPATCHERS_BEAN, definition)
        }
    }
}

/**
 * What the runtime keeps per plugin between its phases.
 */
private class PaperPluginState(
    val descriptor: PluginDescriptor,
    val instance: PluginInstanceHolder,
) {
    @Volatile
    var loadFailure: Throwable? = null

    @Volatile
    var dispatchers: PluginDispatchers? = null

    /**
     * The event bus listeners registered when the plugin was enabled; touched only on the main
     * thread.
     */
    val surfEventListeners = mutableListOf<Any>()

    /**
     * The PlaceholderAPI expansions registered when the plugin was enabled; touched only on the
     * main thread.
     */
    val papiExpansions = PapiExpansionRegistration()
}
