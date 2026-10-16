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
import dev.slne.surf.api.minestom.server.impl.mixin.SurfMixinAgent
import dev.slne.surf.api.minestom.server.impl.performance.EntityTickFilter
import dev.slne.surf.api.minestom.server.impl.player.PlayerEvents
import dev.slne.surf.api.minestom.server.impl.player.PlayerFlightListener
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.property.ServerProperties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Lifecycle of the surf api on a Minestom server; at most one runtime exists per JVM.
 *
 * [start] runs the load phase synchronously: it installs the surf listeners and services and
 * calls `load` on each feature in `loadPriority` order. Features are enabled on the next tick, and
 * a feature whose `enable` throws is logged and skipped. [shutdown] is idempotent and calls
 * `disable` on every loaded feature in reverse order, including ones that failed to enable.
 *
 * A failing [start] rolls back what it installed with the same teardown as [shutdown] and rethrows
 * the failure. A teardown step that throws is logged and does not skip the following steps.
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

    private val running = AtomicBoolean(false)

    private var packetEventsPrepared = false

    override fun feature(id: String): SurfMinestomFeature? = builder.features[id]

    private fun load() {
        if (builder.recommendedServerProperties) RecommendedServerProperties.apply()
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
        EntityTickFilter.configure(builder.disabledEntityTicks)
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

        // Installed after the features, so the server's provider takes precedence over one a feature
        // installed
        builder.permissionProvider?.let { provider -> MinestomPermissions.installProvider(provider) }
        running.set(true)

        SchedulerManager.scheduleNextTick(::enable)
        SchedulerManager.buildShutdownTask(::shutdown)
    }

    private fun enable() {
        if (!running.get()) return

        try {
            runBlocking { instance.onEnable() }
        } catch (failure: Throwable) {
            LOGGER.error("Failed to enable the core listeners and the inventory framework", failure)
        }
        reportFailedMixins()

        for (feature in features) {
            try {
                feature.enable(this)
            } catch (failure: Throwable) {
                LOGGER.error("Failed to enable feature {}", feature.id, failure)
            }
        }
    }

    override fun shutdown() {
        if (!running.compareAndSet(true, false)) return
        teardown()
    }

    /**
     * Undoes everything [load] installed and releases [current].
     *
     * Every step is a no-op for an install step that never ran, so this also rolls back a partial
     * [load]. A failing step is logged and does not skip the following ones.
     *
     * @return the first step failure with the later ones attached as suppressed exceptions, or
     * `null` when every step succeeded
     */
    private fun teardown(): Throwable? {
        running.set(false)

        var failure: Throwable? = null
        fun step(name: String, action: () -> Unit) {
            try {
                action()
            } catch (stepFailure: Throwable) {
                LOGGER.error("Failed to {} while shutting down the surf api", name, stepFailure)
                val first = failure
                if (first == null) failure = stepFailure else first.addSuppressed(stepFailure)
            }
        }

        try {
            val toDisable = loaded.toList()
            loaded.clear()
            for (feature in toDisable.asReversed()) {
                step("disable feature ${feature.id}") { feature.disable(this) }
            }

            if (builder.configurationPhase != null) {
                step("uninstall the configuration phase") { ConfigurationPhase.uninstall() }
            }
            step("stop the command api") { commandApi.stop() }
            step("uninstall the flight listener") { PlayerFlightListener.uninstall() }
            step("reset the entity tick filter") { EntityTickFilter.configure(emptySet()) }
            step("uninstall the player events") { PlayerEvents.uninstall() }
            step("remove the event node") { GlobalEventHandler.removeChild(eventNode) }

            if (packetEventsPrepared) {
                step("disable the core instance") { runBlocking { instance.onDisable() } }
                step("terminate packetevents") { packetEvents.terminate() }
            }

            step("reset the permission provider") { MinestomPermissions.installProvider(null) }
            step("reset the unsigned argument factory") { SignedCommandArguments.resetUnsignedFactory() }
        } finally {
            current.compareAndSet(this, null)
        }
        return failure
    }

    private fun preparePacketEvents() {
        PacketEvents.setAPI(NoopPacketEvents())
        packetEventsPrepared = true
        packetEvents.load()
        packetEvents.init()
    }

    private fun reportFailedMixins() {
        if (!SurfMixinAgent.isInstalled()) return

        val failed = SurfMixinAgent.failedTransformations()
        if (failed.isNotEmpty()) {
            LOGGER.error("Mixins failed to apply to {}, the surf api does not work correctly", failed)
        }
    }

    companion object {
        private val LOGGER = ComponentLogger.logger()

        /**
         * Fails startup when Mixin is not installed or a class failed to transform, since the
         * CommandAPI, the configuration phase and the performance fixes depend on the mixins.
         * Only tests ([ServerProperties.INSIDE_TEST]) may run without Mixin. Detects transformation
         * failures that happened before the call.
         */
        private fun checkMixins() {
            if (!SurfMixinAgent.isInstalled()) {
                check(ServerProperties.INSIDE_TEST.get()) {
                    "Mixin is not installed; start the server from the jar built by the surf Gradle plugin"
                }
                LOGGER.warn("Mixin is not installed, the surf mixins do not apply")
                return
            }

            val failed = SurfMixinAgent.failedTransformations()
            check(failed.isEmpty()) { "Mixins failed to apply to $failed, see the log above" }
        }

        private val current = AtomicReference<SurfMinestomRuntime?>()

        fun current(): SurfMinestomServer = checkNotNull(current.get()) {
            "The surf api has not been started yet, call surfMinestomServer { } first"
        }

        fun start(builder: SurfMinestomServerBuilder): SurfMinestomServer {
            checkNotNull(MinecraftServer.process()) {
                "The surf api needs an initialized server, call MinecraftServer.init() first"
            }

            checkMixins()

            val runtime = SurfMinestomRuntime(builder)
            check(current.compareAndSet(null, runtime)) { "The surf api is already running" }

            try {
                runtime.load()
            } catch (failure: Throwable) {
                runtime.teardown()?.let(failure::addSuppressed)
                current.compareAndSet(runtime, null)
                throw failure
            }

            return runtime
        }
    }
}
