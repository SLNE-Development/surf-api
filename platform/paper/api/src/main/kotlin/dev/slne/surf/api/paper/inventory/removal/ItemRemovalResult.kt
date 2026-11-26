package dev.slne.surf.api.paper.inventory.removal

import dev.slne.surf.api.paper.inventory.internal.similarTo
import org.bukkit.inventory.ItemStack

/**
 * The outcome of committing an [ItemRemovalPlan], in total and per item.
 *
 * @property entries one entry per item of the plan, in the plan's order
 */
class ItemRemovalResult internal constructor(val entries: List<Entry>) {

    /**
     * The number of items the commit took out of the inventory.
     */
    val removed: Int
        get() = entries.sumOf { it.removed }

    /**
     * The number of items the commit did not remove.
     */
    val missing: Int
        get() = entries.sumOf { it.missing }

    /**
     * Returns the number of items similar to [item] that the commit removed.
     *
     * @throws IllegalArgumentException if the plan holds no item similar to [item]
     */
    fun removedOf(item: ItemStack): Int {
        return entries.similarTo(item, Entry::item).removed
    }

    /**
     * Returns the number of items similar to [item] that the commit did not remove.
     *
     * @throws IllegalArgumentException if the plan holds no item similar to [item]
     */
    fun missingOf(item: ItemStack): Int {
        return entries.similarTo(item, Entry::item).missing
    }

    override fun toString(): String = "ItemRemovalResult(entries=$entries)"

    /**
     * The outcome for all items similar to [item].
     *
     * @property item a single item that stands for the entry; its amount carries no meaning
     * @property removed the number of these items the commit took out of the inventory
     * @property missing the number of these items the commit did not remove
     */
    class Entry internal constructor(val item: ItemStack, val removed: Int, val missing: Int) {

        override fun toString(): String {
            return "Entry(item=$item, removed=$removed, missing=$missing)"
        }
    }
}
