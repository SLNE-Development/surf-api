package dev.slne.surf.surfapi.bukkit.api.inventory.gui.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import it.unimi.dsi.fastutil.objects.ObjectList

interface MergedGui {

    val inventoryComponent: InventoryComponent

    fun addPane(pane: Pane)

    val panes: ObjectList<Pane>
    val items: ObjectList<GuiItem>
}