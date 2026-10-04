package dev.slne.surf.api.minestom.server.impl

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.core.server.packet.NoopPacketEvents
import dev.slne.surf.api.minestom.extension.GlobalEventHandler
import dev.slne.surf.api.minestom.extension.SchedulerManager
import dev.slne.surf.api.minestom.permission.MinestomPermissions
import dev.slne.surf.api.minestom.player.PlayerLimit
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.impl.command.MinestomCommandAPIService
import dev.slne.surf.api.minestom.server.impl.command.SignedCommandArguments
import dev.slne.surf.api.minestom.server.impl.configuration.ConfigurationPhase
import dev.slne.surf.api.minestom.server.impl.dialog.DialogCallbackListener
import dev.slne.surf.api.minestom.server.impl.inventory.framework.MinestomInventoryLoader
import dev.slne.surf.api.minestom.server.impl.player.PlayerEvents
import dev.slne.surf.api.minestom.server.impl.player.PlayerFlightListener
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import java.util.concurrent.atomic.AtomicReference

/**
 * Brings the surf api up and down on a Minestom server.
 */
internal class SurfMinestomRuntime private constructor(
    private val builder: SurfMinestomServerBuilder,
) : SurfMinestomServer {
    override val eventNode: EventNode<Event> = EventNode.all(builder.eventNodeName)
    override val features: Collection<SurfMinestomFeature> =
        builder.features.values.sortedBy(SurfMinestomFeature::loadPriority)

    private val inventoryLoader = MinestomInventoryLoader()
    private val instance = SurfMinestomInstance(inventoryLoader)
    private val commandApi = MinestomCommandAPIService()

    private val loaded = mutableListOf<SurfMinestomFeature>()
    private val enabled = mutableListOf<SurfMinestomFeature>()

    @Volatile
    private var running = false

    override fun feature(id: String): SurfMinestomFeature? = builder.features[id]

    private fun load() {
        preparePacketEvents()
        runBlocking {
            instance.bootstrap()
            instance.onLoad()
        }

        GlobalEventHandler.addChild(eventNode)

        inventoryLoader.register(eventNode)
        DialogCallbackListener.register(eventNode)
        PlayerFlightListener.install()
        PlayerLimit.maxPlayers = builder.maxPlayers
        PlayerEvents.install(eventNode, configurationPhase = builder.configurationPhase != null)
        commandApi.start(eventNode)

        builder.configurationPhase?.let { phase ->
            ConfigurationPhase.install(
                phase.build(),
                eventNode
            )
        }
        builder.registrars.forEach { registrar -> registrar.register(eventNode) }

        for (feature in features) {
            LOGGER.info("Loading feature {}", feature.id)
            feature.load(this)
            loaded += feature
        }

        // Set after the features, so that it takes precedence over one a feature installed
        builder.permissionProvider?.let { provider -> MinestomPermissions.installProvider(provider) }
        running = true

        SchedulerManager.scheduleNextTick(::enable)
        SchedulerManager.buildShutdownTask(::shutdown)
    }

    private fun enable() {
        if (!running) return

        runBlocking { instance.onEnable() }

        for (feature in features) {
            try {
                feature.enable(this)
                enabled += feature
            } catch (failure: Throwable) {
                LOGGER.error("Failed to enable feature {}", feature.id, failure)
            }
        }
    }

    override fun shutdown() {
        if (!running) return
        running = false

        for (feature in loaded.asReversed()) {
            try {
                feature.disable(this)
            } catch (failure: Throwable) {
                LOGGER.error("Failed to disable feature {}", feature.id, failure)
            }
        }
        loaded.clear()
        enabled.clear()

        if (builder.configurationPhase != null) ConfigurationPhase.uninstall()
        commandApi.stop()
        PlayerFlightListener.uninstall()
        PlayerEvents.uninstall()
        GlobalEventHandler.removeChild(eventNode)

        runBlocking { instance.onDisable() }
        packetEvents.terminate()

        MinestomPermissions.installProvider(null)
        SignedCommandArguments.resetUnsignedFactory()

        current.compareAndSet(this, null)
    }

    private fun preparePacketEvents() {
        PacketEvents.setAPI(NoopPacketEvents())
        packetEvents.load()
        packetEvents.init()
    }

    companion object {
        private val LOGGER = ComponentLogger.logger()
        private val current = AtomicReference<SurfMinestomRuntime?>()

        fun current(): SurfMinestomServer = checkNotNull(current.get()) {
            "The surf api has not been started yet, call surfMinestomServer { } first"
        }

        fun start(builder: SurfMinestomServerBuilder): SurfMinestomServer {
            checkNotNull(MinecraftServer.process()) {
                "The surf api needs an initialized server, call MinecraftServer.init() first"
            }

            val runtime = SurfMinestomRuntime(builder)
            check(current.compareAndSet(null, runtime)) { "The surf api is already running" }

            try {
                runtime.load()
            } catch (failure: Throwable) {
                current.compareAndSet(runtime, null)
                throw failure
            }

            return runtime
        }
    }
}
