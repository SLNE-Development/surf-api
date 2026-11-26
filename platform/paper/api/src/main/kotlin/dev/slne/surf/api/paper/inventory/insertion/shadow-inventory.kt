package dev.slne.surf.api.paper.inventory.insertion

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

private const val ROW_SIZE = 9

/**
 * Copies the storage slots of this inventory into a detached inventory with the same stack limit.
 *
 * Bukkit only creates inventories in whole rows, so the slots past the copied ones hold full
 * stacks that accept nothing.
 */
internal fun Inventory.copyStorageSlots(): Inventory {
    val storage = storageContents
    val rows = (storage.size + ROW_SIZE - 1) / ROW_SIZE
    val shadow = Bukkit.createInventory(null, rows.coerceAtLeast(1) * ROW_SIZE)

    shadow.maxStackSize = maxStackSize
    shadow.storageContents = storage

    for (slot in storage.size until shadow.size) {
        shadow.setItem(slot, fullPaddingStack())
    }

    return shadow
}

/**
 * Splits [amount] items similar to this item into stacks of at most its max stack size.
 */
internal fun ItemStack.splitIntoStacks(amount: Int): Array<ItemStack> {
    val stackSize = maxStackSize
    val stackCount = (amount + stackSize - 1) / stackSize

    return Array(stackCount) { index ->
        asQuantity(minOf(stackSize, amount - index * stackSize))
    }
}

private fun fullPaddingStack(): ItemStack {
    val padding = ItemStack.of(Material.BARRIER)

    return padding.asQuantity(padding.maxStackSize)
}
