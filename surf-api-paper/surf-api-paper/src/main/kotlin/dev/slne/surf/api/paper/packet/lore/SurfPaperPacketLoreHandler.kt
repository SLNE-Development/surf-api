package dev.slne.surf.api.paper.packet.lore

import io.papermc.paper.persistence.PersistentDataContainerView
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

/**
 * A functional interface for modifying the lore of an item stack in a Bukkit-based environment.
 *
 * Implementations of this interface allow custom manipulation of an item stack's lore, such as
 * adding, removing, or reordering lore components. The handler operates on a mutable list of lore
 * components, providing flexibility for combining dynamic and pre-existing lore elements.
 *
 * This is particularly useful when multiple handlers are involved, as they can each contribute
 * to the final lore displayed on the item stack.
 */
fun interface SurfPaperPacketLoreHandler {
    /**
     * The priority controlling when this handler is invoked relative to others.
     *
     * Smaller values run earlier, larger values run later. Defaults to
     * [SurfPaperPacketLorePriority.NORMAL]. See [SurfPaperPacketLorePriority] for the
     * available predefined constants.
     *
     * Implementations may override this to provide a per-handler default. The priority
     * passed when registering the handler (if any) takes precedence over this value.
     */
    val priority: Short
        get() = SurfPaperPacketLorePriority.NORMAL

    /**
     * Handles the modification of the lore of an item stack.
     *
     * @param loreToDisplay A mutable list of lore components representing the text displayed
     *                      on the item stack.
     *                      - The list initially contains the item's "real" lore and any lore
     *                        added by other lore handlers.
     *                      - Implementations can append, remove, or insert lore components
     *                        to influence the displayed text.
     *                      - Adding lore at the beginning of the list will give it higher
     *                        visual priority.
     *
     * @param pdc A read-only view of the persistent data container associated with
     *                      the item stack. This allows accessing custom metadata or additional
     *                      item-specific information without modifying the container directly.
     *
     * @param itemStack     The item stack being modified. Provides access to the item's properties
     *                      and allows contextual decisions to be made when updating its lore.
     *
     * Example Usage:
     * ```
     * val handler = SurfPaperPacketLoreHandler { lore, data, item ->
     *     val customLore = Component.text("Special Item!")
     *     lore.add(0, customLore) // Add at the top of the lore list
     * }
     * ```
     */
    fun handleLore(
        loreToDisplay: MutableList<Component>,
        pdc: PersistentDataContainerView,
        itemStack: ItemStack
    )
}
