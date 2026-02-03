package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Dynamic component that renders based on a callback.
 * Takes a single slot for both start and end (1x1 area).
 */
open class DynamicComponent(
    private val slot: Slot,
    private val renderer: (ViewContext) -> GuiItem?,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    override val startSlot: Slot = slot
    override val endSlot: Slot = slot
    
    override fun render(context: ViewContext): GuiItem? = renderer(context)

    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}
