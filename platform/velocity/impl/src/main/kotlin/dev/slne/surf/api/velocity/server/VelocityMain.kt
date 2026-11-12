@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.velocity.server

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.api.core.server.CoreInstance
import dev.slne.surf.api.di.boot.SurfContainerAccess
import dev.slne.surf.api.di.boot.SurfContainerHost
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.velocity.api.metrics.Metrics
import dev.slne.surf.api.velocity.server.di.VelocityContainerPlatform
import dev.slne.surf.api.velocity.server.generated.BuildConfig
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
    dependencies = [Dependency(id = "packetevents"), Dependency(id = "commandapi"), Dependency(id = "luckperms")]
)
class VelocityMain @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    val pluginContainer: PluginContainer,
    @param:DataDirectory val dataDirectory: Path,
    val executorService: ExecutorService,
    private val metricsFactory: Metrics.Factory
) : CoreInstance() {

    lateinit var bstats: Metrics

    /**
     * surf-api's container hierarchy for this proxy.
     *
     * The root holds surf-api's own beans; a plugin that wants one gets a child below it. There is
     * one host per process, and this is it.
     */
    val containers = SurfContainerHost(VelocityContainerPlatform(server, dataDirectory))

    init {
        instance = this

        SuspendingEventHandler(server.eventManager).register()

        runBlocking {
            // Before anything else in surf-api. A bean is resolved where it is first used rather
            // than where it is declared, so the container has to answer from the first class that
            // asks, and `install` is what makes it answerable through `SurfContainer`.
            containers.startRoot(this@VelocityMain)
            SurfContainerAccess.install(containers)

            bootstrap()
            onLoad()
        }
    }

    @Subscribe
    suspend fun onProxyInitialization(unused: ProxyInitializeEvent) {
        bstats = metricsFactory.make(this, 29466)

        // The proxy's own enable phase, and so the container's: a bean's `enable` is the first
        // point at which the rest of the proxy is there to talk to.
        containers.enable(this)

        onEnable()
    }

    @Subscribe
    suspend fun onProxyShutdown(unused: ProxyShutdownEvent) {
        if (::bstats.isInitialized) {
            bstats.shutdown()
        }

        onDisable()

        // Last, and children before the root: a plugin's bean may hold one of surf-api's, so the
        // reverse order would close a connection while something still writes to it.
        containers.stopAll()
        SurfContainerAccess.uninstall(containers)
    }

    companion object {
        lateinit var instance: VelocityMain
    }
}

val plugin get() = VelocityMain.instance