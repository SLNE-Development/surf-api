package dev.slne.surf.api.velocity.server.spring

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.core.server.spring.CommandApiCommandBeans
import dev.slne.surf.api.core.server.spring.SurfEventHandlerBeans
import dev.slne.surf.api.core.server.spring.beansDeclaringMethodsAnnotated
import dev.slne.surf.api.spring.PluginPresence
import dev.slne.surf.api.spring.context.*
import dev.slne.surf.api.velocity.plugin.SurfVelocityPlugin
import dev.slne.surf.api.velocity.plugin.SurfVelocityPluginRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.ConfigurableApplicationContext
import java.nio.file.Path
import java.util.*

/**
 * surf-api's Spring runtime for the plugins of a Velocity proxy.
 *
 * A plugin's context is built and its load hooks run when Guice injects the members of the
 * plugin's main class, through [load]. Velocity creates plugins one after another in dependency
 * order, so the contexts of a plugin's dependencies exist by then. Velocity has no phase of its
 * own for enabling and disabling plugins, so surf-api's plugin drives them from its
 * `ProxyInitializeEvent` and `ProxyShutdownEvent` handlers: [enablePlugins] enables the plugins in
 * the order they loaded, [disablePlugins] disables them in reverse, and [closePlugins] closes
 * whatever is left.
 *
 * Enabling a plugin registers its listener beans, commands and event bus listeners before its
 * enable hooks run. A plugin that fails to enable is disabled and closed again; the others
 * continue. A plugin whose creation failed after its context loaded is closed without being
 * enabled.
 *
 * Listener beans are registered while Velocity fires `ProxyInitializeEvent`, so their handlers for
 * that event never run; beans use [dev.slne.surf.api.spring.SurfLifecycle.onEnable] instead.
 *
 * Velocity has no main thread, so the load hooks block the thread that creates the plugin.
 *
 * @param pluginsDirectory the directory Velocity loads plugins from; a plugin's data folder is the
 * folder named after its id in it
 */
internal class VelocitySpringRuntime(
    private val server: ProxyServer,
    rootContext: ConfigurableApplicationContext,
    private val pluginsDirectory: Path,
) : SurfVelocityPluginRuntime {

    private val log = logger()
    private val contexts = SurfContextManager(
        bootstrapRoot = rootContext,
        presence = PluginPresence(server.pluginManager::isLoaded),
    )
    private val loaded = LinkedHashMap<String, VelocityPluginState>()

    override fun load(plugin: SurfVelocityPlugin, container: PluginContainer) {
        val descriptor = describe(plugin, container)

        val coroutineScope = surfCoroutineScope(
            descriptor.name,
            descriptor.classLoader,
            Dispatchers.Default,
        )
        val pluginContext = contexts.start(descriptor, coroutineScope) { context ->
            context.beanFactory.registerSingleton(PROXY_BEAN, server)
            context.beanFactory.registerSingleton(PLUGIN_CONTAINER_BEAN, container)
            context.registerFallbackInstance(
                PLUGIN_LOGGER_BEAN,
                ComponentLogger::class.java,
                ComponentLogger.logger(descriptor.name),
            )
            context.registerBeanDefinition(PLUGIN_INSTANCE_BEAN, instanceDefinition(plugin))
        }

        try {
            runBlocking { pluginContext.load() }
        } catch (exception: Throwable) {
            contexts.close(descriptor.name)
            throw exception
        }

        synchronized(loaded) {
            loaded[descriptor.name] = VelocityPluginState(plugin, pluginContext)
        }
    }

    /**
     * Enables every loaded plugin, in the order the plugins were constructed.
     */
    suspend fun enablePlugins() {
        for (state in snapshot()) {
            if (!isRegistered(state)) {
                log.atSevere()
                    .log("Plugin '%s' failed to construct and is not enabled", state.name)
                discard(state)
                continue
            }

            try {
                enable(state)
            } catch (exception: Throwable) {
                log.atSevere()
                    .withCause(exception)
                    .log("Plugin '%s' failed to enable and is disabled", state.name)
                disableLogging(state)
            }
        }
    }

    /**
     * Disables every loaded plugin and closes its contexts, in the reverse order of
     * [enablePlugins].
     */
    suspend fun disablePlugins() {
        for (context in snapshot().asReversed()) {
            disableLogging(context)
        }
    }

    /**
     * Closes every plugin context that is still open.
     */
    fun closePlugins() {
        synchronized(loaded) { loaded.clear() }
        contexts.closeAll()
    }

    private suspend fun enable(state: VelocityPluginState) {
        val beanContexts = state.pluginContext.beanContexts

        state.velocityListeners = velocityListeners(beanContexts, state.plugin)

        for (velocityListener in state.velocityListeners) {
            server.eventManager.register(state.plugin, velocityListener)
        }

        CommandApiCommandBeans.register(beanContexts)
        SurfEventHandlerBeans.register(beanContexts, state.surfEventListeners)

        state.pluginContext.enable()
    }

    private suspend fun disableLogging(state: VelocityPluginState) {
        try {
            disable(state)
        } catch (exception: Throwable) {
            log.atSevere()
                .withCause(exception)
                .log("Plugin '%s' failed to disable", state.name)
        }
    }

    private suspend fun disable(state: VelocityPluginState) {
        val removed = synchronized(loaded) { loaded.remove(state.name, state) }
        if (!removed) return

        try {
            state.pluginContext.disable()
        } finally {
            unregisterListeners(state)
            contexts.close(state.name)
        }
    }

    private fun unregisterListeners(state: VelocityPluginState) {
        runCatching { SurfEventHandlerBeans.unregister(state.surfEventListeners) }
            .onFailure {
                log.atSevere()
                    .withCause(it)
                    .log("Failed to unregister the event bus listeners of '%s'", state.name)
            }

        for (listener in state.velocityListeners) {
            runCatching { server.eventManager.unregisterListener(state.plugin, listener) }
                .onFailure {
                    log.atSevere()
                        .withCause(it)
                        .log("Failed to unregister a listener of '%s'", state.name)
                }
        }
    }

    /**
     * Whether Velocity registered the plugin, which it only does once Guice finished creating it;
     * the context is loaded while Guice injects the plugin's members, before the injection of a
     * subclass member can still fail.
     */
    private fun isRegistered(state: VelocityPluginState): Boolean {
        return server.pluginManager.fromInstance(state.plugin).isPresent
    }

    /**
     * Closes the contexts of a plugin that never enabled, without running its disable hooks.
     */
    private fun discard(state: VelocityPluginState) {
        val removed = synchronized(loaded) { loaded.remove(state.name, state) }

        if (removed) {
            contexts.close(state.name)
        }
    }

    private fun snapshot(): List<VelocityPluginState> {
        return synchronized(loaded) { loaded.values.toList() }
    }

    private fun describe(plugin: SurfVelocityPlugin, container: PluginContainer): PluginDescriptor {
        val type = plugin.javaClass
        val description = container.description

        return PluginDescriptor(
            name = description.id,
            classLoader = type.classLoader,
            source = Path.of(type.protectionDomain.codeSource.location.toURI()),
            basePackages = listOf(type.packageName),
            dependencies = description.dependencies.map { it.id },
            aliases = description.providedIds.toList(),
            dataDirectory = pluginsDirectory.resolve(description.id),
        )
    }

    /**
     * A primary bean definition for [plugin] under its concrete type.
     *
     * Velocity creates the instance, but the context manages it like any other singleton: it
     * injects `@Autowired` members and runs the `Aware` callbacks, `@PostConstruct` and every
     * `BeanPostProcessor` on it, and calls `@PreDestroy` and `DisposableBean.destroy` when it
     * closes. Only the inferred `close()` or `shutdown()` call is suppressed.
     */
    private fun instanceDefinition(plugin: SurfVelocityPlugin): RootBeanDefinition {
        return RootBeanDefinition(plugin.javaClass).apply {
            setInstanceSupplier { plugin }
            isPrimary = true
            destroyMethodName = ""
        }
    }

    private companion object {
        const val PROXY_BEAN = "surfProxyServer"
        const val PLUGIN_CONTAINER_BEAN = "surfPluginContainer"
        const val PLUGIN_INSTANCE_BEAN = "surfPluginInstance"
        const val PLUGIN_LOGGER_BEAN = "surfPluginLogger"
    }
}

/**
 * What the runtime keeps per loaded plugin.
 */
private class VelocityPluginState(
    val plugin: SurfVelocityPlugin,
    val pluginContext: PluginContext,
) {
    val name: String get() = pluginContext.descriptor.name

    @Volatile
    var velocityListeners: List<Any> = emptyList()

    /**
     * The event bus listeners registered when the plugin was enabled.
     */
    val surfEventListeners: MutableList<Any> = Collections.synchronizedList(mutableListOf())
}

/**
 * The singleton beans of [contexts] with `@Subscribe` methods, except [plugin], which Velocity
 * registers as a listener on its own and refuses to register a second time.
 *
 * Only the beans defined in each context itself are considered, never those of its ancestors.
 */
private fun velocityListeners(
    contexts: List<ConfigurableApplicationContext>,
    plugin: Any,
): List<Any> {
    return beansDeclaringMethodsAnnotated(contexts, Subscribe::class.java).filter { it !== plugin }
}
