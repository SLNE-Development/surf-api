package dev.slne.surf.api.paper.server.impl.inventory.temporary

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.*

/**
 * The items the server hands back to the player when the open view closes: the item on the
 * cursor and the inputs of a workstation menu, such as a crafting grid, an anvil or a merchant.
 *
 * Only the menus of the workstations themselves hand their inputs back. An inventory a plugin
 * creates with a workstation's [InventoryType] keeps its contents and is left alone.
 */
internal fun Player.looseItems(): List<ItemStack> {
    val items = mutableListOf<ItemStack>()

    if (!itemOnCursor.isEmpty) {
        items += itemOnCursor.clone()
    }

    val top = openInventory.topInventory

    for (slot in workstationInputSlots()) {
        val item = top.getItem(slot) ?: continue
        if (item.isEmpty) continue

        items += item.clone()
    }

    return items
}

/**
 * Removes the [looseItems] from the cursor and the workstation inputs and returns them.
 */
internal fun Player.takeLooseItems(): List<ItemStack> {
    val items = looseItems()

    if (!itemOnCursor.isEmpty) {
        setItemOnCursor(null)
    }

    val top = openInventory.topInventory

    for (slot in workstationInputSlots()) {
        val item = top.getItem(slot)

        if (item != null && !item.isEmpty) {
            top.setItem(slot, null)
        }
    }

    return items
}

/**
 * The input slots of the open workstation menu, in the slot order of its top inventory.
 *
 * A crafting grid lists its inputs after the result slot. The other workstations are read through
 * the view's slot types, which do not cover the player's own grid in creative mode.
 */
private fun Player.workstationInputSlots(): List<Int> {
    val view = openInventory
    val top = view.topInventory

    if (top is CraftingInventory) {
        return List(top.matrix.size) { input -> CRAFTING_RESULT_SLOTS + input }
    }

    if (!top.isWorkstation()) return emptyList()

    return (0 until top.size)
        .filter { slot -> view.getSlotType(slot) == InventoryType.SlotType.CRAFTING }
}

private const val CRAFTING_RESULT_SLOTS = 1

private fun Inventory.isWorkstation(): Boolean {
    return this is AnvilInventory ||
        this is SmithingInventory ||
        this is GrindstoneInventory ||
        this is StonecutterInventory ||
        this is LoomInventory ||
        this is CartographyInventory ||
        this is EnchantingInventory ||
        this is MerchantInventory ||
        this is BeaconInventory
}
