package dev.slne.surf.api.paper.inventory.insertion

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * A preview of inserting [amount] items similar to an item into [inventory], for example to tell
 * a player in advance whether a reward or a purchase fits.
 *
 * The plan describes the inventory at the moment it was made and never updates. [commit] plans
 * again against the current contents, so it stays safe when the inventory changed in between.
 *
 * Planning runs Bukkit's own `Inventory.addItem` on a copy of the slots in scope. On an unchanged
 * inventory, [commit] therefore inserts exactly what the plan predicts. On a player inventory, only
 * the storage slots are in scope: armor, offhand and crafting grid stay untouched.
 *
 * Planning and committing must run on the thread that owns the inventory: the thread of the
 * holding entity, the region thread of the inventory's location, or the global tick thread for an
 * inventory with neither. On any other thread they throw [IllegalStateException].
 */
class ItemInsertionPlan internal constructor(
    val inventory: Inventory,
    private val item: ItemStack,
    val amount: Int,
    val leftover: Int,
) {

    /**
     * Whether all [amount] items fit.
     */
    val fits: Boolean
        get() = leftover == 0

    /**
     * Plans the insertion again against the current inventory and applies it under [policy].
     *
     * @throws IllegalStateException off the thread that owns the inventory
     */
    fun commit(policy: ItemInsertionPolicy = ItemInsertionPolicy.REJECT): ItemInsertionResult {
        val current = inventory.planInsertion(item, amount)

        when (policy) {
            ItemInsertionPolicy.REJECT -> {
                if (!current.fits) return ItemInsertionResult(inserted = 0, leftover = amount)
            }
        }

        val leftover = inventory.addItem(*item.splitIntoStacks(amount))
            .values
            .sumOf { it.amount }

        return ItemInsertionResult(inserted = amount - leftover, leftover = leftover)
    }
}

/**
 * Plans the insertion of [amount] items similar to [item] into this inventory.
 *
 * The amount of [item] itself is ignored; [amount] may exceed one stack. Items are stacked by
 * Bukkit's `ItemStack.isSimilar` and up to each item's own max stack size.
 *
 * @throws IllegalArgumentException if [item] is empty or [amount] is not positive
 * @throws IllegalStateException off the thread that owns the inventory
 */
fun Inventory.planInsertion(item: ItemStack, amount: Int): ItemInsertionPlan {
    require(!item.isEmpty) { "An empty item cannot be inserted." }
    require(amount > 0) { "The amount to insert must be positive, but was $amount." }
    ensureOwningThread("Item insertion off the thread that owns the inventory")

    val leftover = copyStorageSlots()
        .addItem(*item.splitIntoStacks(amount))
        .values
        .sumOf { it.amount }

    return ItemInsertionPlan(this, item.clone(), amount, leftover)
}

/**
 * Plans the insertion of [amount] items similar to [item] into the storage slots of this player.
 *
 * @see Inventory.planInsertion
 */
fun Player.planInsertion(item: ItemStack, amount: Int): ItemInsertionPlan {
    return inventory.planInsertion(item, amount)
}
