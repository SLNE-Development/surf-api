package dev.slne.surf.api.minestom.server.visibility

import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.visibility.PlayerVisibility
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
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

    /** How many players a player sees at most while the budget is active. */
    var maxVisible: Int = 50

    /** The budget activates once at least this many players would be visible. */
    var activateAt: Int = 60

    /** The budget deactivates once fewer than this many players would be visible. */
    var deactivateBelow: Int = 45

    /** How often, in ticks, the visible players of every player are selected anew. */
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
 * Limits how many players a player sees at once to the nearest ones, which keeps crowded areas
 * cheap for both the server and the clients.
 *
 * The feature owns the viewer rule and the viewable rule of every player. Restrict what a player
 * sees through [PlayerVisibility.updateViewerRule] instead of `Player#updateViewerRule`, which
 * would remove the budget for that player.
 */
@OptIn(InternalSurfApi::class)
class PlayerVisibilityFeature internal constructor(
    private val settings: PlayerVisibilitySettings,
) : SurfMinestomFeature, PlayerVisibility.Provider {

    override val id: String = ID

    private val handlers = ConcurrentHashMap<UUID, PlayerVisibilityHandler>()
    private val service = PlayerVisibilityService(settings) { player -> handlers[player.uuid] }

    override fun load(api: SurfMinestomServer) {
        PlayerVisibility.installProvider(this)

        api.eventNode.addListener<PlayerSpawnEvent> { event ->
            if (!event.isFirstSpawn) return@addListener

            val handler = PlayerVisibilityHandler(event.player)
            handlers[event.player.uuid] = handler
            handler.init()
        }

        api.eventNode.addListener<PlayerDisconnectEvent> { event ->
            handlers.remove(event.player.uuid)
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

    internal fun handler(player: Player): PlayerVisibilityHandler? = handlers[player.uuid]

    override fun updateViewerRule(player: Player, predicate: Predicate<in Entity>?): Boolean {
        val handler = handler(player) ?: return false
        handler.requestRule(predicate)
        return true
    }

    override fun refreshViewerRule(player: Player): Boolean {
        val handler = handler(player) ?: return false
        handler.requestRuleRefresh()
        return true
    }

    override fun isLimited(player: Player): Boolean = handler(player)?.visibilityLimited == true

    companion object {
        const val ID = "player-visibility"
    }
}

/**
 * Limits how many players a player sees at once to the nearest ones.
 *
 * @see PlayerVisibilityFeature
 */
fun SurfMinestomServerBuilder.withPlayerVisibility(block: PlayerVisibilitySettings.() -> Unit = {}) {
    val settings = PlayerVisibilitySettings().apply(block)
    settings.validate()
    install(PlayerVisibilityFeature(settings))
}
