package dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.inventory.ItemStack

interface PaginatedPane: Pane {
    val page: Int
    val totalPages: Int

    fun page(page: Int)
    fun previousPage()
    fun nextPage()

    fun populateWithGuiItems(items: ObjectList<GuiItem>)
    fun populateWithItemStacks(items: ObjectList<ItemStack>)

    override fun clone(): PaginatedPane
}