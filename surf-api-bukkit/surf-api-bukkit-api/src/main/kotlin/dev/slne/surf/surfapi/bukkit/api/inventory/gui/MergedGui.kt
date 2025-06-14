package dev.slne.surf.surfapi.bukkit.api.inventory.gui

import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.OutlinePane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.PaginatedPane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.StaticPane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import it.unimi.dsi.fastutil.objects.ObjectList
import org.jetbrains.annotations.Unmodifiable
import java.util.*

@GuiDsl
interface MergedGui {
    val inventoryComponent: InventoryComponent
    val panes: @Unmodifiable ObjectList<out Pane>
    val items: @Unmodifiable ObjectList<out GuiItem>


    fun addPane(pane: Pane)

    fun outlinePane(
        slot: Slot,
        length: Int,
        height: Int,
        uuid: UUID? = null,
        init: OutlinePane.() -> Unit,
    ): OutlinePane {
        val pane = InventoryBridge.instance.createOutlinePane(slot, length, height, uuid)
        pane.init()
        addPane(pane)
        return pane
    }

    fun paginatedPane(
        slot: Slot,
        length: Int,
        height: Int,
        uuid: UUID? = null,
        init: PaginatedPane.() -> Unit,
    ): PaginatedPane {
        val pane = InventoryBridge.instance.createPaginatedPane(slot, length, height, uuid)
        pane.init()
        addPane(pane)
        return pane
    }

    fun staticPane(
        slot: Slot,
        length: Int,
        height: Int,
        uuid: UUID? = null,
        init: StaticPane.() -> Unit,
    ): StaticPane {
        val pane = InventoryBridge.instance.createStaticPane(slot, length, height, uuid)
        pane.init()
        addPane(pane)
        return pane
    }
}