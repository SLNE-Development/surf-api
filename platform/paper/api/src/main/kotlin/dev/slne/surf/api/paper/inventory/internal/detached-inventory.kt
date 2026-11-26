package dev.slne.surf.api.paper.inventory.internal

import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

private const val ROW_SIZE = 9

/**
 * Copies [slots] into the first slots of a detached inventory and leaves the rest empty.
 *
 * Bukkit only creates inventories in whole rows, so the copy may have more slots than [slots].
 */
internal fun detachedInventoryOf(slots: Array<ItemStack?>): Inventory {
    val rows = (slots.size + ROW_SIZE - 1) / ROW_SIZE
    val shadow = Bukkit.createInventory(null, rows.coerceAtLeast(1) * ROW_SIZE)

    shadow.storageContents = slots

    return shadow
}
