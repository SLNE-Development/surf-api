package dev.slne.surf.api.paper.inventory.insertion

import org.bukkit.inventory.ItemStack

/**
 * The outcome of committing an [ItemInsertionPlan], in total and per item.
 *
 * Under [ItemInsertionPolicy.DROP], the leftover items are the ones dropped into the world.
 *
 * @property entries one entry per item of the plan, in the plan's order
 */
class ItemInsertionResult internal constructor(val entries: List<Entry>) {

    /**
     * The number of items the commit put into the inventory.
     */
    val inserted: Int
        get() = entries.sumOf { it.inserted }

    /**
     * The number of items the commit did not insert.
     */
    val leftover: Int
        get() = entries.sumOf { it.leftover }

    /**
     * Returns the number of items similar to [item] that the commit inserted.
     *
     * @throws IllegalArgumentException if the plan holds no item similar to [item]
     */
    fun insertedOf(item: ItemStack): Int {
        return entries.similarTo(item, Entry::item).inserted
    }

    /**
     * Returns the number of items similar to [item] that the commit did not insert.
     *
     * @throws IllegalArgumentException if the plan holds no item similar to [item]
     */
    fun leftoverOf(item: ItemStack): Int {
        return entries.similarTo(item, Entry::item).leftover
    }

    override fun toString(): String = "ItemInsertionResult(entries=$entries)"

    /**
     * The outcome for all items similar to [item].
     *
     * @property item a single item that stands for the entry; its amount carries no meaning
     * @property inserted the number of these items the commit put into the inventory
     * @property leftover the number of these items the commit did not insert
     */
    data class Entry(val item: ItemStack, val inserted: Int, val leftover: Int)
}
