package dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.types

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestGui
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractNamedGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.utils.InventoryBased
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.InventoryComponentImpl
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import it.unimi.dsi.fastutil.objects.Object2IntMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

open class ChestGuiImpl(
    initialSize: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.FIVE_ROWS,
    parent: AbstractGui? = null,
) : AbstractNamedGui(parent), ChestGui, InventoryBased {

    private var rows: Int = initialSize.rows
        set(value) {
            if (value == field) return

            inventoryComponent = InventoryComponentImpl(9, value + 4)

            for (pane in inventoryComponent.panes) {
                inventoryComponent.addPane(pane)
            }

            sizeDirty = true
        }

    override val size: ChestGui.ChestGuiSize
        get() = ChestGui.ChestGuiSize.fromRows(rows) ?: error("Invalid rows count: $rows")

    override var inventoryComponent = InventoryComponentImpl(9, rows + 4)
        set(value) {
            field = value
            panes = value.panes.freeze()
        }

    private var sizeDirty = false

    final override var panes = inventoryComponent.panes.freeze()
        private set

    override val items get() = panes.flatMapTo(mutableObjectListOf()) { it.items }.freeze()
    override val viewers
        get() = backingInventory.viewers.filterIsInstanceTo(mutableObjectSetOf<Player>()).freeze()

    override fun size(size: ChestGui.ChestGuiSize) {
        if (this.size == size) return
        rows = size.rows
    }

    override fun updateAllItems(): Object2IntMap<GuiItemImpl> =
        panes.fold(mutableObject2IntMapOf()) { acc, pane ->
            acc.apply { putAll(pane.updateItems()) }
        }

    override fun updateItem0(item: UpdatableGuiItemImpl) =
        panes.find { it.items.contains(item) }?.updateItem(item)


    override fun addPane(pane: Pane) {
        inventoryComponent.addPane(pane)
    }

    override fun createInventory() = Bukkit.createInventory(this, rows * 9, title)
    override fun getInventory() = backingInventory

    override fun show(player: Player) {
        if (titleDirty || sizeDirty) {
            backingInventory = createInventory()

            titleDirty = false
            sizeDirty = false
        }

        backingInventory.clear()

        val height = inventoryComponent.height
        inventoryComponent.display()

        val topComponent = inventoryComponent.excludeRows(height - 4, height - 1)
        val bottomComponent = inventoryComponent.excludeRows(0, height - 5)

        topComponent.placeItems(backingInventory, 0)

        if (bottomComponent.hasItem()) {
            if (!cache.contains(player)) {
                cache.storeAndClear(player)
            }

            bottomComponent.placeItems(player.inventory, 0)
        }

        player.openInventory(backingInventory)
    }

    override fun click(event: InventoryClickEvent) {
        inventoryComponent.click(this, event, event.rawSlot)
    }

    override fun isPlayerInventoryUsed() =
        inventoryComponent.excludeRows(0, inventoryComponent.height - 5).hasItem()
}