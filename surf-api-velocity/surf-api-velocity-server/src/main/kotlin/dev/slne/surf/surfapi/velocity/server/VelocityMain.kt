package dev.slne.surf.surfapi.velocity.server

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.surfapi.core.server.CoreInstance
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.ExecutorService

@Plugin(
    id = "surf-api-velocity",
    name = "Surf API Velocity",
    version = "2.0.0",
    description = "The Surf API Velocity plugin.",
    authors = ["twisti"],
    dependencies = [Dependency(id = "packetevents"), Dependency(id = "commandapi")]
)
class VelocityMain @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    val pluginContainer: PluginContainer,
    @param:DataDirectory val dataDirectory: Path,
    val executorService: ExecutorService,
) : CoreInstance() {

    init {
        instance = this

        SuspendingEventHandler(server.eventManager).register()

        runBlocking {
            bootstrap()
            onLoad()
        }
    }

    @Subscribe
    suspend fun onProxyInitialization(unused: ProxyInitializeEvent) {
        onEnable()
    }

    @Subscribe
    suspend fun onProxyShutdown(unused: ProxyShutdownEvent) {
        onDisable()
    }

    companion object {
        lateinit var instance: VelocityMain
    }
}

val plugin get() = VelocityMain.instance