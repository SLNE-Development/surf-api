package dev.slne.surf.api.paper.inventory.framework.view

import me.devnatan.inventoryframework.View
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayDeque

internal object ViewNavigationHistory {
    private val paths = ConcurrentHashMap<UUID, ArrayDeque<NavEntry>>()
    private val backNav = ConcurrentHashMap.newKeySet<UUID>()

    private fun deque(player: Player) = paths.computeIfAbsent(player.uniqueId) { ArrayDeque() }

    fun reset(player: Player, entry: NavEntry) {
        val deque = deque(player)
        deque.clear()
        deque.addLast(entry)
    }

    fun pushForward(player: Player, entry: NavEntry) = deque(player).addLast(entry)

    fun markBackNavigation(player: Player) = backNav.add(player.uniqueId)

    fun consumeBackNavigation(player: Player) = backNav.remove(player.uniqueId)

    fun popToPrevious(player: Player): NavEntry? {
        val deque = paths[player.uniqueId] ?: return null
        deque.removeLastOrNull()
        return deque.lastOrNull()
    }

    fun isPending(uuid: UUID): Boolean = backNav.contains(uuid)

    fun clear(player: Player) {
        paths.remove(player.uniqueId)
        backNav.remove(player.uniqueId)
    }

    internal data class NavEntry(val viewClass: Class<out View>, val data: Any?)
}