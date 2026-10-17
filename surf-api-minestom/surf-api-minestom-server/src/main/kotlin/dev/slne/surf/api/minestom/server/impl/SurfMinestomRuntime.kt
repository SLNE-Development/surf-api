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
import dev.slne.surf.api.minestom.server.impl.player.SurfPlayerProviders
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.property.ServerProperties
import net.minestom.server.thread.TickSchedulerThread
import net.minestom.server.thread.TickThread
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Lifecycle of the surf api on a Minestom server; at most one runtime exists per JVM.
 *
 * [start] loads and enables synchronously: it installs the surf listeners and services, calls
 * `load` on each feature in `loadPriority` order and then `enable` in the same order, so every
 * feature and plugin is ready before the server accepts players. A failing [start] rolls back what
 * it installed with the same teardown as [shutdown] and rethrows the failure.
 *
 * [shutdown] is idempotent and calls `disable` on every loaded feature in reverse order. A teardown
 * step that throws is logged and does not skip the following steps. Startup and teardown hold
 * [lifecycleLock]: a shutdown requested by another thread while the api starts waits for the
 * startup, which then fails and rolls back. A shutdown from the starting thread itself, e.g. from a
 * feature's `enable`, is refused, since the startup would continue on a torn down api.
 *
 * [stop] stops the whole server once and runs the registered shutdown actions afterwards.
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

    private val lifecycleLock = ReentrantLock()

    private val shutdownRequested = AtomicBoolean(false)

    @Volatile
    private var startingThread: Thread? = null

    private val stopping = AtomicBoolean(false)

    /**
     * Completed once [stop] has finished, whether or not a step failed.
     */
    private val stopped = CompletableFuture<Unit>()

    @Volatile
    private var stoppingThread: Thread? = null

    override fun feature(id: String): SurfMinestomFeature? = builder.features[id]

    private fun load() {
        SurfPlayerProviders.install(builder.playerFactory)

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

        SchedulerManager.buildShutdownTask(::shutdown)

        enable()
    }

    private fun enable() {
        runBlocking { instance.onEnable() }
        reportFailedMixins()

        for (feature in features) {
            LOGGER.info("Enabling feature {}", feature.id)
            feature.enable(this)
        }
    }

    override fun shutdown() {
        checkNotStarting()
        shutdownRequested.set(true)

        lifecycleLock.withLock {
            if (!running.compareAndSet(true, false)) return
            teardown()
        }
    }

    override fun stop() {
        val thread = Thread.currentThread()
        check(thread !is TickThread && thread !is TickSchedulerThread) {
            "The server cannot be stopped from the tick thread ${thread.name}, which stop() would " +
                    "block until the plugins and Minestom are stopped; call stop() from another thread"
        }
        checkNotStarting()

        if (!stopping.compareAndSet(false, true)) {
            if (Thread.currentThread() !== stoppingThread) stopped.join()
            return
        }

        stoppingThread = Thread.currentThread()
        try {
            // Minestom runs the shutdown of the api as one of its shutdown tasks, before it closes the
            // connections
            try {
                if (MinecraftServer.isStarted()) MinecraftServer.stopCleanly()
            } catch (failure: Throwable) {
                LOGGER.error("Failed to stop the Minestom server cleanly", failure)
            }

            // Covers a server that never started, and a Minestom stop whose earlier shutdown tasks
            // threw before they reached the api
            shutdown()

            for (action in builder.shutdownActions) {
                try {
                    action()
                } catch (failure: Throwable) {
                    LOGGER.error("A shutdown action failed", failure)
                }
            }
        } finally {
            stopped.complete(Unit)
        }
    }

    private fun checkNotStarting() {
        check(Thread.currentThread() !== startingThread) {
            "The surf api cannot be shut down while it starts; throw from load or enable to fail the start instead"
        }
    }

    /**
     * Stops the server when the JVM shuts down, so that the plugins, the features and the shutdown
     * actions also stop on a signal. Installed once the start succeeded; tests do not get one.
     *
     * The hook stays registered after [shutdown]: [stop] then still runs the shutdown actions.
     */
    private fun installShutdownHook() {
        if (ServerProperties.INSIDE_TEST.get()) return

        if (ServerProperties.SHUTDOWN_ON_SIGNAL.get()) {
            LOGGER.warn(
                "Minestom stops the server from a shutdown hook of its own, which runs concurrently " +
                        "with the one of the surf api; start the server from the jar built by the surf " +
                        "Gradle plugin, or set -Dminestom.shutdown-on-signal=false"
            )
        }

        Runtime.getRuntime().addShutdownHook(Thread(::stop, "surf-minestom-shutdown"))
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
            step("reset the player provider") { SurfPlayerProviders.uninstall() }
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

            runtime.lifecycleLock.withLock {
                runtime.startingThread = Thread.currentThread()
                try {
                    runtime.load()

                    // Requested by another thread, which waits for the lock and then finds the api
                    // torn down
                    check(!runtime.shutdownRequested.get()) { "The surf api was shut down while it started" }
                } catch (failure: Throwable) {
                    runtime.teardown()?.let(failure::addSuppressed)
                    current.compareAndSet(runtime, null)
                    throw failure
                } finally {
                    runtime.startingThread = null
                }
            }

            runtime.installShutdownHook()
            return runtime
        }
    }
}
