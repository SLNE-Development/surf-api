package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Container component that renders multiple items at specific slots.
 * This is useful for paginated components or complex layouts.
 * The area is defined by startSlot (top-left) and endSlot (bottom-right).
 */
abstract class ContainerComponent(
    override val startSlot: Slot,
    override val endSlot: Slot,
    override val priority: ComponentPriority = ComponentPriority.NORMAL
) : Component() {
    /**
     * Render multiple items at their respective slots.
     * Override this to provide the slot-to-item mapping.
     */
    abstract override fun renderSlots(context: ViewContext): Map<Slot, GuiItem>
    
    /**
     * Container doesn't render a single item.
     */
    final override fun render(context: ViewContext): GuiItem? = null
}
