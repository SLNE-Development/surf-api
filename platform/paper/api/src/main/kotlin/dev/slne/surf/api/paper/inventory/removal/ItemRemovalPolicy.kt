package dev.slne.surf.api.paper.inventory.removal

/**
 * What committing an [ItemRemovalPlan] does when the inventory changed since planning and no
 * longer holds enough of the items.
 */
enum class ItemRemovalPolicy {
    /**
     * Removes nothing unless the inventory holds every item in its full amount.
     */
    REJECT,

    /**
     * Removes what the inventory holds, up to each item's amount.
     */
    PARTIAL,
}
