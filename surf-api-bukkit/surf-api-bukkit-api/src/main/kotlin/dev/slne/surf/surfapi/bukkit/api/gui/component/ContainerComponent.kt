package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.area.ComponentArea
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Container component that renders multiple items at specific slots.
 * This is useful for paginated components, panels, or complex layouts.
 * The area can be any shape (cuboid, circular, custom).
 */
abstract class ContainerComponent(
    override val area: ComponentArea
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
