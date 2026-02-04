package dev.slne.surf.surfapi.bukkit.api.gui.component.components

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.area.ComponentArea
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.component.ComponentPriority
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import it.unimi.dsi.fastutil.objects.Object2ObjectMap

/**
 * Container component that renders multiple items at specific slots.
 * This is useful for paginated components, panels, or complex layouts.
 * The area can be any shape (cuboid, circular, custom).
 */
abstract class ContainerComponent(
    override val area: ComponentArea,
    override val priority: ComponentPriority = ComponentPriority.NORMAL
) : Component() {
    /**
     * Render multiple items at their respective slots.
     * Override this to provide the slot-to-item mapping.
     */
    abstract override fun renderSlots(context: ViewContext): Object2ObjectMap<Slot, GuiItem>

    /**
     * Container doesn't render a single item.
     */
    final override fun render(context: ViewContext): GuiItem? = null

    override fun toString(): String {
        return "ContainerComponent(area=$area, priority=$priority) ${super.toString()}"
    }
}
