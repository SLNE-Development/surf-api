package dev.slne.surf.surfapi.bukkit.api.inventory.gui

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import it.unimi.dsi.fastutil.objects.ObjectList
import org.jetbrains.annotations.Unmodifiable

@GuiDsl
interface MergedGui {
    val inventoryComponent: InventoryComponent

    fun addPane(pane: Pane)

    val panes: @Unmodifiable ObjectList<out Pane>
    val items: @Unmodifiable ObjectList<out Pane>
}