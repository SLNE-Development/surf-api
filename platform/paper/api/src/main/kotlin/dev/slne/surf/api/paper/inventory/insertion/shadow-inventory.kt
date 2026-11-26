package dev.slne.surf.api.paper.inventory.insertion

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

private const val ROW_SIZE = 9

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
    val offhand = copyOffhand(includeOffhand)
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
    val owners = ArrayList<Int>()
    val stacks = ArrayList<ItemStack>()

    for ((index, request) in requests.withIndex()) {
        for (stack in request.item.splitIntoStacks(request.amount)) {
            owners += index
            stacks += stack
        }
    }

    val storageLeftovers = storage.addItem(*stacks.toTypedArray())
    val leftovers = offhand?.addLeftovers(storageLeftovers) ?: storageLeftovers
    val leftoversPerRequest = List(requests.size) { ArrayList<ItemStack>() }

    for ((stackIndex, stack) in leftovers) {
        leftoversPerRequest[owners[stackIndex]] += stack
    }

    return leftoversPerRequest
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
 * Splits [amount] items similar to this item into stacks of at most its max stack size.
 */
internal fun ItemStack.splitIntoStacks(amount: Int): Array<ItemStack> {
    val stackSize = maxStackSize
    val stackCount = (amount + stackSize - 1) / stackSize

    return Array(stackCount) { index ->
        asQuantity(minOf(stackSize, amount - index * stackSize))
    }
}

/**
 * Adds [leftovers], keyed by their index in an earlier `addItem` call, and returns what is still
 * left over under the same keys.
 */
private fun Inventory.addLeftovers(leftovers: Map<Int, ItemStack>): Map<Int, ItemStack> {
    val stackIndices = leftovers.keys.sorted()
    val remaining = addItem(*stackIndices.map(leftovers::getValue).toTypedArray())

    return remaining.mapKeys { (index, _) -> stackIndices[index] }
}

/**
 * Copies [slots] into a detached inventory with the stack limit of this inventory.
 *
 * Bukkit only creates inventories in whole rows, so the slots past the copied ones hold full
 * stacks that accept nothing.
 */
private fun Inventory.copySlots(slots: Array<ItemStack?>): Inventory {
    val rows = (slots.size + ROW_SIZE - 1) / ROW_SIZE
    val shadow = Bukkit.createInventory(null, rows.coerceAtLeast(1) * ROW_SIZE)

    shadow.maxStackSize = maxStackSize
    shadow.storageContents = slots

    for (slot in slots.size until shadow.size) {
        shadow.setItem(slot, fullPaddingStack())
    }

    return shadow
}

private fun fullPaddingStack(): ItemStack {
    val padding = ItemStack.of(Material.BARRIER)

    return padding.asQuantity(padding.maxStackSize)
}
