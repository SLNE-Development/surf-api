package dev.slne.surf.api.paper.inventory.temporary

import net.kyori.adventure.key.Key
import org.bukkit.entity.Player
import org.bukkit.inventory.PlayerInventory
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.plugin.Plugin

/**
 * Begins a temporary inventory session for this player: stores the original inventory and lets
 * [fill] hand out the temporary inventory.
 *
 * The begin runs on the thread that owns the player, in the player's next tick, also when called
 * on that thread. A begin from an inventory click therefore runs after the click took effect. It
 * then runs in one piece:
 *
 * 1. It moves the cursor item and the inputs of an open workstation menu, such as the crafting
 *    grid or an anvil, into the inventory and closes the open view, so those items belong to the
 *    original inventory. If they do not fit, nothing changes and the result is
 *    [TemporaryInventoryBeginResult.Unavailable].
 * 2. It stores every slot of the inventory, armor, offhand, body armor and saddle included, and
 *    the selected hotbar slot in the player's persistent data container. If one item cannot be
 *    stored, nothing else changes and the result is [TemporaryInventoryBeginResult.Unstorable].
 * 3. It empties the inventory and runs [fill] with it and with an empty persistent data container
 *    for the plugin's own session state, such as a location to return to. The container is stored
 *    with the original inventory and reaches [TemporaryInventoryEndEvent.state].
 *
 * If [fill] throws, the original inventory is put back, nothing stays stored, and the exception
 * is rethrown. If the coroutine is cancelled before the begin runs, nothing changes. If it is
 * cancelled after the begin ran, the session ends again with [TemporaryInventoryEndCause.ENDED]
 * in the player's next tick, and the end event fires.
 *
 * A plugin that is disabled, or whose disable has begun, cannot begin a session.
 *
 * The plugin keeps everything outside the inventory to itself: ender chest, experience, health,
 * effects, game mode and flight. While the session runs, it also keeps the temporary items where
 * they belong, for example by preventing drops, container access and pickups.
 *
 * ```kotlin
 * val result = player.beginTemporaryInventorySession(plugin, EDIT_MODE) { inventory, state ->
 *     inventory.setItem(0, editTool)
 *     state.set(RETURN_WORLD, PersistentDataType.STRING, player.world.name)
 * }
 *
 * when (result) {
 *     is TemporaryInventoryBeginResult.Started -> sessions[player.uniqueId] = result.session
 *     is TemporaryInventoryBeginResult.AlreadyActive -> player.sendMessage(busyMessage)
 *     is TemporaryInventoryBeginResult.Unstorable -> player.sendMessage(unstorableMessage)
 *     TemporaryInventoryBeginResult.Unavailable -> Unit
 * }
 * ```
 *
 * @param plugin the plugin that owns the session
 * @param key the kind of session, which listeners and [TemporaryInventoryBeginResult.AlreadyActive]
 * report
 * @param fill hands out the temporary inventory; runs on the player's thread and must not suspend
 */
suspend fun Player.beginTemporaryInventorySession(
    plugin: Plugin,
    key: Key,
    fill: (inventory: PlayerInventory, state: PersistentDataContainer) -> Unit,
): TemporaryInventoryBeginResult {
    return TemporaryInventorySessions.instance.begin(this, plugin, key, fill)
}

/**
 * The player's active temporary inventory session, or `null`; readable from any thread.
 */
val Player.temporaryInventorySession: TemporaryInventorySession?
    get() = TemporaryInventorySessions.instance.activeSession(uniqueId)
