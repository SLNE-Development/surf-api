package dev.slne.surf.api.paper.inventory.insertion

import dev.slne.surf.api.paper.region.TickThreadGuard
import org.bukkit.entity.Entity
import org.bukkit.inventory.Inventory

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
