package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext

/**
 * Simple item component that renders a static item.
 */
open class ItemComponent(
    private val item: GuiItem,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    override fun render(context: ViewContext): GuiItem = item

    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}
