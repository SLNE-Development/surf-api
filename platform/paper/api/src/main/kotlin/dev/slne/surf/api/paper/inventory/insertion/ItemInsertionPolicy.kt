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

    /**
     * Inserts what fits and leaves the rest out.
     */
    PARTIAL,

    /**
     * Inserts what fits and drops the rest into the world, where it never despawns.
     *
     * A player's inventory drops at the player, with the player as the items' owner. Any other
     * inventory drops at the center of its location without an owner. Committing an inventory
     * without a location under this policy throws [IllegalStateException] before anything changes,
     * even when every item fits.
     */
    DROP,
}
