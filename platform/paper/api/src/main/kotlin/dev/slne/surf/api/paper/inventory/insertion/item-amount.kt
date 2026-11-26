package dev.slne.surf.api.paper.inventory.insertion

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
        "The plan holds no item similar to $item."
    }
}

/**
 * Item amounts split into stacks of at most each item's max stack size, for Bukkit's `addItem` and
 * `removeItem`, which key their leftovers by the index of the stack they were given.
 */
internal class RequestStacks(private val requests: List<ItemAmount>) {
    private val owners = ArrayList<Int>()

    /**
     * The stacks of all requests, in request order.
     */
    val stacks: Array<ItemStack>

    init {
        val split = ArrayList<ItemStack>()

        for ((index, request) in requests.withIndex()) {
            for (stack in request.item.splitIntoStacks(request.amount)) {
                owners += index
                split += stack
            }
        }

        stacks = split.toTypedArray()
    }

    /**
     * Groups [leftovers], keyed by stack index, by the request each stack belongs to.
     */
    fun leftoversPerRequest(leftovers: Map<Int, ItemStack>): List<List<ItemStack>> {
        val leftoversPerRequest = List(requests.size) { ArrayList<ItemStack>() }

        for ((stackIndex, stack) in leftovers) {
            leftoversPerRequest[owners[stackIndex]] += stack
        }

        return leftoversPerRequest
    }
}

/**
 * Hands these leftovers, keyed by their index in an earlier `addItem` or `removeItem` call, to
 * [operation] and returns what is still left over under the same keys.
 */
internal fun Map<Int, ItemStack>.retry(
    operation: (Array<ItemStack>) -> Map<Int, ItemStack>,
): Map<Int, ItemStack> {
    if (isEmpty()) return this

    val stackIndices = keys.sorted()
    val remaining = operation(stackIndices.map(::getValue).toTypedArray())

    return remaining.mapKeys { (index, _) -> stackIndices[index] }
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
