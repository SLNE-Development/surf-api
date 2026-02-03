package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Simple item component that renders a static item.
 * Takes a single slot for both start and end (1x1 area).
 */
open class ItemComponent(
    private val slot: Slot,
    private val item: GuiItem,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    override val startSlot: Slot = slot
    override val endSlot: Slot = slot
    
    override fun render(context: ViewContext): GuiItem = item

    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}
