package dev.slne.surf.api.paper.inventory.insertion

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

private const val OFF_THREAD_REASON = "Item insertion off the thread that owns the inventory"

/**
 * A preview of inserting a set of items into [inventory], for example to tell a player in advance
 * whether a reward, a kit or a purchase fits.
 *
 * The set fits only when all its items fit together: two items that each fit alone may compete for
 * the same empty slots. Similar items, as Bukkit's `ItemStack.isSimilar` defines them, form one
 * entry.
 *
 * The plan describes the inventory at the moment it was made and never updates. [commit] works on
 * the current contents and follows an [ItemInsertionPolicy], so it stays safe when the inventory
 * changed in between.
 *
 * Planning runs Bukkit's own `Inventory.addItem` on a copy of the slots in scope. On an unchanged
 * inventory, [commit] therefore inserts exactly what the plan predicts. On a player inventory, only
 * the storage slots are in scope, plus the offhand when the plan includes it: armor and crafting
 * grid stay untouched. The offhand only takes what the storage slots leave over.
 *
 * Planning and committing must run on the thread that owns the inventory: the thread of the
 * holding entity, the region thread of the inventory's location, or the global tick thread for an
 * inventory with neither. On any other thread they throw [IllegalStateException].
 *
 * @property entries one entry per set of similar items, in the order they were first given
 */
class ItemInsertionPlan internal constructor(
    val inventory: Inventory,
    val entries: List<Entry>,
    private val includeOffhand: Boolean,
) {

    /**
     * The number of items to insert.
     */
    val amount: Int
        get() = entries.sumOf { it.amount }

    /**
     * The number of items that would not fit.
     */
    val leftover: Int
        get() = entries.sumOf { it.leftover }

    /**
     * Whether all items fit together.
     */
    val fits: Boolean
        get() = leftover == 0

    /**
     * Returns the number of items similar to [item] that would not fit.
     *
     * @throws IllegalArgumentException if the plan holds no item similar to [item]
     */
    fun leftoverOf(item: ItemStack): Int {
        return entries.similarTo(item, Entry::item).leftover
    }

    /**
     * Inserts the items into the current inventory and applies [policy] to what no longer fits.
     *
     * @throws IllegalStateException off the thread that owns the inventory, or under
     * [ItemInsertionPolicy.DROP] when the inventory has no location
     */
    fun commit(policy: ItemInsertionPolicy = ItemInsertionPolicy.REJECT): ItemInsertionResult {
        inventory.ensureOwningThread(OFF_THREAD_REASON)

        val requests = entries.map { ItemAmount.of(it.item, it.amount) }
        val dropTarget = if (policy == ItemInsertionPolicy.DROP) inventory.dropTarget() else null

        if (policy == ItemInsertionPolicy.REJECT && !inventory.fitsNow(requests)) {
            return ItemInsertionResult(entries.map(::rejectedEntry))
        }

        val leftovers = inventory.insert(requests, includeOffhand)

        dropTarget?.drop(leftovers.flatten())

        return ItemInsertionResult(requests.zip(leftovers, ::resultEntry))
    }

    private fun Inventory.fitsNow(requests: List<ItemAmount>): Boolean {
        return planItemInsertion(requests, includeOffhand).fits
    }

    override fun toString(): String {
        return "ItemInsertionPlan(entries=$entries, includeOffhand=$includeOffhand)"
    }

    /**
     * The planned insertion of all items similar to [item].
     *
     * @property item a single item that stands for the entry; its amount carries no meaning
     * @property amount the number of these items to insert
     * @property leftover the number of these items that would not fit
     */
    data class Entry(val item: ItemStack, val amount: Int, val leftover: Int) {

        /**
         * Whether all [amount] items of this entry fit.
         */
        val fits: Boolean
            get() = leftover == 0
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
    return planItemInsertion(listOf(ItemAmount.of(item, amount)), includeOffhand = false)
}

/**
 * Plans the insertion of [items] into this inventory, each in its own amount.
 *
 * @throws IllegalArgumentException if one of [items] is empty
 * @throws IllegalStateException off the thread that owns the inventory
 * @see ItemInsertionPlan
 */
fun Inventory.planInsertion(vararg items: ItemStack): ItemInsertionPlan {
    return planInsertion(items.asList())
}

/**
 * Plans the insertion of [items] into this inventory, each in its own amount.
 *
 * An empty collection fits.
 *
 * @throws IllegalArgumentException if one of [items] is empty
 * @throws IllegalStateException off the thread that owns the inventory
 * @see ItemInsertionPlan
 */
fun Inventory.planInsertion(items: Collection<ItemStack>): ItemInsertionPlan {
    return planItemInsertion(itemAmountsOf(items), includeOffhand = false)
}

/**
 * Plans the insertion of [items] into this inventory, each key in the amount it maps to.
 *
 * The amount of each key itself is ignored. An empty map fits.
 *
 * @throws IllegalArgumentException if a key is empty or an amount is not positive
 * @throws IllegalStateException off the thread that owns the inventory
 * @see ItemInsertionPlan
 */
fun Inventory.planInsertion(items: Map<ItemStack, Int>): ItemInsertionPlan {
    return planItemInsertion(itemAmountsOf(items), includeOffhand = false)
}

/**
 * Plans the insertion of [amount] items similar to [item] into the storage slots of this player.
 *
 * @param includeOffhand whether the offhand takes what the storage slots leave over
 * @see Inventory.planInsertion
 */
fun Player.planInsertion(
    item: ItemStack,
    amount: Int,
    includeOffhand: Boolean = false,
): ItemInsertionPlan {
    return inventory.planItemInsertion(listOf(ItemAmount.of(item, amount)), includeOffhand)
}

/**
 * Plans the insertion of [items], each in its own amount, into the storage slots of this player.
 *
 * @param includeOffhand whether the offhand takes what the storage slots leave over
 * @see Inventory.planInsertion
 */
fun Player.planInsertion(
    vararg items: ItemStack,
    includeOffhand: Boolean = false,
): ItemInsertionPlan {
    return planInsertion(items.asList(), includeOffhand)
}

/**
 * Plans the insertion of [items], each in its own amount, into the storage slots of this player.
 *
 * @param includeOffhand whether the offhand takes what the storage slots leave over
 * @see Inventory.planInsertion
 */
fun Player.planInsertion(
    items: Collection<ItemStack>,
    includeOffhand: Boolean = false,
): ItemInsertionPlan {
    return inventory.planItemInsertion(itemAmountsOf(items), includeOffhand)
}

/**
 * Plans the insertion of [items], each key in the amount it maps to, into the storage slots of
 * this player.
 *
 * @param includeOffhand whether the offhand takes what the storage slots leave over
 * @see Inventory.planInsertion
 */
fun Player.planInsertion(
    items: Map<ItemStack, Int>,
    includeOffhand: Boolean = false,
): ItemInsertionPlan {
    return inventory.planItemInsertion(itemAmountsOf(items), includeOffhand)
}

private fun Inventory.planItemInsertion(
    requests: List<ItemAmount>,
    includeOffhand: Boolean,
): ItemInsertionPlan {
    ensureOwningThread(OFF_THREAD_REASON)

    val leftovers = insertStacks(copyStorageSlots(), copyOffhand(includeOffhand), requests)
    val entries = requests.zip(leftovers) { request, stacks ->
        ItemInsertionPlan.Entry(
            item = request.item,
            amount = request.amount,
            leftover = stacks.sumOf { it.amount },
        )
    }

    return ItemInsertionPlan(this, entries, includeOffhand)
}

private fun resultEntry(request: ItemAmount, leftover: List<ItemStack>): ItemInsertionResult.Entry {
    val leftoverAmount = leftover.sumOf { it.amount }

    return ItemInsertionResult.Entry(
        item = request.item,
        inserted = request.amount - leftoverAmount,
        leftover = leftoverAmount,
    )
}

private fun rejectedEntry(entry: ItemInsertionPlan.Entry): ItemInsertionResult.Entry {
    return ItemInsertionResult.Entry(item = entry.item, inserted = 0, leftover = entry.amount)
}
