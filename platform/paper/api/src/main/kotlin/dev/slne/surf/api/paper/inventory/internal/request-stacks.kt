package dev.slne.surf.api.paper.inventory.internal

import org.bukkit.inventory.ItemStack

/**
 * Item amounts split into stacks of at most each item's max stack size, for Bukkit's `addItem` and
 * `removeItem`, which key their leftovers by the index of the stack they were given.
 */
internal class RequestStacks(private val requests: List<ItemAmount>) {

    /**
     * The stacks of all requests, in request order.
     */
    val stacks: Array<ItemStack>

    /**
     * The index of the request each stack in [stacks] belongs to.
     */
    private val owners: IntArray

    init {
        val stacksPerRequest = requests.map { request ->
            request.item.splitIntoStacks(request.amount)
        }

        stacks = stacksPerRequest.flatMap { requestStacks -> requestStacks.asList() }.toTypedArray()
        owners = IntArray(stacks.size)

        var stackIndex = 0

        for ((requestIndex, requestStacks) in stacksPerRequest.withIndex()) {
            owners.fill(requestIndex, stackIndex, stackIndex + requestStacks.size)
            stackIndex += requestStacks.size
        }
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
 * Splits [amount] items similar to this item into stacks of at most its max stack size.
 */
private fun ItemStack.splitIntoStacks(amount: Int): Array<ItemStack> {
    val stackSize = maxStackSize
    val stackCount = (amount + stackSize - 1) / stackSize

    return Array(stackCount) { index ->
        asQuantity(minOf(stackSize, amount - index * stackSize))
    }
}
