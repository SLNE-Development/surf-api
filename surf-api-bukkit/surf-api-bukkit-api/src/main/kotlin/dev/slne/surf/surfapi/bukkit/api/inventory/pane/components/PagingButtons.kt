package dev.slne.surf.surfapi.bukkit.api.inventory.pane.components

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import org.bukkit.inventory.ItemStack

interface PagingButtons: Pane {
    fun setBackwardsButton(item: GuiItem)
    fun setBackwardsButton(item: ItemStack, action: ClickHandlerDsl = {})

    fun setForwardsButton(item: GuiItem)
    fun setForwardsButton(item: ItemStack, action: ClickHandlerDsl = {})

    override fun clone(): PagingButtons
}