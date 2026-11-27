package dev.slne.surf.api.velocity.server

import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.api.core.server.CoreInstance
import dev.slne.surf.api.nats.NatsPlatform
import dev.slne.surf.api.redis.RedisPlatform
import dev.slne.surf.api.spring.SurfContexts
import dev.slne.surf.api.spring.context.SurfRootContext
import dev.slne.surf.api.spring.context.surfCoroutineScope
import dev.slne.surf.api.velocity.api.metrics.Metrics
import dev.slne.surf.api.velocity.plugin.SurfVelocityPluginRuntime
import dev.slne.surf.api.velocity.server.generated.BuildConfig
import dev.slne.surf.api.velocity.server.spring.VelocitySpringRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.ExecutorService

@Plugin(
    id = "surf-api-velocity",
    name = "Surf API Velocity",
    version = BuildConfig.VERSION,
    description = "The Surf API Velocity plugin.",
    authors = ["twisti", "SLNE Development Team"],
    dependencies = [
        Dependency(id = "packetevents"),
        Dependency(id = "commandapi"),
        Dependency(id = "luckperms"),
    ],
)
class VelocityMain @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    val pluginContainer: PluginContainer,
    @param:DataDirectory val dataDirectory: Path,
    val executorService: ExecutorService,
    private val metricsFactory: Metrics.Factory,
) : CoreInstance() {

    lateinit var bstats: Metrics

    /**
     * surf-api's root Spring context on this proxy, holding surf-api's own beans.
     */
    var root: SurfRootContext
        private set

    private var springRuntime: VelocitySpringRuntime

    init {
        instance = this

        SuspendingEventHandler(server.eventManager).register()

        NatsPlatform.install(dataDirectory)
        RedisPlatform.install(
            dataDirectory,
            VelocityPluginNames { server.pluginManager.plugins }::nameOf,
        )

        runBlocking {
            val classLoader = VelocityMain::class.java.classLoader
            root = SurfRootContext.create(
                classLoader,
                surfCoroutineScope("surf-api", classLoader, Dispatchers.Default),
            )
            SurfContexts.install(root.context)
            root.load()

            springRuntime = VelocitySpringRuntime(server, root.context, dataDirectory.parent)
            SurfVelocityPluginRuntime.install(springRuntime)

            bootstrap()
            onLoad()
        }
    }

    /**
     * Enables surf-api, then the surf plugins in the order Velocity constructed them.
     *
     * Runs before every other handler of the event, including the [PostOrder.FIRST] ones, so that a
     * plugin's own `ProxyInitializeEvent` handler finds its beans enabled. Velocity fires the event
     * after every plugin is constructed, so every surf plugin has loaded by now.
     */
    @Subscribe(priority = Short.MAX_VALUE)
    suspend fun onProxyInitialization(unused: ProxyInitializeEvent) {
        springRuntime.registerRootListeners(this)
        root.enable()

        onEnable()

        springRuntime.enablePlugins()

        runCatching { bstats = metricsFactory.make(this, BSTATS_PLUGIN_ID) }
            .onFailure { logger.warn("Failed to start bStats", it) }
    }

    /**
     * Disables the surf plugins in reverse order, then surf-api.
     *
     * Runs after every other handler of the event, including the [PostOrder.LAST] ones, so that a
     * plugin's own `ProxyShutdownEvent` handler still finds its beans. The root context closes even
     * when an earlier step fails.
     */
    @Subscribe(priority = Short.MIN_VALUE)
    suspend fun onProxyShutdown(unused: ProxyShutdownEvent) {
        if (::bstats.isInitialized) {
            runCatching { bstats.shutdown() }.onFailure { logger.warn("Failed to stop bStats", it) }
        }

        try {
            try {
                springRuntime.disablePlugins()
            } finally {
                springRuntime.closePlugins()
                SurfVelocityPluginRuntime.uninstall()
            }

            onDisable()
        } finally {
            closeRoot()
        }
    }

    /**
     * Disables and closes the root context, then uninstalls it; each step runs even when the one
     * before it fails.
     */
    private suspend fun closeRoot() {
        try {
            root.disable()
        } finally {
            try {
                root.close()
            } finally {
                SurfContexts.uninstall()
            }
        }
    }

    companion object {
        private const val BSTATS_PLUGIN_ID = 29466

        lateinit var instance: VelocityMain
    }
}

val plugin get() = VelocityMain.instance