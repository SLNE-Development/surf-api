package dev.slne.surf.surfapi.bukkit.api.inventory

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestGui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.OutlinePane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.PaginatedPane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.StaticPane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Mask
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Pattern
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Inventory
import java.util.*

@InternalSurfApi
interface InventoryBridge {

    fun getGuiByInventory(inventory: Inventory): Gui?
    fun createGuiItem(key: NamespacedKey?, uuid: UUID?): GuiItem
    fun createUpdatableGuiItem(
        key: NamespacedKey?,
        uuid: UUID?
    ): UpdatableGuiItem

    fun createMask(vararg mask: String): Mask
    fun createPattern(vararg pattern: String): Pattern

    fun createChestGui(size: ChestGui.ChestGuiSize, parent: Gui? = null): ChestGui


    fun createOutlinePane(slot: Slot, length: Int, height: Int, uuid: UUID?): OutlinePane
    fun createPaginatedPane(slot: Slot, length: Int, height: Int, uuid: UUID?): PaginatedPane
    fun createStaticPane(slot: Slot, length: Int, height: Int, uuid: UUID?): StaticPane

    companion object {
        val instance = requiredService<InventoryBridge>()
    }
}