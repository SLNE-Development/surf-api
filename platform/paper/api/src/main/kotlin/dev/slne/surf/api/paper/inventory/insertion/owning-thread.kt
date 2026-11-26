package dev.slne.surf.api.paper.inventory.insertion

import dev.slne.surf.api.paper.coroutine.PaperDispatchersElement
import dev.slne.surf.api.paper.coroutine.withEntity
import dev.slne.surf.api.paper.region.TickThreadGuard
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.bukkit.entity.Entity
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.PlayerInventory

/**
 * Throws [IllegalStateException] unless the current thread owns this inventory.
 */
internal fun Inventory.ensureOwningThread(reason: String) {
    val guard = TickThreadGuard.instance
    val holder = getHolder(false)
    val location = location

    when {
        holder is Entity -> guard.ensureTickThread(holder, reason)
        location != null -> guard.ensureTickThread(location.world, location, reason)
        else -> guard.ensureGlobalTickThread(reason)
    }
}

/**
 * Runs [action] on the thread that [ensureOwningThread] accepts for this inventory and returns its
 * result.
 *
 * The switch goes through the `PaperDispatchers` of the calling plugin. The holder of a block
 * inventory is resolved only on the region thread, because resolving it reads the world. [action]
 * does not suspend, so it runs on the owning thread in one piece. If the coroutine is cancelled
 * before [action] starts, [action] never runs.
 *
 * @throws IllegalStateException if the calling coroutine does not belong to a plugin that runs on
 * surf-api
 * @throws kotlinx.coroutines.CancellationException if the coroutine is cancelled before [action]
 * starts, or the entity holding the inventory is removed first
 */
internal suspend fun <R> Inventory.onOwningThread(action: () -> R): R {
    val context = currentCoroutineContext()
    val dispatchers = context[PaperDispatchersElement]?.dispatchers
        ?: error(
            "Switching to the thread that owns an inventory needs a coroutine of a surf plugin, " +
                "such as one of its scope or its lifecycle hooks",
        )

    context.ensureActive()

    val player = (this as? PlayerInventory)?.holder
    if (player != null) return player.withEntity { action() }

    val location = location
    if (location == null) {
        val holder = getHolder(false)

        return if (holder is Entity) {
            holder.withEntity { action() }
        } else {
            withContext(dispatchers.main) { action() }
        }
    }

    return withContext(dispatchers.region(location)) {
        val holder = getHolder(false)

        if (holder is Entity) holder.withEntity { action() } else action()
    }
}
