package dev.slne.surf.api.paper.inventory.insertion

/**
 * What committing an [ItemInsertionPlan] does when the inventory changed since planning and the
 * items no longer fit.
 */
enum class ItemInsertionPolicy {
    /**
     * Inserts nothing unless every item fits.
     */
    REJECT,
}
