package dev.slne.surf.api.paper.packet.lore

import io.papermc.paper.persistence.PersistentDataContainerView
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

/**
 * A simplified handler for modifying the lore of an item stack in a Bukkit-based environment.
 *
 * This interface extends [SurfPaperPacketLoreHandlerSimple] and provides a streamlined version
 * of the `handleLore` method that does not require interacting with the persistent data container
 * or the item stack itself. Implementations can focus solely on modifying the list of lore components.
 *
 * This is particularly useful when additional item metadata or item-specific context is unnecessary.
 */
fun interface SurfPaperPacketLoreHandlerSimple : SurfPaperPacketLoreHandler {
    override fun handleLore(
        loreToDisplay: MutableList<Component>,
        pdc: PersistentDataContainerView,
        itemStack: ItemStack
    ) {
        handleLore(loreToDisplay)
    }

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
     * Example Usage:
     * ```
     * val handler = SurfPaperPacketLoreHandlerSimple { lore ->
     *     lore.add(Component.text("Simplified Lore"))
     * }
     * ```
     */
    fun handleLore(loreToDisplay: MutableList<Component>)
}
