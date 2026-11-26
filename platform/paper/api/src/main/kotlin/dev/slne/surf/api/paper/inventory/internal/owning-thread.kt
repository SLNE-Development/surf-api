package dev.slne.surf.api.paper.inventory.internal

import dev.slne.surf.api.paper.coroutine.PaperDispatchersElement
import dev.slne.surf.api.paper.coroutine.withEntity
import dev.slne.surf.api.paper.region.TickThreadGuard
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.PlayerInventory

/**
 * Throws [IllegalStateException] unless the current thread owns this inventory.
 *
 * The thread of the holding entity owns an entity's inventory, and the region thread of its
 * location owns a block's inventory. An inventory with neither belongs to no region, so every tick
 * thread owns it.
 */
internal fun Inventory.ensureOwningThread(reason: String) {
    val guard = TickThreadGuard.instance
    val holder = getHolder(false)
    val location = location

    when {
        holder is Entity -> guard.ensureTickThread(holder, reason)
        location != null -> guard.ensureTickThread(location.world, location, reason)
        else -> check(Bukkit.isPrimaryThread()) { "Not on a tick thread: $reason" }
    }
}

/**
 * Runs [action] on a thread that [ensureOwningThread] accepts for this inventory and returns its
 * result.
 *
 * The switch goes through the `PaperDispatchers` of the calling plugin. The holder of a block
 * inventory is resolved only on the region thread, because resolving it reads the world. An
 * inventory that belongs to no region runs [action] in place on a tick thread and on the global
 * region thread otherwise. [action] does not suspend, so it runs on the owning thread in one piece.
 * If the coroutine is cancelled before [action] starts, [action] never runs.
 *
 * @throws IllegalStateException if the calling coroutine does not belong to a plugin that runs on
 * surf-api
 * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled before [action]
 * starts, or the entity holding the inventory is removed first
 */
internal suspend fun <R> Inventory.onOwningThread(action: () -> R): R {
    val context = currentCoroutineContext()
    val dispatchers = context[PaperDispatchersElement]?.dispatchers
        ?: error("The calling coroutine does not belong to a surf plugin.")

    context.ensureActive()

    val player = (this as? PlayerInventory)?.holder
    if (player != null) return player.withEntity { action() }

    val location = location

    if (location == null) {
        val holder = getHolder(false)

        return when {
            holder is Entity -> holder.withEntity { action() }
            Bukkit.isPrimaryThread() -> action()
            else -> withContext(dispatchers.main) { action() }
        }
    }

    return withContext(dispatchers.region(location)) {
        val holder = getHolder(false)

        if (holder is Entity) holder.withEntity { action() } else action()
    }
}
