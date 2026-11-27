package dev.slne.surf.api.paper.server.impl.inventory.temporary

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.paper.inventory.insertion.planInsertion
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
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Begins and ends temporary inventory sessions.
 *
 * The player's persistent data container holds the only copy of the original inventory
 * ([TemporaryInventoryStorage]) and decides whether a player has a session. The concurrent index
 * of active sessions only answers queries from any thread; an end that finds no stored session
 * drops the player's index entry. Both change only on the thread that owns the player, and every
 * begin and end runs there without suspending, so no save of the player and no other access to
 * the inventory falls in between.
 *
 * An end fires plugin callbacks, such as crafting events, before it completes. An end of the same
 * player that such a callback starts returns `false`.
 *
 * [closeAdmission] marks an owner as closed for new sessions and then reads the index, while a
 * begin indexes its session and then checks its owner. Every session of a closed owner therefore
 * either is in the list [closeAdmission] returns or is rolled back by its begin.
 *
 * Automatic ends call [end] with their cause on the player's thread, look sessions up with
 * [activeSession], and discard loose temporary items with [discardLooseItems].
 */
@Service
class TemporaryInventoryService(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
    private val tickThreadGuard: TickThreadGuard,
) : TemporaryInventorySessions {

    private val log = logger()
    private val activeSessions = ConcurrentHashMap<UUID, ActiveTemporaryInventorySession>()
    private val endingPlayers = ConcurrentHashMap.newKeySet<UUID>()
    private val closedOwners = ConcurrentHashMap.newKeySet<String>()

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
                        beginAndResume(continuation, player, plugin, key, fill)
                    }
                }
            }
        }
    }

    override fun activeSession(playerUuid: UUID): ActiveTemporaryInventorySession? {
        return activeSessions[playerUuid]
    }

    /**
     * Refuses new sessions of the plugin named [pluginName] until [openAdmission], and returns its
     * active sessions.
     */
    fun closeAdmission(pluginName: String): List<ActiveTemporaryInventorySession> {
        closedOwners += pluginName

        return activeSessions.values.filter { it.owner.name == pluginName }
    }

    /**
     * Accepts new sessions of the plugin named [pluginName] again.
     */
    fun openAdmission(pluginName: String) {
        closedOwners -= pluginName
    }

    /**
     * Ends the session stored in the player's data with [cause], whether or not it is active in
     * this server run, and fires [TemporaryInventoryEndEvent].
     *
     * @param discarded temporary items already taken from the player for this session, reported
     * with the event in addition to those the end takes
     * @return `false` if the player's data holds no session or an entry this server cannot read,
     * or if the player's session is already ending
     * @throws IllegalStateException off the thread that owns the player
     */
    fun end(
        player: Player,
        cause: TemporaryInventoryEndCause,
        discarded: List<ItemStack> = emptyList(),
    ): Boolean {
        tickThreadGuard.ensureTickThread(player, OFF_THREAD_END)

        return endStored(player, cause, discarded) { entry -> entry.sessions.firstOrNull() }
    }

    /**
     * Takes the item on the cursor and the inputs of an open workstation menu from the player and
     * returns them.
     */
    fun discardLooseItems(player: Player): List<ItemStack> = player.takeLooseItems()

    internal fun isActive(session: ActiveTemporaryInventorySession): Boolean {
        return activeSessions[session.playerUuid] === session
    }

    internal fun end(session: ActiveTemporaryInventorySession): Boolean {
        if (!isActive(session)) return false

        val player = checkNotNull(server.getPlayer(session.playerUuid)) {
            "The player of session ${session.id} is offline."
        }
        tickThreadGuard.ensureTickThread(player, OFF_THREAD_END)

        return endStored(player, TemporaryInventoryEndCause.ENDED, emptyList()) { entry ->
            entry.session(session.id)
        }
    }

    internal suspend fun endAsync(session: ActiveTemporaryInventorySession): Boolean {
        if (!isActive(session)) return false

        val player = server.getPlayer(session.playerUuid) ?: return false

        return try {
            withContext(dispatchers.entity(player)) { end(session) }
        } catch (_: CancellationException) {
            currentCoroutineContext().ensureActive()
            false
        }
    }

    private fun beginAndResume(
        continuation: CancellableContinuation<TemporaryInventoryBeginResult>,
        player: Player,
        plugin: Plugin,
        key: Key,
        fill: (inventory: PlayerInventory, state: PersistentDataContainer) -> Unit,
    ) {
        val result = try {
            beginNow(player, plugin, key, fill)
        } catch (failure: Throwable) {
            continuation.resumeWithException(failure)
            return
        }

        continuation.resume(result) { _, unclaimed, _ -> endUnclaimed(player, unclaimed) }
    }

    /**
     * Ends the session of a begin whose caller was cancelled before it received the result.
     *
     * Called on the cancelling thread or on the player's thread; the end itself runs in the
     * player's next tick.
     */
    private fun endUnclaimed(player: Player, unclaimed: TemporaryInventoryBeginResult) {
        val started = unclaimed as? TemporaryInventoryBeginResult.Started ?: return
        val session = started.session as ActiveTemporaryInventorySession
        val ticket = Job()

        dispatchers.entity(player).dispatch(ticket) {
            if (!ticket.isCancelled) {
                end(session)
            }
        }
    }

    private fun beginNow(
        player: Player,
        plugin: Plugin,
        key: Key,
        fill: (inventory: PlayerInventory, state: PersistentDataContainer) -> Unit,
    ): TemporaryInventoryBeginResult {
        if (player.isDead || !player.isConnected || !player.isValid || !plugin.isAdmitted()) {
            return TemporaryInventoryBeginResult.Unavailable
        }

        val entry = TemporaryInventoryStorage.read(player)
            ?: return TemporaryInventoryBeginResult.Unavailable
        val existing = entry.sessions.firstOrNull()

        if (existing != null) {
            return TemporaryInventoryBeginResult.AlreadyActive(Key.key(existing.key))
        }

        if (!player.storeLooseItems()) return TemporaryInventoryBeginResult.Unavailable

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
            rollBack(player, original, entry)

            throw failure
        }

        val active = ActiveTemporaryInventorySession(
            id = session.id,
            key = key,
            playerUuid = player.uniqueId,
            owner = plugin,
            service = this,
        )
        activeSessions[player.uniqueId] = active

        if (!plugin.isAdmitted()) {
            activeSessions.remove(player.uniqueId, active)
            rollBack(player, original, entry)

            return TemporaryInventoryBeginResult.Unavailable
        }

        return TemporaryInventoryBeginResult.Started(active)
    }

    private fun Plugin.isAdmitted(): Boolean = isEnabled && name !in closedOwners

    /**
     * Moves the loose items of the open view into the player's storage slots, so closing the view
     * hands nothing back, and returns `false` without changing anything when they do not fit.
     */
    private fun Player.storeLooseItems(): Boolean {
        val looseItems = looseItems()
        if (looseItems.isEmpty()) return true

        val insertion = planInsertion(looseItems).commit()
        if (insertion.leftover > 0) return false

        takeLooseItems()

        return true
    }

    private fun rollBack(player: Player, original: OriginalInventory, entry: StoredEntry) {
        original.restore(player.inventory)
        TemporaryInventoryStorage.write(player, entry)
    }

    /**
     * Ends the session that [select] picks from the player's stored entry, unless the player's
     * session is already ending.
     *
     * Drops the player's index entry when the stored entry is unreadable or holds no such session.
     */
    private inline fun endStored(
        player: Player,
        cause: TemporaryInventoryEndCause,
        discarded: List<ItemStack>,
        select: (StoredEntry) -> StoredSession?,
    ): Boolean {
        if (!endingPlayers.add(player.uniqueId)) return false

        try {
            val entry = TemporaryInventoryStorage.read(player)
            val session = entry?.let(select)

            if (entry == null || session == null) {
                activeSessions.remove(player.uniqueId)

                return false
            }

            endNow(player, entry, session, cause, discarded)

            return true
        } finally {
            endingPlayers.remove(player.uniqueId)
        }
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

        if (bytes.isNotEmpty()) {
            state.readFromBytes(bytes)
        }

        return state
    }

    private companion object {
        const val OFF_THREAD_END = "Ending a temporary inventory session off the player's thread"
        val HOTBAR_SLOTS = 0..8
    }
}
