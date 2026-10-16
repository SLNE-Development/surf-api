package dev.slne.surf.api.minestom.server

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.permission.PermissionProvider
import dev.slne.surf.api.minestom.server.configuration.ConfigurationPhaseBuilder
import dev.slne.surf.api.minestom.server.impl.SurfMinestomRuntime
import net.minestom.server.entity.EntityType
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

@DslMarker
annotation class SurfMinestomServerDsl

/**
 * An optional part of the surf api, installed through [SurfMinestomServerBuilder.install].
 *
 * Feature modules ship their feature together with an extension on [SurfMinestomServerBuilder], e.g.
 * `withSignedChat()` or `withLuckPerms()`, so that depending on the module is all it takes to
 * offer the feature.
 *
 * Features are loaded in the order of their [loadPriority], then in the order they were
 * installed, and disabled in reverse.
 */
interface SurfMinestomFeature {

    /** Identifies the feature; a feature can be installed only once. */
    val id: String

    /** Features with a higher priority are loaded later and disabled earlier. */
    val loadPriority: Int get() = 0

    /**
     * Called while [surfMinestomServer] runs, before the server starts. Register listeners, packet
     * listeners and commands here.
     */
    fun load(api: SurfMinestomServer) = Unit

    /** Called on the first tick after the server started. */
    fun enable(api: SurfMinestomServer) = Unit

    /** Called when the server shuts down, or when [SurfMinestomServer.shutdown] is called. */
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
     *   configuration phase. Kept when set with `-D`; requires the server jar built by the surf
     *   Gradle plugin, whose bootstrap makes the property writable at runtime.
     * - Packet compression off, since the connection to the proxy is local. Kept when the
     *   threshold was changed from Minestom's default before [surfMinestomServer].
     */
    var recommendedServerProperties: Boolean = true

    /** Installs [feature]. */
    fun install(feature: SurfMinestomFeature) {
        require(features.putIfAbsent(feature.id, feature) == null) {
            "The feature '${feature.id}' is already installed"
        }
    }

    /** Whether a feature with [id] is installed. */
    fun isInstalled(id: String): Boolean = id in features

    /** Registers [registrars] on the event node of the surf api. */
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
     * @throws IllegalArgumentException for [EntityType.PLAYER]
     */
    fun disableEntityTicking(vararg types: EntityType) {
        disableEntityTicking(types.asIterable())
    }

    /** @see disableEntityTicking */
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
     * Without this, Minestom runs its own configuration.
     *
     * @see ConfigurationPhaseBuilder
     */
    fun withConfigurationPhase(block: ConfigurationPhaseBuilder.() -> Unit = {}) {
        val builder = configurationPhase ?: ConfigurationPhaseBuilder().also { configurationPhase = it }
        builder.block()
    }
}

/**
 * The running surf api.
 */
interface SurfMinestomServer {

    /** The event node all surf listeners are registered on. */
    val eventNode: EventNode<Event>

    /** The installed features, in the order they were installed. */
    val features: Collection<SurfMinestomFeature>

    /** The installed feature with [id], or `null` when it is not installed. */
    fun feature(id: String): SurfMinestomFeature?

    /**
     * Disables every feature and the surf api itself.
     *
     * Runs automatically when the server shuts down.
     */
    fun shutdown()

    companion object {
        /** The running surf api. */
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
 * The CommandAPI is ready as soon as this returns, so commands can be registered right after it.
 */
fun surfMinestomServer(block: SurfMinestomServerBuilder.() -> Unit = {}): SurfMinestomServer =
    SurfMinestomRuntime.start(SurfMinestomServerBuilder().apply(block))
