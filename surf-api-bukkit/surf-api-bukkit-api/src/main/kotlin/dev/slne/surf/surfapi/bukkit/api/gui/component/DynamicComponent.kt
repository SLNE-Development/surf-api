package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import org.bukkit.inventory.ItemStack

/**
 * Dynamic component that renders based on a callback.
 */
open class DynamicComponent(
    private val renderer: (ViewContext) -> ItemStack?,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    
    override fun render(context: ViewContext): ItemStack? = renderer(context)
    
    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}
