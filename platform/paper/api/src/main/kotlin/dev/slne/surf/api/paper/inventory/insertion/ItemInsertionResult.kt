package dev.slne.surf.api.paper.inventory.insertion

/**
 * The outcome of committing an [ItemInsertionPlan].
 *
 * @property inserted the number of items the commit put into the inventory
 * @property leftover the number of items the commit did not insert
 */
data class ItemInsertionResult(val inserted: Int, val leftover: Int)
