package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Dynamic component that renders based on a callback.
 */
open class DynamicComponent(
    private val renderer: (ViewContext) -> GuiItem?,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    
    override fun render(context: ViewContext): GuiItem? = renderer(context)
    
    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}
