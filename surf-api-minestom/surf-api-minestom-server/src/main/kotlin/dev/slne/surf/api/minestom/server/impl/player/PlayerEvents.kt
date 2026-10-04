package dev.slne.surf.api.minestom.server.impl.player

import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.extension.GlobalEventHandler
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.player.PlayerLimit
import dev.slne.surf.api.minestom.player.event.AsyncPlayerCountEvent
import dev.slne.surf.api.minestom.player.event.AsyncPlayerSpawnLocationEvent
import dev.slne.surf.api.minestom.player.event.PlayerJoinEvent
import dev.slne.surf.api.minestom.player.event.PlayerQuitEvent
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import net.minestom.server.adventure.MinestomAdventure
import net.minestom.server.adventure.audience.Audiences
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerDisconnectEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.ping.Status
import java.text.MessageFormat
import java.util.*

/**
 * Calls the player events of the surf api.
 *
 * Without the configuration phase of the surf api, [AsyncPlayerCountEvent] and
 * [AsyncPlayerSpawnLocationEvent] are called around Minestom's own
 * [AsyncPlayerConfigurationEvent]: from a node that runs before every other listener and from one
 * that runs after them. With it, they are steps of their own.
 */
internal object PlayerEvents {
    private val SERVER_FULL: Component = Component.text("The server is full.", NamedTextColor.RED)

    private var first: EventNode<Event>? = null
    private var last: EventNode<Event>? = null

    /** Players that joined, so that only they are announced to have left. */
    private val joined = Collections.newSetFromMap(WeakHashMap<Player, Boolean>())

    @Volatile
    private var translationsRegistered = false

    fun install(node: EventNode<Event>, configurationPhase: Boolean) {
        registerTranslations()

        if (!configurationPhase) {
            val first = EventNode.all("surf-api-player-events-first").setPriority(Int.MIN_VALUE)
            first.addListener<AsyncPlayerConfigurationEvent> { event ->
                if (event.isFirstConfig) admit(event.player)
            }

            val last = EventNode.all("surf-api-player-events-last").setPriority(Int.MAX_VALUE)
            last.addListener<AsyncPlayerConfigurationEvent> { event ->
                if (event.isFirstConfig && event.player.isOnline) chooseSpawnLocation(event)
            }

            GlobalEventHandler.addChild(first)
            GlobalEventHandler.addChild(last)
            this.first = first
            this.last = last
        }

        node.addListener<PlayerSpawnEvent> { event ->
            if (event.isFirstSpawn) join(event.player)
        }
        node.addListener<PlayerDisconnectEvent> { event -> quit(event.player) }
        node.addListener<ServerListPingEvent>(::reportPlayerCount)
    }

    fun uninstall() {
        first?.let(GlobalEventHandler::removeChild)
        last?.let(GlobalEventHandler::removeChild)
        first = null
        last = null
        synchronized(joined) { joined.clear() }
    }

    /**
     * Calls [AsyncPlayerCountEvent] for [player] and kicks them unless a slot is theirs.
     *
     * @return whether the player may join
     */
    fun admit(player: Player): Boolean {
        val maxPlayers = PlayerLimit.maxPlayers
        val playerCount = PlayerLimit.playerCount
        val full = maxPlayers != null && playerCount > maxPlayers

        val event = AsyncPlayerCountEvent(
            player = player,
            playerCount = playerCount,
            maxPlayers = maxPlayers,
            result = if (full) AsyncPlayerCountEvent.Result.KICK_FULL else AsyncPlayerCountEvent.Result.ALLOWED,
            kickMessage = SERVER_FULL,
        )
        EventDispatcher.call(event)

        if (event.isAllowed) return true

        player.kick(event.kickMessage)
        return false
    }

    /**
     * Calls [AsyncPlayerSpawnLocationEvent] and applies its result to [configuration].
     */
    fun chooseSpawnLocation(configuration: AsyncPlayerConfigurationEvent) {
        val player = configuration.player
        val instance = requireNotNull(configuration.spawningInstance) {
            "You need to specify a spawning instance in the AsyncPlayerConfigurationEvent"
        }

        val event = AsyncPlayerSpawnLocationEvent(player, instance, player.respawnPoint)
        EventDispatcher.call(event)

        configuration.spawningInstance = event.instance
        player.respawnPoint = event.position
    }

    private fun join(player: Player) {
        synchronized(joined) { joined += player }

        val event = PlayerJoinEvent(
            player,
            Component.translatable("multiplayer.player.joined", NamedTextColor.YELLOW, player.name),
        )
        EventDispatcher.call(event)

        event.joinMessage?.let(::broadcast)
    }

    private fun quit(player: Player) {
        val wasJoined = synchronized(joined) { joined.remove(player) }
        if (!wasJoined) return

        val event = PlayerQuitEvent(
            player,
            Component.translatable("multiplayer.player.left", NamedTextColor.YELLOW, player.name),
        )
        EventDispatcher.call(event)

        event.quitMessage?.let(::broadcast)
    }

    private fun broadcast(message: Component) {
        for (player in ConnectionManager.onlinePlayers) {
            player.sendMessage(message)
        }
        Audiences.console().sendMessage(GlobalTranslator.render(message, MinestomAdventure.getDefaultLocale()))
    }

    private fun reportPlayerCount(event: ServerListPingEvent) {
        val maxPlayers = PlayerLimit.maxPlayers ?: return
        val playerInfo = event.status.playerInfo() ?: return

        event.status = Status.builder(event.status)
            .playerInfo(
                Status.PlayerInfo.builder(playerInfo)
                    .onlinePlayers(PlayerLimit.playerCount)
                    .maxPlayers(maxPlayers)
                    .build()
            )
            .build()
    }

    /** Server-side fallbacks, so that the console shows the messages rather than their keys. */
    private fun registerTranslations() {
        if (translationsRegistered) return
        translationsRegistered = true

        val store = TranslationStore.messageFormat(Key.key("surf", "player"))
        store.defaultLocale(Locale.US)
        store.registerAll(
            Locale.US,
            mapOf(
                "multiplayer.player.joined" to MessageFormat("{0} joined the game"),
                "multiplayer.player.left" to MessageFormat("{0} left the game"),
            ),
        )
        GlobalTranslator.translator().addSource(store)
    }
}
