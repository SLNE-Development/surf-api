package dev.slne.surf.api.minestom.inventory.framework.view

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import me.devnatan.inventoryframework.View
import net.minestom.server.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque

/**
 * Per-player stack of the views opened through back-navigable navigation.
 *
 * [markBackNavigation] flags a pending back navigation so that opening or closing the target view
 * neither pushes nor clears the stack; [consumeBackNavigation] resets the flag.
 */
internal object ViewNavigationHistory {
    private val paths = ConcurrentHashMap<UUID, ArrayDeque<NavEntry>>()
    private val backNav = ConcurrentHashMap.newKeySet<UUID>()

    private fun deque(player: Player) = paths.computeIfAbsent(player.uuid) { ArrayDeque() }

    fun reset(player: Player, entry: NavEntry) {
        val deque = deque(player)
        deque.clear()
        deque.addLast(entry)
    }

    fun pushForward(player: Player, entry: NavEntry): Unit = deque(player).addLast(entry)

    fun markBackNavigation(player: Player): Boolean = backNav.add(player.uuid)

    fun consumeBackNavigation(player: Player): Boolean = backNav.remove(player.uuid)

    fun popToPrevious(player: Player): NavEntry? {
        val deque = paths[player.uuid] ?: return null

        deque.removeLastOrNull()

        return deque.lastOrNull()
    }

    fun isPending(uuid: UUID): Boolean = backNav.contains(uuid)

    fun clear(player: Player) {
        paths.remove(player.uuid)
        backNav.remove(player.uuid)
    }

    internal data class NavEntry(val viewClass: Class<out View>, val data: Any?)
}

/**
 * Forgets the views [player] navigated through; called by the server once they disconnect.
 */
@InternalSurfApi
fun clearViewNavigationHistory(player: Player): Unit = ViewNavigationHistory.clear(player)
