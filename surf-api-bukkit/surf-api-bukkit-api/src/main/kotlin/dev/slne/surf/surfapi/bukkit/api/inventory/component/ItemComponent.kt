package dev.slne.surf.surfapi.bukkit.api.inventory.component

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * Represents an item component that can be placed in a GUI.
 */
interface ItemComponent : RenderableComponent {
    /**
     * The slot position of this item (0-based index).
     */
    val slot: Int

    /**
     * The ItemStack to be rendered.
     */
    val itemStack: ItemStack?

    /**
     * Whether the item can be taken from the inventory.
     */
    val canTake: Boolean
        get() = false

    /**
     * Called when the item is clicked.
     */
    suspend fun onClick(player: Player, clickType: ClickType)

    /**
     * Updates the item for the specified player.
     */
    suspend fun updateFor(player: Player)
}
