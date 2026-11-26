package dev.slne.surf.api.paper.inventory.internal

import org.bukkit.inventory.ItemStack

/**
 * A number of items similar to [item], whose own amount is always one.
 */
internal class ItemAmount private constructor(val item: ItemStack, val amount: Int) {

    companion object {
        fun of(item: ItemStack, amount: Int): ItemAmount {
            require(!item.isEmpty) { "The item must not be empty." }
            require(amount > 0) { "The item amount must be positive, but was $amount." }

            return ItemAmount(item.asOne(), amount)
        }
    }
}

/**
 * Converts [items] into item amounts, each stack counting its own amount.
 */
internal fun itemAmountsOf(items: Collection<ItemStack>): List<ItemAmount> {
    return mergeSimilar(items.map { item -> ItemAmount.of(item, item.amount) })
}

/**
 * Converts [items] into item amounts, each key counting the amount it maps to.
 */
internal fun itemAmountsOf(items: Map<ItemStack, Int>): List<ItemAmount> {
    return mergeSimilar(items.map { (item, amount) -> ItemAmount.of(item, amount) })
}

/**
 * Returns the entry whose item, selected by [itemOf], is similar to [item].
 *
 * @throws IllegalArgumentException if no entry matches
 */
internal inline fun <T> List<T>.similarTo(item: ItemStack, itemOf: (T) -> ItemStack): T {
    return requireNotNull(find { entry -> itemOf(entry).isSimilar(item) }) {
        "No entry holds an item similar to $item."
    }
}

/**
 * Sums the amounts of similar items, keeping the order in which each item first appears.
 */
private fun mergeSimilar(amounts: List<ItemAmount>): List<ItemAmount> {
    val merged = ArrayList<ItemAmount>(amounts.size)

    for (amount in amounts) {
        val index = merged.indexOfFirst { it.item.isSimilar(amount.item) }

        if (index == -1) {
            merged += amount
        } else {
            merged[index] = ItemAmount.of(amount.item, merged[index].amount + amount.amount)
        }
    }

    return merged
}
