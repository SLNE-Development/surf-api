package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryBeginResult
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndCause
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndEvent
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventorySessions
import dev.slne.surf.api.paper.region.TickThreadGuard
import kotlinx.coroutines.*
import net.kyori.adventure.key.Key
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * Begins and ends temporary inventory sessions.
 *
 * The player's persistent data container holds the only copy of the original inventory
 * ([TemporaryInventoryStorage]) and decides whether a player has a session. The concurrent index
 * of active sessions only answers queries from any thread. Both change only on the thread that
 * owns the player, and every begin and end runs there without suspending, so no save of the
 * player and no other access to the inventory falls in between.
 *
 * Automatic ends call [end] with their cause on the player's thread, look sessions up with
 * [activeSession] and [activeSessionsOwnedBy], and discard loose temporary items with
 * [discardLooseItems].
 */
@Service
class TemporaryInventoryService(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
    private val tickThreadGuard: TickThreadGuard,
) : TemporaryInventorySessions {

    private val log = logger()
    private val activeSessions = ConcurrentHashMap<UUID, ActiveTemporaryInventorySession>()

    override suspend fun begin(
        player: Player,
        plugin: Plugin,
        key: Key,
        fill: (inventory: PlayerInventory, state: PersistentDataContainer) -> Unit,
    ): TemporaryInventoryBeginResult {
        return suspendCancellableCoroutine { continuation ->
            val ticket = Job()

            // `dispatch` always schedules, unlike `withContext`, which runs in place when the
            // caller already owns the player. A rejected ticket is cancelled and run off-thread.
            dispatchers.entity(player).dispatch(ticket) {
                when {
                    ticket.isCancelled -> {
                        continuation.resume(TemporaryInventoryBeginResult.Unavailable)
                    }

                    continuation.isActive -> {
                        continuation.resumeWith(runCatching { beginNow(player, plugin, key, fill) })
                    }
                }
            }
        }
    }

    override fun activeSession(playerUuid: UUID): ActiveTemporaryInventorySession? {
        return activeSessions[playerUuid]
    }

    /**
     * The active sessions that the plugin named [pluginName] began.
     */
    fun activeSessionsOwnedBy(pluginName: String): List<ActiveTemporaryInventorySession> {
        return activeSessions.values.filter { it.owner.name == pluginName }
    }

    /**
     * Ends the session stored in the player's data with [cause], whether or not it is active in
     * this server run, and fires [TemporaryInventoryEndEvent].
     *
     * @param discarded temporary items already taken from the player for this session, reported
     * with the event in addition to those the end takes
     * @return `false` if the player's data holds no session, or an entry this server cannot read
     * @throws IllegalStateException off the thread that owns the player
     */
    fun end(
        player: Player,
        cause: TemporaryInventoryEndCause,
        discarded: List<ItemStack> = emptyList(),
    ): Boolean {
        tickThreadGuard.ensureTickThread(player, OFF_THREAD_END)

        val entry = TemporaryInventoryStorage.read(player) ?: return false
        val session = entry.sessions.firstOrNull() ?: return false

        endNow(player, entry, session, cause, discarded)

        return true
    }

    /**
     * Takes the item on the cursor and the crafting inputs of the open view from the player and
     * returns them.
     *
     * The crafting inputs are those of the player's own crafting grid or of an open crafting
     * table.
     */
    fun discardLooseItems(player: Player): List<ItemStack> {
        val discarded = mutableListOf<ItemStack>()
        val cursor = player.itemOnCursor

        if (!cursor.isEmpty) {
            discarded += cursor.clone()
            player.setItemOnCursor(null)
        }

        val crafting = player.openInventory.topInventory as? CraftingInventory ?: return discarded
        val matrix = crafting.matrix

        discarded += matrix.filterNotNull().filterNot { it.isEmpty }.map { it.clone() }
        crafting.matrix = arrayOfNulls(matrix.size)

        return discarded
    }

    internal fun isActive(session: ActiveTemporaryInventorySession): Boolean {
        return activeSessions[session.playerUuid] === session
    }

    internal fun end(session: ActiveTemporaryInventorySession): Boolean {
        val player = server.getPlayer(session.playerUuid)

        if (player == null) {
            check(!isActive(session)) { "The player of session ${session.id} is offline." }

            return false
        }

        tickThreadGuard.ensureTickThread(player, OFF_THREAD_END)

        if (!isActive(session)) return false

        val entry = TemporaryInventoryStorage.read(player) ?: return false
        val stored = entry.session(session.id)

        if (stored == null) {
            activeSessions.remove(session.playerUuid, session)

            return false
        }

        endNow(player, entry, stored, TemporaryInventoryEndCause.ENDED, emptyList())

        return true
    }

    internal suspend fun endAsync(session: ActiveTemporaryInventorySession): Boolean {
        val player = server.getPlayer(session.playerUuid) ?: return false

        return try {
            withContext(dispatchers.entity(player)) { end(session) }
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            false
        }
    }

    private fun beginNow(
        player: Player,
        plugin: Plugin,
        key: Key,
        fill: (inventory: PlayerInventory, state: PersistentDataContainer) -> Unit,
    ): TemporaryInventoryBeginResult {
        if (player.isDead || !player.isConnected || !player.isValid) {
            return TemporaryInventoryBeginResult.Unavailable
        }

        val entry = TemporaryInventoryStorage.read(player)
            ?: return TemporaryInventoryBeginResult.Unavailable
        val existing = entry.sessions.firstOrNull()

        if (existing != null) {
            return TemporaryInventoryBeginResult.AlreadyActive(Key.key(existing.key))
        }

        player.closeInventory(InventoryCloseEvent.Reason.PLUGIN)

        val inventory = player.inventory
        val original = OriginalInventory.capture(inventory)
        val slots = when (val serialized = original.serialize()) {
            is SerializedSlots.Stored -> serialized.slots
            is SerializedSlots.Failed -> {
                return TemporaryInventoryBeginResult.Unstorable(serialized.slot, serialized.cause)
            }
        }
        val session = StoredSession(
            id = UUID.randomUUID(),
            owner = plugin.name,
            key = key.asString(),
            selectedSlot = original.selectedSlot,
            slots = slots,
            state = ByteArray(0),
        )

        TemporaryInventoryStorage.write(player, entry.withSession(session))
        inventory.clear()

        try {
            val state = player.persistentDataContainer.adapterContext.newPersistentDataContainer()

            fill(inventory, state)
            TemporaryInventoryStorage.write(
                player,
                entry.withSession(session.withState(state.serializeToBytes())),
            )
        } catch (failure: Throwable) {
            original.restore(inventory)
            TemporaryInventoryStorage.write(player, entry)

            throw failure
        }

        val active = ActiveTemporaryInventorySession(session.id, key, player.uniqueId, plugin, this)
        activeSessions[player.uniqueId] = active

        return TemporaryInventoryBeginResult.Started(active)
    }

    private fun endNow(
        player: Player,
        entry: StoredEntry,
        session: StoredSession,
        cause: TemporaryInventoryEndCause,
        discarded: List<ItemStack>,
    ) {
        val inventory = player.inventory
        val temporaryItems = buildList {
            addAll(discarded)
            addAll(inventory.takeItems())
            addAll(discardLooseItems(player))
        }
        val restore = session.planRestore(inventory.size, ItemStack::deserializeBytes)

        for ((slot, item) in restore.items.withIndex()) {
            inventory.setItem(slot, item)
        }

        if (session.selectedSlot in HOTBAR_SLOTS) {
            inventory.heldItemSlot = session.selectedSlot
        }

        logUnreadable(player, session, restore.unreadable)
        TemporaryInventoryStorage.write(
            player,
            entry.withoutSession(session.id, restore.unreadable),
        )
        activeSessions.computeIfPresent(player.uniqueId) { _, active ->
            active.takeUnless { it.id == session.id }
        }

        TemporaryInventoryEndEvent(
            player = player,
            key = Key.key(session.key),
            sessionId = session.id,
            cause = cause,
            state = player.readState(session.state),
            discardedItems = temporaryItems,
        ).call()
    }

    private fun logUnreadable(
        player: Player,
        session: StoredSession,
        unreadable: List<UnreadableSlot>,
    ) {
        for (slot in unreadable) {
            log.atSevere()
                .withCause(slot.cause)
                .log(
                    "Slot %d of %s's original inventory (session %s of %s) cannot be read; " +
                        "it stays in the player's data.",
                    slot.slot,
                    player.name,
                    session.id,
                    session.owner,
                )
        }
    }

    private fun PlayerInventory.takeItems(): List<ItemStack> {
        return contents.filterNotNull()
            .filterNot { it.isEmpty }
            .map { it.clone() }
    }

    private fun Player.readState(bytes: ByteArray): PersistentDataContainer {
        val state = persistentDataContainer.adapterContext.newPersistentDataContainer()
        if (bytes.isNotEmpty()) state.readFromBytes(bytes)

        return state
    }

    private companion object {
        const val OFF_THREAD_END = "Ending a temporary inventory session off the player's thread"
        val HOTBAR_SLOTS = 0..8
    }
}
