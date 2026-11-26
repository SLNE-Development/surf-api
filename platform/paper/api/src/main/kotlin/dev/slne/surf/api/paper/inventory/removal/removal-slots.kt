package dev.slne.surf.api.paper.inventory.removal

import dev.slne.surf.api.paper.inventory.internal.ItemAmount
import dev.slne.surf.api.paper.inventory.internal.RequestStacks
import dev.slne.surf.api.paper.inventory.internal.detachedInventoryOf
import dev.slne.surf.api.paper.inventory.internal.retry
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

/**
 * The slots of a player inventory that an item removal may take from besides the storage slots.
 */
internal data class RemovalScope(val includeOffhand: Boolean, val includeArmor: Boolean) {

    companion object {
        val STORAGE = RemovalScope(includeOffhand = false, includeArmor = false)
    }
}

/**
 * Copies the slots in [scope] into a detached inventory whose storage slots hold all of them.
 */
internal fun Inventory.copySlotsInScope(scope: RemovalScope): Inventory {
    val slots = storageContents.asList() + extraSlotGroups(scope).flatMap { it.read().asList() }

    return detachedInventoryOf(slots.toTypedArray())
}

/**
 * Removes [requests] through Bukkit's own `Inventory.removeItem`, from the storage slots first and
 * then from the further slot groups in [scope], and returns the missing amount per request.
 */
internal fun Inventory.removeInScope(requests: List<ItemAmount>, scope: RemovalScope): List<Int> {
    val requestStacks = RequestStacks(requests)
    var leftovers: Map<Int, ItemStack> = removeItem(*requestStacks.stacks)

    for (group in extraSlotGroups(scope)) {
        leftovers = leftovers.retry(group::remove)
    }

    return requestStacks.leftoversPerRequest(leftovers).map { stacks -> stacks.sumOf { it.amount } }
}

/**
 * Slots of a player inventory outside the storage slots, read and written as one array.
 */
private class SlotGroup(
    val read: () -> Array<ItemStack?>,
    private val write: (Array<ItemStack?>) -> Unit,
) {

    fun remove(stacks: Array<ItemStack>): Map<Int, ItemStack> {
        val slots = read()
        val shadow = detachedInventoryOf(slots)
        val leftovers = shadow.removeItem(*stacks)

        write(shadow.storageContents.copyOf(slots.size))

        return leftovers
    }
}

private fun Inventory.extraSlotGroups(scope: RemovalScope): List<SlotGroup> {
    if (this !is PlayerInventory) return emptyList()

    return buildList {
        if (scope.includeOffhand) {
            add(
                SlotGroup(
                    read = { arrayOf(itemInOffHand) },
                    write = { slots -> setItemInOffHand(slots[0]) },
                ),
            )
        }

        if (scope.includeArmor) {
            add(SlotGroup(read = { armorContents }, write = { slots -> armorContents = slots }))
        }
    }
}
