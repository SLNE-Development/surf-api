package dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Flippable
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Mask
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Orientable
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Rotatable

interface OutlinePane : Pane, Orientable, Flippable, Rotatable {

    fun setItem(index: Int, item: GuiItem)
    fun addItem(item: GuiItem)
    fun removeItem(item: GuiItem)
    fun applyMask(mask: Mask)

    override fun clone(): OutlinePane
}