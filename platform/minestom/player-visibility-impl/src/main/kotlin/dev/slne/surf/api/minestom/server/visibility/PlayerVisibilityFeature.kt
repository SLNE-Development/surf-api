package dev.slne.surf.api.minestom.server.visibility

import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.visibility.PlayerVisibility
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate

/**
 * Configures [withPlayerVisibility].
 */
@SurfMinestomServerDsl
class PlayerVisibilitySettings internal constructor() {

    /**
     * How many players a player sees at most while the limit is active.
     */
    var maxVisible: Int = 50

    /**
     * The limit activates once at least this many players would be visible.
     */
    var activateAt: Int = 60

    /**
     * The limit deactivates once fewer than this many players would be visible.
     */
    var deactivateBelow: Int = 45

    /**
     * How often, in ticks, the visible players of every player are selected anew.
     */
    var refreshIntervalTicks: Int = 5

    /**
     * Distance multiplier for players that are already visible, so that they are not swapped
     * for players that are only slightly closer. `1.0` disables the bias.
     */
    var retentionDistanceFactor: Double = 0.9

    internal fun validate() {
        require(maxVisible > 0) { "maxVisible must be greater than 0" }
        require(activateAt >= maxVisible) { "activateAt must be greater than or equal to maxVisible" }
        require(deactivateBelow in 1..maxVisible && deactivateBelow < activateAt) {
            "deactivateBelow must be between 1 and maxVisible, and less than activateAt"
        }
        require(refreshIntervalTicks > 0) { "refreshIntervalTicks must be greater than 0" }
        require(retentionDistanceFactor > 0.0 && retentionDistanceFactor <= 1.0) {
            "retentionDistanceFactor must be between 0.0 and 1.0"
        }
    }
}

/**
 * Limits how many players each player sees at once to the nearest ones.
 *
 * The feature owns the viewer rule and the viewable rule of every player from their first
 * configuration on. Restrict what a player sees through [PlayerVisibility.updateViewerRule];
 * calling `Player#updateViewerRule` directly removes the limit for that player.
 */
@OptIn(InternalSurfApi::class)
class PlayerVisibilityFeature internal constructor(
    private val settings: PlayerVisibilitySettings,
) : SurfMinestomFeature, PlayerVisibility.Provider {

    override val id: String = ID

    private val handlers = ConcurrentHashMap<UUID, PlayerVisibilityHandler>()
    private val service = PlayerVisibilityService(settings) { player -> handler(player) }

    override fun load(api: SurfMinestomServer) {
        PlayerVisibility.installProvider(this)

        api.eventNode.addListener<AsyncPlayerConfigurationEvent> { event ->
            handlerFor(event.player)
        }

        api.eventNode.addListener<PlayerSpawnEvent> { event ->
            if (!event.isFirstSpawn) return@addListener

            handlerFor(event.player)
        }

        api.eventNode.addListener<PlayerDisconnectEvent> { event ->
            val player = event.player
            val handler = handler(player) ?: return@addListener

            if (handlers.remove(player.uuid, handler)) {
                handler.close()
            }
        }
    }

    override fun enable(api: SurfMinestomServer) {
        service.start()
    }

    override fun disable(api: SurfMinestomServer) {
        PlayerVisibility.installProvider(null)
        service.stop()
        handlers.values.forEach(PlayerVisibilityHandler::close)
        handlers.clear()
    }

    internal fun handler(player: Player): PlayerVisibilityHandler? =
        handlers[player.uuid]?.takeIf { it.isFor(player) }

    /**
     * Returns the handler of [player], creating and initializing it when it does not exist yet.
     *
     * The handler is created during the first configuration, before [player] is registered in an
     * entity tracker, so that no player is shown before the limit applies. Requests that arrive
     * earlier create it on demand. A handler left over from an earlier connection with the same
     * UUID is replaced and closed. Returns `null` for a player that is no longer online.
     */
    private fun handlerFor(player: Player): PlayerVisibilityHandler? {
        handler(player)?.let { return it }
        if (!player.isOnline) return null

        val created = PlayerVisibilityHandler(player)
        var stale: PlayerVisibilityHandler? = null
        val current = handlers.compute(player.uuid) { _, existing ->
            if (existing != null && existing.isFor(player)) {
                existing
            } else {
                stale = existing
                created
            }
        }

        stale?.close()
        if (current === created) {
            created.init()
        }

        return current
    }

    override fun updateViewerRule(player: Player, predicate: Predicate<in Entity>?): Boolean {
        val handler = handlerFor(player) ?: return false
        handler.requestRule(predicate)
        return true
    }

    override fun refreshViewerRule(player: Player): Boolean {
        val handler = handlerFor(player) ?: return false
        handler.requestRuleRefresh()
        return true
    }

    override fun isLimited(player: Player): Boolean = handler(player)?.visibilityLimited == true

    override fun manages(player: Player): Boolean = handler(player) != null

    companion object {
        const val ID = "player-visibility"
    }
}

/**
 * Installs the player visibility limit.
 *
 * @throws IllegalArgumentException If the settings violate their documented constraints.
 * @see PlayerVisibilityFeature
 */
fun SurfMinestomServerBuilder.withPlayerVisibility(block: PlayerVisibilitySettings.() -> Unit = {}) {
    val settings = PlayerVisibilitySettings().apply(block)
    settings.validate()
    install(PlayerVisibilityFeature(settings))
}
