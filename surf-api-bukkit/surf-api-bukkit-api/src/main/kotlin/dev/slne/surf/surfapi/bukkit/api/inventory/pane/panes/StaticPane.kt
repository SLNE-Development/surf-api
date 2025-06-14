package dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Flippable
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Rotatable
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import org.bukkit.inventory.ItemStack

interface StaticPane: Pane, Flippable, Rotatable {

    fun setItem(slot: Slot, item: GuiItem)
    fun setItem(slot: Slot, init: GuiItem.() -> Unit)

    fun fillWith(item: ItemStack, handler: ClickHandlerDsl = {})
    fun removeItem(item: GuiItem)
    fun removeItem(slot: Slot)

    override fun clone(): StaticPane
}