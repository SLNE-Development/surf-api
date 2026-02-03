package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import org.bukkit.inventory.ItemStack

/**
 * Container component that renders multiple items at specific slots.
 * This is useful for paginated components or complex layouts.
 */
abstract class ContainerComponent : Component() {
    /**
     * Render multiple items at their respective slots.
     * Override this to provide the slot-to-item mapping.
     */
    abstract override fun renderSlots(context: ViewContext): Map<Int, ItemStack>
    
    /**
     * Container doesn't render a single item.
     */
    final override fun render(context: ViewContext): ItemStack? = null
}
