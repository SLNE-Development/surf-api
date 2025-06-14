package dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.PaginatedPane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.AbstractPane
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.InventoryComponentImpl
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.ceil

class PaginatedPaneImpl(
    slot: Slot,
    length: Int,
    height: Int,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : AbstractPane(slot, length, height, priority, uuid), PaginatedPane {

    override val totalPages: Int
        get() = paginatedPanes.size

    override val panes: ObjectList<AbstractPane>
        get() {
            val panes = mutableObjectListOf<AbstractPane>()

            paginatedPanes.forEach { (_, paginatedPane) ->
                paginatedPane.forEach { pane ->
                    panes.addAll(pane.panes)
                }
                panes.addAll(paginatedPane)
            }

            return panes
        }

    override val items: ObjectList<GuiItemImpl>
        get() = panes.flatMap { it.items }.toObjectList()

    override fun clear() {
        paginatedPanes.clear()
    }

    private val paginatedPanes = mutableInt2ObjectMapOf<ObjectList<AbstractPane>>()
    override var page: Int = 0
        set(value) {
            if (!paginatedPanes.containsKey(value)) {
                throw IllegalArgumentException("Page $value does not exist.")
            }

            field = value
        }

    override fun page(page: Int) {
        this.page = page
    }

    override fun previousPage() {
        if (page > 0) {
            page--
        }
    }

    override fun nextPage() {
        if (paginatedPanes.containsKey(page + 1)) {
            page++
        }
    }

    fun addPage(pane: Pane) {
        require(pane is AbstractPane) { "Only AbstractPane can be added to PaginatedPane." }
        val newPageList = mutableObjectListOf(pane)

        if (paginatedPanes.isEmpty()) {
            paginatedPanes[0] = newPageList
            return
        }

        val highestPage = paginatedPanes.keys.maxOrNull() ?: 0

        if (highestPage == Int.MAX_VALUE) {
            throw ArithmeticException("Cannot add more pages, maximum reached.")
        }

        paginatedPanes[highestPage + 1] = newPageList
    }

    fun addPane(page: Int, pane: Pane) {
        require(pane is AbstractPane) { "Only AbstractPane can be added to PaginatedPane." }

        if (!paginatedPanes.containsKey(page)) {
            paginatedPanes[page] = mutableObjectListOf()
        }

        paginatedPanes[page]!!.add(pane)
        paginatedPanes[page]!!.sortWith(Comparator.comparing(AbstractPane::priority))
    }

    override fun populateWithGuiItems(items: ObjectList<GuiItem>) {
        if (items.isEmpty()) {
            return
        }

        val itemsPerPage = length * height
        val pagesNeeded: Int = ceil(items.size / itemsPerPage.toDouble()).coerceAtLeast(1.0).toInt()

        for (i in 0 until pagesNeeded) {
            val page = OutlinePaneImpl(Slot(0, 0), length, height)

            for (j in 0 until itemsPerPage) {
                val index = i * itemsPerPage + j

                if (index >= items.size) {
                    break
                }

                page.addItem(items[index])
            }

            addPane(i, page)
        }
    }

    override fun populateWithItemStacks(items: ObjectList<ItemStack>) =
        populateWithGuiItems(items.map { GuiItemImpl().apply { item(it) } }.toObjectList())

    override fun display(
        component: InventoryComponentImpl,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    ) {
        val panes = paginatedPanes[page] ?: return

        for (pane in panes) {
            if (!pane.visible) {
                continue
            }

            val newPaneOffsetX = paneOffsetX + slot.getX(maxLength)
            val newPaneOffsetY = paneOffsetY + slot.getY(maxLength)
            val newMaxLength = minOf(length, maxLength)
            val newMaxHeight = minOf(height, maxHeight)

            pane.display(component, newPaneOffsetX, newPaneOffsetY, newMaxLength, newMaxHeight)
        }
    }

    override fun updateItems(): Object2IntMap<GuiItemImpl> {
        val updatedItems = mutableObject2IntMapOf<GuiItemImpl>()
        val selectedPagePanes = paginatedPanes[page] ?: return updatedItems

        for (pane in selectedPagePanes) {
            if (!pane.visible) {
                continue
            }

            val paneItems = pane.updateItems()
            for ((item, index) in paneItems) {
                updatedItems[item] =
                    index + pane.slot.getX(length) + pane.slot.getY(length) * length
            }
        }

        return updatedItems
    }

    override fun updateItem(item: UpdatableGuiItemImpl): Int? {
        val selectedPagePanes = paginatedPanes[page] ?: return null

        for (pane in selectedPagePanes) {
            if (!pane.visible) {
                continue
            }

            val index = pane.updateItem(item) ?: continue
            return index + pane.slot.getX(length) + pane.slot.getY(length) * length
        }

        return null
    }

    override fun click(
        gui: AbstractGui,
        component: InventoryComponentImpl,
        event: InventoryClickEvent,
        slot: Int,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    ): Boolean {
        val length = minOf(length, maxLength)
        val height = minOf(height, maxHeight)

        val xPosition = this.slot.getX(maxLength)
        val yPosition = this.slot.getY(maxLength)

        val totalLength = component.length
        val adjustedSlot =
            slot - (xPosition + paneOffsetX) - totalLength * (yPosition + paneOffsetY)

        val x = adjustedSlot % length
        val y = adjustedSlot / length

        if (x < 0 || x >= length || y < 0 || y >= height) {
            return false
        }

        callOnClick(event)

        for (pane in paginatedPanes.getOrElse(page) { mutableObjectListOf<AbstractPane>() }.freeze()) {
            if (!pane.visible) {
                continue
            }

            if (pane.click(
                    gui,
                    component,
                    event,
                    slot,
                    paneOffsetX + xPosition,
                    paneOffsetY + yPosition,
                    length,
                    height
                )
            ) {
                return true
            }
        }

        return false
    }

    override fun clone(): PaginatedPaneImpl {
        val paginatedPane = PaginatedPaneImpl(
            slot,
            length,
            height,
            priority,
            uuid
        )

        paginatedPanes.forEach { (pageNumber, panes) ->
            panes.forEach { pane ->
                paginatedPane.addPane(pageNumber, pane.clone())
            }
        }

        paginatedPane.page = page
        paginatedPane.visible = visible
        paginatedPane.onClick = onClick

        return paginatedPane
    }

    fun removePage(page: Int) {
        if (paginatedPanes.remove(page) == null) {
            return
        }

        val newPanes = mutableInt2ObjectMapOf<ObjectList<AbstractPane>>()
        for ((index, panes) in paginatedPanes) {
            if (index > page) {
                newPanes.put(index - 1, panes)
            } else {
                newPanes.put(index, panes)
            }
        }

        paginatedPanes.clear()
        paginatedPanes.putAll(newPanes)
    }
}