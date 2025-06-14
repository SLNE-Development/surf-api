package dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.types

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.MergedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.types.ChestGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractNamedGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.utils.InventoryBased
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.AbstractPane
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.InventoryComponentImpl
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import it.unimi.dsi.fastutil.objects.Object2IntMap
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

open class ChestGuiImpl(
    title: Component,
    initialSize: ChestGui.ChestGuiSize = ChestGui.ChestGuiSize.FIVE_ROWS,
    parent: AbstractGui? = null,
) : AbstractNamedGui(title, parent), MergedGui, InventoryBased {

    private var rows: Int = initialSize.rows
        set(value) {
            inventoryComponent = InventoryComponentImpl(9, value + 4)

            for (pane in inventoryComponent.panes) {
                inventoryComponent.addPane(pane)
            }

            sizeDirty = true
        }

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

    override fun updateAllItems(): Object2IntMap<GuiItemImpl> =
        panes.fold(mutableObject2IntMapOf()) { acc, pane ->
            acc.apply { putAll(pane.updateItems()) }
        }

    override fun updateItem0(item: UpdatableGuiItemImpl) =
        panes.find { it.items.contains(item) }?.updateItem(item)


    override fun addPane(pane: AbstractPane) {
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

    override fun clone() = super.clone() as ChestGuiImpl

    override fun click(event: InventoryClickEvent) {
        inventoryComponent.click(this, event, event.rawSlot)
    }

    override fun isPlayerInventoryUsed() =
        inventoryComponent.excludeRows(0, inventoryComponent.height - 5).hasItem()
}

//@MenuMarker
//class ChestSinglePlayerGui internal constructor(
//    val player: Player,
//    title: Component,
//    size: ChestGuiSize = ChestGuiSize.SIX_ROWS,
//    parent: AbstractGui? = null,
//) : ChestGuiImpl(title, size, parent)