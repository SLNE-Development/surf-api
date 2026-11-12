package dev.slne.surf.api.minestom.server

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.permission.PermissionProvider
import dev.slne.surf.api.minestom.player.SurfMinestomPlayer
import dev.slne.surf.api.minestom.server.configuration.ConfigurationPhaseBuilder
import dev.slne.surf.api.minestom.server.impl.SurfMinestomRuntime
import net.minestom.server.entity.EntityType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection

@DslMarker
annotation class SurfMinestomServerDsl

/**
 * An optional part of the surf api, installed through [SurfMinestomServerBuilder.install].
 *
 * Feature modules ship their feature together with an extension on [SurfMinestomServerBuilder], e.g.
 * `withSignedChat()` or `withLuckPerms()`, so that adding the module as a dependency is enough to
 * offer the feature.
 *
 * Features are loaded ordered by [loadPriority], ties in installation order, then enabled in the
 * same order, and disabled in reverse. A feature that throws in [load] or [enable] fails
 * [surfMinestomServer], which disables the features loaded so far; a feature that throws in
 * [disable] is logged and the other features are still disabled.
 */
interface SurfMinestomFeature {

    /**
     * Identifies the feature; a feature can be installed only once.
     */
    val id: String

    /**
     * Features with a higher priority are loaded later and disabled earlier.
     */
    val loadPriority: Int get() = 0

    /**
     * Called while [surfMinestomServer] runs, before the server starts. Register listeners, packet
     * listeners and commands here.
     */
    fun load(api: SurfMinestomServer) = Unit

    /**
     * Called once every feature is loaded, while [surfMinestomServer] still runs, so before the
     * server accepts players.
     *
     * The server does not tick yet: work scheduled for a tick runs once it is started, and must not
     * be waited for here.
     */
    fun enable(api: SurfMinestomServer) = Unit

    /**
     * Called when the server shuts down or [SurfMinestomServer.shutdown] is called, for every
     * loaded feature, including one whose [enable] failed.
     *
     * When the server stops, Minestom no longer ticks: work scheduled for a tick does not run and
     * must not be waited for here.
     */
    fun disable(api: SurfMinestomServer) = Unit
}

/**
 * Configures the surf api.
 *
 * @see surfMinestomServer
 */
@SurfMinestomServerDsl
class SurfMinestomServerBuilder internal constructor() {
    internal val features = linkedMapOf<String, SurfMinestomFeature>()
    internal val registrars = mutableListOf<EventRegistrar>()
    internal var configurationPhase: ConfigurationPhaseBuilder? = null
    internal var permissionProvider: PermissionProvider? = null
    internal val disabledEntityTicks = mutableSetOf<EntityType>()
    internal var playerFactory = SurfMinestomPlayerFactory(::SurfMinestomPlayer)
    internal val shutdownActions = mutableListOf<() -> Unit>()

    /**
     * The name of the event node the surf api registers its listeners on. It is added as a child
     * of the global event handler.
     */
    var eventNodeName: String = "surf-api-minestom"

    /**
     * The number of players the server admits, or `null` for any number.
     *
     * @see dev.slne.surf.api.minestom.player.PlayerLimit
     * @see dev.slne.surf.api.minestom.player.event.AsyncPlayerCountEvent
     */
    var maxPlayers: Int? = null

    /**
     * Whether to apply the settings surf recommends for a server behind a proxy, unless the server
     * chose them itself:
     *
     * - `minestom.keep-alive-delay`: 2 seconds, so the proxy does not drop a player held in the
     *   configuration phase. The server chose it by setting it with `-D`. Applying it requires the
     *   server jar built by the surf Gradle plugin, whose bootstrap makes the property writable at
     *   runtime.
     * - Packet compression off, since the connection to the proxy is local. The server chose it by
     *   changing the threshold from Minestom's default before [surfMinestomServer].
     */
    var recommendedServerProperties: Boolean = true

    /**
     * Installs [feature].
     */
    fun install(feature: SurfMinestomFeature) {
        require(features.putIfAbsent(feature.id, feature) == null) {
            "The feature '${feature.id}' is already installed"
        }
    }

    /**
     * Whether a feature with [id] is installed.
     */
    fun isInstalled(id: String): Boolean = id in features

    /**
     * Registers [registrars] on the event node of the surf api.
     */
    fun listeners(vararg registrars: EventRegistrar) {
        this.registrars += registrars
    }

    /**
     * Resolves permissions with [provider], e.g. for the requirements of CommandAPI commands.
     *
     * Features such as `withLuckPerms()` install their own provider; this one takes precedence.
     */
    fun permissions(provider: PermissionProvider) {
        permissionProvider = provider
    }

    /**
     * Stops entities of [types] from ticking, e.g. purely decorative entities in a lobby.
     *
     * Takes effect only when the server runs with the surf Mixin agent, i.e. from the server jar
     * built by the surf Gradle plugin.
     *
     * Only the entity's own update, physics and tick events stop. Tasks on the entity's scheduler,
     * such as `Entity#scheduleRemove` or `scheduler().scheduleNextTick`, keep running.
     *
     * @throws IllegalArgumentException for [EntityType.PLAYER]
     */
    fun disableEntityTicking(vararg types: EntityType) {
        disableEntityTicking(types.asIterable())
    }

    /**
     * @see disableEntityTicking
     */
    fun disableEntityTicking(types: Iterable<EntityType>) {
        types.forEach { type ->
            require(type != EntityType.PLAYER) { "Players have to tick" }
            disabledEntityTicks += type
        }
    }

    /**
     * Runs the configuration phase as an ordered list of steps that can each be replaced, removed
     * or extended.
     *
     * Without this, Minestom runs its own configuration phase.
     *
     * @see ConfigurationPhaseBuilder
     */
    fun withConfigurationPhase(block: ConfigurationPhaseBuilder.() -> Unit = {}) {
        val builder = configurationPhase ?: ConfigurationPhaseBuilder().also { configurationPhase = it }
        builder.block()
    }

    /**
     * Creates every player of the server with [factory], e.g. to use a subclass of
     * [SurfMinestomPlayer] with behavior of the server's own.
     *
     * Players are [SurfMinestomPlayer]s by default. The surf api installs the factory as Minestom's
     * player provider; while the api runs, `ConnectionManager#setPlayerProvider` rejects any other
     * provider. A provider set through Minestom before the api starts fails the start, except in
     * tests (`minestom.inside-test`), where Minestom's test environment sets one for every test.
     */
    fun playerFactory(factory: SurfMinestomPlayerFactory) {
        playerFactory = factory
    }

    /**
     * Runs [action] when the server stops, after Minestom stopped and the features and plugins
     * are disabled, e.g. to close a database the server's own code opened.
     *
     * Actions run in registration order on the thread that stops the server; one that throws is
     * logged and the others still run. They do not terminate the process, and must not call
     * [exitFlushingLogs]; [SurfMinestomServer.instance] is no longer available while they run.
     *
     * @see SurfMinestomServer.stop
     */
    fun onShutdown(action: () -> Unit) {
        shutdownActions += action
    }
}

/**
 * Creates the players of a surf server.
 *
 * @see SurfMinestomServerBuilder.playerFactory
 */
fun interface SurfMinestomPlayerFactory {

    /**
     * Creates the player of [connection], called on the player's login thread.
     */
    fun create(connection: PlayerConnection, gameProfile: GameProfile): SurfMinestomPlayer
}

/**
 * The running surf api.
 */
interface SurfMinestomServer {

    /**
     * The event node all surf listeners are registered on.
     */
    val eventNode: EventNode<Event>

    /**
     * The installed features, ordered by [SurfMinestomFeature.loadPriority], ties in installation order.
     */
    val features: Collection<SurfMinestomFeature>

    /**
     * The installed feature with [id], or `null` when it is not installed.
     */
    fun feature(id: String): SurfMinestomFeature?

    /**
     * Disables every feature and the surf api itself.
     *
     * Runs automatically when Minestom stops, see [stop] for stopping the whole server. Does
     * nothing once the api is shut down. A call from another thread while the api starts waits for
     * the start, which then fails; a call from within the start, e.g. from a feature's `enable`,
     * throws [IllegalStateException].
     */
    fun shutdown()

    /**
     * Stops the server: stops Minestom, which shuts down the api, then runs the actions registered
     * with [SurfMinestomServerBuilder.onShutdown].
     *
     * Blocks until the server is stopped. Runs once; a concurrent call waits until the first one
     * has finished, and a later call returns right away. The surf api calls it from a JVM shutdown
     * hook as well, so that the actions also run when the process is terminated.
     *
     * Minestom stops the plugins and its connections on the calling thread, which stays blocked
     * until they are stopped, so a call from a tick thread, e.g. directly from the executor of a
     * command a player ran, throws [IllegalStateException]. Like [shutdown], it cannot be called
     * from within the start.
     *
     * The process keeps running afterwards. To terminate it, use [exit], or [exitFlushingLogs] from
     * a thread that may block.
     */
    fun stop()

    /**
     * Stops the server like [stop], flushes the logs and terminates the JVM with [status].
     *
     * Runs [exitFlushingLogs] on a new non-daemon thread named `surf-minestom-exit` and returns
     * right away, so it may be called from any thread, including a tick thread, e.g. the executor
     * of a command a player ran, and the console. A shutdown action or a `disable` may call it on a
     * server reference captured before the stop, since [instance] is unavailable then. The process
     * terminates asynchronously with the status of the first exit request, after a start in
     * progress has finished; the stop itself runs once. While the JVM already shuts down, the
     * request is logged and ignored.
     */
    fun exit(status: Int = 0)

    companion object {
        /**
         * The running surf api.
         */
        val instance: SurfMinestomServer
            get() = SurfMinestomRuntime.current()
    }
}

/**
 * Starts the surf api on this Minestom server.
 *
 * Call it once, after `MinecraftServer.init()` and before `MinecraftServer#start`:
 *
 * ```
 * val server = MinecraftServer.init()
 *
 * surfMinestomServer {
 *     withConfigurationPhase()
 *     withSignedChat()
 *     withLuckPerms()
 *     withSpark()
 * }
 *
 * server.start("0.0.0.0", 25565)
 * ```
 *
 * Every feature is loaded and enabled, and with them every plugin, before this returns, so the
 * server is ready for players once it is started. The CommandAPI is ready as well, so commands can
 * be registered right after it.
 *
 * @throws IllegalStateException when the server is not initialized, the api is already running,
 * or Mixin is not installed (tests excepted)
 * @throws Exception whatever a feature threw while loading or enabling; the features loaded so far
 * are disabled again
 */
fun surfMinestomServer(block: SurfMinestomServerBuilder.() -> Unit = {}): SurfMinestomServer =
    SurfMinestomRuntime.start(SurfMinestomServerBuilder().apply(block))
