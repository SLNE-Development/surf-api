package dev.slne.surf.api.paper.inventory.insertion

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

/**
 * Where [ItemInsertionPolicy.DROP] puts the items that did not fit, and who owns them.
 */
internal class DropTarget(private val location: Location, private val owner: UUID?) {

    fun drop(stacks: List<ItemStack>) {
        for (stack in stacks) {
            location.world.dropItem(location, stack) { item ->
                item.owner = owner
                item.isUnlimitedLifetime = true
            }
        }
    }
}

/**
 * Returns the drop target of this inventory: its player with the player as owner, or else the
 * center of its location without an owner.
 *
 * @throws IllegalStateException if the inventory has neither a player holder nor a location
 */
internal fun Inventory.dropTarget(): DropTarget {
    val holder = getHolder(false)

    if (holder is Player) return DropTarget(holder.location, holder.uniqueId)

    val location = checkNotNull(location) { "The inventory has no location to drop items at." }

    return DropTarget(location.toCenterLocation(), owner = null)
}
