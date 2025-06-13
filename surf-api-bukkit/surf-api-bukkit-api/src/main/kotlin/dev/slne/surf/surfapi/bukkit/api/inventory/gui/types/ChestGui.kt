package dev.slne.surf.surfapi.bukkit.api.inventory.gui.types

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.MenuMarker
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.NamedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.utils.InventoryBased
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.utils.MergedGui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import dev.slne.surf.surfapi.core.api.util.toObjectSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

open class ChestGui internal constructor(
    title: Component,
    var size: ChestGuiSize = ChestGuiSize.FIVE_ROWS,
    parent: Gui? = null,
) : NamedGui(title, parent), MergedGui, InventoryBased {

    private var rows: Int = size.rows
        set(value) {
            inventoryComponent = InventoryComponent(9, value + 4)

            for (pane in inventoryComponent.panes) {
                inventoryComponent.addPane(pane)
            }

            sizeDirty = true
        }

    override var inventoryComponent = InventoryComponent(9, rows + 4)
    private var sizeDirty = false

    override val panes get() = inventoryComponent.panes
    override val items get() = panes.flatMap { it.items }.toObjectList()
    override val viewers get() = backingInventory.viewers.filterIsInstance<Player>().toObjectSet()
    override fun updateAllItems(): Object2IntMap<GuiItem> =
        panes.fold(mutableObject2IntMapOf()) { acc, pane ->
            acc.apply { putAll(pane.updateItems()) }
        }

    override fun updateItem0(item: UpdatableGuiItem) =
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

    override fun clone() = super.clone() as ChestGui

    override fun click(event: InventoryClickEvent) {
        inventoryComponent.click(this, event, event.rawSlot)
    }

    override fun isPlayerInventoryUsed() =
        inventoryComponent.excludeRows(0, inventoryComponent.height - 5).hasItem()

    enum class ChestGuiSize(val rows: Int) {
        ONE_ROW(1),
        TWO_ROWS(2),
        THREE_ROWS(3),
        FOUR_ROWS(4),
        FIVE_ROWS(5),
        SIX_ROWS(6);
    }
}

@MenuMarker
class ChestSinglePlayerGui internal constructor(
    val player: Player,
    title: Component,
    size: ChestGuiSize = ChestGuiSize.SIX_ROWS,
    parent: Gui? = null,
) : ChestGui(title, size, parent)