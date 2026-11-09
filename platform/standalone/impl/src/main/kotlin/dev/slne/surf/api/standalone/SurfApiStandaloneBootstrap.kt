package dev.slne.surf.api.standalone

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.core.server.packet.NoopPacketEvents
import dev.slne.surf.api.di.boot.SurfContainerAccess
import dev.slne.surf.api.di.boot.SurfContainerHost
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.standalone.impl.SurfStandaloneInstance
import dev.slne.surf.api.standalone.impl.di.StandaloneContainerPlatform
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

@OptIn(InternalSurfApi::class)
object SurfApiStandaloneBootstrap {
    private val shutdown = AtomicBoolean(false)

    /**
     * surf-api's container for this process.
     *
     * One root and no children: a standalone process has a single classloader, so the embedding
     * application's beans and surf-api's are on one classpath and belong in one container.
     */
    private val containers = SurfContainerHost(StandaloneContainerPlatform())

    suspend fun bootstrap() {
        // [shutdown] latches so that an explicit call and the shutdown hook do not tear the process
        // down twice. Clearing it here is what makes the pair symmetric: without it a process that
        // shuts down and starts again can never shut down a second time, and would leave a whole
        // container running with nothing holding it.
        shutdown.set(false)

        preparePacketEvents()

        // Before anything else in surf-api, because a bean is resolved where it is first used
        // rather than where it is declared, and `install` is what makes it resolvable at all.
        containers.startRoot(SurfStandaloneInstance)
        SurfContainerAccess.install(containers)

        SurfStandaloneInstance.bootstrap()
    }

    @OptIn(ExperimentalVersionOverloading::class)
    suspend fun enable(@IntroducedAt("3.52.0") registerShutdownHook: Boolean = true) {
        SurfStandaloneInstance.onLoad()
        SurfStandaloneInstance.onEnable()

        if (registerShutdownHook) {
            Runtime.getRuntime()
                .addShutdownHook(thread(start = false) { runBlocking { shutdown() } })
        }
    }

    suspend fun shutdown() {
        if (shutdown.getAndSet(true)) {
            return
        }

        SurfStandaloneInstance.onDisable()

        // Last, so a bean that holds a connection is still answering while everything that might
        // write to it shuts down.
        containers.stopAll()
        SurfContainerAccess.uninstall(containers)

        destroyPacketEvents()
    }

    private fun preparePacketEvents() {
        PacketEvents.setAPI(NoopPacketEvents())
        packetEvents.load()
        packetEvents.init()
    }

    private fun destroyPacketEvents() {
        packetEvents.terminate()
    }
}