package dev.slne.surf.surfapi.bukkit.api.gui.component.components

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.area.ComponentArea
import dev.slne.surf.surfapi.bukkit.api.gui.area.SingleSlotArea
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.component.ComponentPriority
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Dynamic component that renders based on a callback at a single slot.
 */
open class DynamicComponent(
    slot: Slot,
    private val renderer: (ViewContext) -> GuiItem?,
    override val priority: ComponentPriority = ComponentPriority.NORMAL,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    override val area: ComponentArea = SingleSlotArea(slot)

    override fun render(context: ViewContext): GuiItem? = renderer(context)

    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }

    override fun toString(): String {
        return "DynamicComponent(renderer=$renderer, priority=$priority, clickHandler=$clickHandler, area=$area) ${super.toString()}"
    }
}
