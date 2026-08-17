package dev.slne.surf.api.minestom.inventory.framework.view

import me.devnatan.inventoryframework.View
import net.minestom.server.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque

internal object ViewNavigationHistory {
    private val paths = ConcurrentHashMap<UUID, ArrayDeque<NavEntry>>()
    private val backNav = ConcurrentHashMap.newKeySet<UUID>()

    private fun deque(player: Player) = paths.computeIfAbsent(player.uuid) { ArrayDeque() }

    fun reset(player: Player, entry: NavEntry) {
        val deque = deque(player)
        deque.clear()
        deque.addLast(entry)
    }

    fun pushForward(player: Player, entry: NavEntry) = deque(player).addLast(entry)

    fun markBackNavigation(player: Player) = backNav.add(player.uuid)

    fun consumeBackNavigation(player: Player) = backNav.remove(player.uuid)

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
