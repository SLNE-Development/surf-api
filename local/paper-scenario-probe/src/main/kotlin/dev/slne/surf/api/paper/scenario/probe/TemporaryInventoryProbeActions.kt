package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.messages.AdventureComponent
import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.paper.inventory.temporary.*
import dev.slne.surf.api.spring.SpringComponent
import kotlinx.coroutines.*
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

/**
 * The temporary inventory sessions the probe began, and the player objects it saw join, by player
 * name.
 *
 * A player object outlives its connection, so a test can begin a session for a player who left.
 */
@SpringComponent
class TemporaryInventoryProbeState(private val server: Server) : Listener {

    val sessions = ConcurrentHashMap<String, TemporaryInventorySession>()

    private val joinedPlayers = ConcurrentHashMap<String, Player>()

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        joinedPlayers[event.player.name] = event.player
    }

    /**
     * The online player called [name], or the object of the player's last join.
     */
    fun player(name: String): Player {
        return server.getPlayerExact(name)
            ?: joinedPlayers[name]
            ?: throw IllegalArgumentException("$name never joined")
    }
}

/**
 * Begins temporary inventory sessions with a fill that the request describes, and logs their
 * results.
 *
 * The fill puts the given items into the emptied inventory, selects the first hotbar slot and
 * stores the session key under [STATE_KEY] in the session state.
 */
@SpringComponent
class TemporaryInventoryBeginner(
    private val plugin: Plugin,
    private val state: TemporaryInventoryProbeState,
) {

    /**
     * Begins a session for [player] and returns the reply for it: `started <session id>`,
     * `already-active <key>`, `unstorable <slot>` or `unavailable`.
     */
    suspend fun begin(player: Player, key: String, items: Map<Int, ItemStack>): String {
        val sessionKey = Key.key(key)
        val result = player.beginTemporaryInventorySession(plugin, sessionKey) { inventory, data ->
            for ((slot, item) in items) {
                inventory.setItem(slot, item)
            }

            inventory.heldItemSlot = 0
            data.set(NamespacedKey(plugin, STATE_KEY), PersistentDataType.STRING, key)
        }

        return when (result) {
            is TemporaryInventoryBeginResult.Started -> {
                state.sessions[player.name] = result.session
                "started ${result.session.id}"
            }

            is TemporaryInventoryBeginResult.AlreadyActive -> {
                "already-active ${result.key.asString()}"
            }

            is TemporaryInventoryBeginResult.Unstorable -> "unstorable ${result.slot}"
            TemporaryInventoryBeginResult.Unavailable -> "unavailable"
        }
    }

    companion object {
        const val STATE_KEY = "session_key"
    }
}

/**
 * `temp-begin <player> <key> <items>`: begins a temporary inventory session whose fill puts
 * `items` (`slot:MATERIAL:amount,...` or `none`) into the inventory, and replies with the result.
 *
 * The player may have left: the probe then uses the object of the player's last join.
 */
@SpringComponent
class TemporaryInventoryBeginProbe(
    private val state: TemporaryInventoryProbeState,
    private val beginner: TemporaryInventoryBeginner,
) : ProbeAction {

    override val name = "temp-begin"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, key, items) = arguments

        return beginner.begin(state.player(playerName), key, parseItems(items))
    }
}

/**
 * `fill-slots <player> <material>`: puts `slot + 1` items of the material into every slot of the
 * player's inventory, armor, offhand, body armor and saddle included.
 */
@SpringComponent
class FillSlotsProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "fill-slots"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, material) = arguments
        val player = server.onlinePlayer(playerName)

        withContext(dispatchers.entity(player)) {
            val inventory = player.inventory

            for (slot in 0 until inventory.size) {
                inventory.setItem(slot, ItemStack(material(material), slot + 1))
            }
        }

        return "ok"
    }
}

/**
 * `temp-end <player> <sync|async>`: ends the session the probe began last for the player, with
 * `end()` on the player's thread or with `endAsync()`, and replies `true` or `false`.
 */
@SpringComponent
class TemporaryInventoryEndProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
    private val state: TemporaryInventoryProbeState,
) : ProbeAction {

    override val name = "temp-end"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, mode) = arguments
        val session = state.sessions[playerName]
            ?: throw IllegalArgumentException("No session began for $playerName")

        val ended = when (mode) {
            "sync" -> withContext(dispatchers.entity(server.onlinePlayer(playerName))) {
                session.end()
            }

            "async" -> session.endAsync()
            else -> throw IllegalArgumentException("Unknown end mode $mode")
        }

        return ended.toString()
    }
}

/**
 * `temp-end-off-thread <player>`: calls `end()` for the player's session off the player's thread
 * and replies `threw <exception class>` or `returned <result>`.
 */
@SpringComponent
class TemporaryInventoryOffThreadEndProbe(
    private val state: TemporaryInventoryProbeState,
) : ProbeAction {

    override val name = "temp-end-off-thread"

    override suspend fun run(arguments: List<String>): String {
        val session = state.sessions[arguments.single()]
            ?: throw IllegalArgumentException("No session began for ${arguments.single()}")

        return withContext(Dispatchers.IO) {
            try {
                "returned ${session.end()}"
            } catch (failure: IllegalStateException) {
                "threw ${failure.javaClass.name}"
            }
        }
    }
}

/**
 * `temp-session <player>`: the player's active session, read off the server threads, as
 * `<key> <session id>`, or `none`.
 */
@SpringComponent
class TemporaryInventorySessionProbe(private val server: Server) : ProbeAction {

    override val name = "temp-session"

    override suspend fun run(arguments: List<String>): String {
        val player = server.onlinePlayer(arguments.single())
        val session = withContext(Dispatchers.Default) { player.temporaryInventorySession }

        return if (session == null) "none" else "${session.key.asString()} ${session.id}"
    }
}

/**
 * `temp-menu <player> <key> <items>`: opens a menu whose first slot holds an emerald. Clicking it
 * begins a temporary inventory session from inside the click event, as `temp-begin` describes,
 * and logs the result as the event `temp-begin <player> <result>`.
 *
 * The click is not cancelled, so the click also takes the emerald onto the cursor.
 */
@SpringComponent
class TemporaryInventoryMenuProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
    private val scope: CoroutineScope,
    private val beginner: TemporaryInventoryBeginner,
) : ProbeAction, Listener {

    private val log = logger()

    override val name = "temp-menu"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, key, items) = arguments
        val player = server.onlinePlayer(playerName)
        val menu = BeginMenu(key, parseItems(items))

        withContext(dispatchers.entity(player)) {
            player.openInventory(menu.inventory)
        }

        return "ok"
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        val menu = event.view.topInventory.getHolder(false) as? BeginMenu ?: return
        val player = event.whoClicked as? Player ?: return
        if (event.rawSlot != BUTTON_SLOT) return

        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            val reply = beginner.begin(player, menu.key, menu.items)

            log.atInfo()
                .log("%s %s %s %s", ProbeProtocol.EVENT, BEGIN_EVENT, player.name, reply)
        }
    }

    private class BeginMenu(val key: String, val items: Map<Int, ItemStack>) : InventoryHolder {

        private val menu = createMenu()

        override fun getInventory(): Inventory = menu

        private fun createMenu(): Inventory {
            val inventory = Bukkit.createInventory(
                this,
                MENU_SIZE,
                AdventureComponent.text("Begin a session"),
            )
            inventory.setItem(BUTTON_SLOT, ItemStack(Material.EMERALD))

            return inventory
        }
    }

    companion object {
        const val BEGIN_EVENT = "temp-begin"
        private const val BUTTON_SLOT = 0
        private const val MENU_SIZE = 9
    }
}

/**
 * Logs every [TemporaryInventoryEndEvent] as the event
 * `temp-end <player> <cause> <session id> <state> <discarded items>`, with the session key stored
 * by the probe's fill as state (or `none`) and the discarded items as sorted `MATERIAL:amount`
 * (or `none`).
 */
@SpringComponent
class TemporaryInventoryEndLog(private val plugin: Plugin) {

    private val log = logger()

    @SurfEventHandler
    fun onEnd(event: TemporaryInventoryEndEvent) {
        val state = event.state.get(
            NamespacedKey(plugin, TemporaryInventoryBeginner.STATE_KEY),
            PersistentDataType.STRING,
        )
        val discarded = event.discardedItems
            .map { it.describe() }
            .sorted()
            .ifEmpty { listOf("none") }
            .joinToString(",")

        log.atInfo()
            .log(
                "%s %s %s %s %s %s %s",
                ProbeProtocol.EVENT,
                END_EVENT,
                event.player.name,
                event.cause,
                event.sessionId,
                state ?: "none",
                discarded,
            )
    }

    companion object {
        const val END_EVENT = "temp-end"
    }
}

/**
 * Reads `slot:MATERIAL:amount,...`, or `none` for no items.
 */
private fun parseItems(items: String): Map<Int, ItemStack> {
    if (items == "none") return emptyMap()

    return items.split(',').associate { item ->
        val (slot, material, amount) = item.split(':')

        slot.toInt() to ItemStack(material(material), amount.toInt())
    }
}
