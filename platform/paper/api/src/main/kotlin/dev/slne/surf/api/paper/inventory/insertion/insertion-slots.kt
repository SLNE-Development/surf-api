package dev.slne.surf.api.paper.inventory.insertion

import dev.slne.surf.api.paper.inventory.internal.ItemAmount
import dev.slne.surf.api.paper.inventory.internal.RequestStacks
import dev.slne.surf.api.paper.inventory.internal.detachedInventoryOf
import dev.slne.surf.api.paper.inventory.internal.retry
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * Inserts [requests] into the real inventory and returns the leftover stacks per request.
 *
 * With [includeOffhand] on a player inventory, the offhand takes what the storage slots leave
 * over, exactly as [copyOffhand] predicts it.
 */
internal fun Inventory.insert(
    requests: List<ItemAmount>,
    includeOffhand: Boolean,
): List<List<ItemStack>> {
    val offhand = copyOffhand(includeOffhand = includeOffhand)
    val leftovers = insertStacks(this, offhand, requests)

    if (offhand != null) {
        (this as PlayerInventory).setItemInOffHand(offhand.getItem(0))
    }

    return leftovers
}

/**
 * Adds [requests] to [storage] and what is left over to [offhand], both through Bukkit's own
 * `Inventory.addItem`, and returns the leftover stacks per request in request order.
 */
internal fun insertStacks(
    storage: Inventory,
    offhand: Inventory?,
    requests: List<ItemAmount>,
): List<List<ItemStack>> {
    val requestStacks = RequestStacks(requests)
    val storageLeftovers = storage.addItem(*requestStacks.stacks)
    val leftovers = if (offhand == null) {
        storageLeftovers
    } else {
        storageLeftovers.retry { stacks -> offhand.addItem(*stacks) }
    }

    return requestStacks.leftoversPerRequest(leftovers)
}

/**
 * Copies the storage slots of this inventory into a detached inventory with the same stack limit.
 */
internal fun Inventory.copyStorageSlots(): Inventory = copySlots(storageContents)

/**
 * Copies the offhand of this player inventory into a detached one-slot inventory, or returns
 * `null` when the offhand is out of scope.
 */
internal fun Inventory.copyOffhand(includeOffhand: Boolean): Inventory? {
    if (!includeOffhand || this !is PlayerInventory) return null

    return copySlots(arrayOf(itemInOffHand))
}

/**
 * Copies [slots] into a detached inventory with the stack limit of this inventory.
 *
 * The slots past the copied ones hold full stacks that accept nothing.
 */
private fun Inventory.copySlots(slots: Array<ItemStack?>): Inventory {
    val shadow = detachedInventoryOf(slots)

    shadow.maxStackSize = maxStackSize

    for (slot in slots.size until shadow.size) {
        shadow.setItem(slot, fullPaddingStack())
    }

    return shadow
}

private fun fullPaddingStack(): ItemStack {
    val padding = ItemStack.of(Material.BARRIER)

    return padding.asQuantity(padding.maxStackSize)
}
