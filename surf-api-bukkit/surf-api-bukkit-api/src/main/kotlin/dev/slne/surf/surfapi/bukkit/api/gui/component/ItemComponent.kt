package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import org.bukkit.inventory.ItemStack

/**
 * Simple item component that renders a static item.
 */
open class ItemComponent(
    private val item: ItemStack,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    
    override fun render(context: ViewContext): ItemStack = item
    
    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}
