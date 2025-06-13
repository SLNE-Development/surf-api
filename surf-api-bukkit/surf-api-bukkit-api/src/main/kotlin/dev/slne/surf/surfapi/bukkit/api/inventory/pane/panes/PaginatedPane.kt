package dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.slot
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.*
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import kotlin.math.ceil

@OptIn(NmsUseWithCaution::class)
class PaginatedPane(
    slot: Slot,
    length: Int,
    height: Int,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : Pane(slot, length, height, priority, uuid) {

    override val panes: ObjectList<Pane>
        get() {
            val panes = mutableObjectListOf<Pane>()

            paginatedPanes.forEach { (_, paginatedPane) ->
                paginatedPane.forEach { pane ->
                    panes.addAll(pane.panes)
                }
                panes.addAll(paginatedPane)
            }

            return panes
        }

    override val items: ObjectList<GuiItem>
        get() = panes.flatMap { it.items }.toObjectList()

    override fun clear() {
        paginatedPanes.clear()
    }

    private val paginatedPanes = mutableInt2ObjectMapOf<ObjectList<Pane>>()
    var page: Int = 0
        set(value) {
            if (!paginatedPanes.containsKey(value)) {
                throw IllegalArgumentException("Page $value does not exist.")
            }

            field = value
        }

    fun previousPage() {
        if (page > 0) {
            page--
        }
    }

    fun nextPage() {
        if (paginatedPanes.containsKey(page + 1)) {
            page++
        }
    }

    fun addPage(pane: Pane) {
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
        if (!paginatedPanes.containsKey(page)) {
            paginatedPanes[page] = mutableObjectListOf()
        }

        paginatedPanes[page]!!.add(pane)
        paginatedPanes[page]!!.sortWith(Comparator.comparing(Pane::priority))
    }

    fun populateWithGuiItems(items: ObjectList<GuiItem>) {
        if (items.isEmpty()) {
            return
        }

        val itemsPerPage = length * height
        val pagesNeeded: Int = ceil(items.size / itemsPerPage.toDouble()).coerceAtLeast(1.0).toInt()

        for (i in 0 until pagesNeeded) {
            val page = OutlinePane(slot(0, 0), length, height)

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

    fun populateWithItemStacks(items: ObjectList<ItemStack>) =
        populateWithGuiItems(items.map { GuiItem(it) }.toObjectList())

    fun getPages() = paginatedPanes.size

    override fun display(
        component: InventoryComponent,
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

    override fun updateItems(): Object2IntMap<GuiItem> {
        val updatedItems = mutableObject2IntMapOf<GuiItem>()
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

    override fun updateItem(item: UpdatableGuiItem): Int? {
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
        gui: Gui,
        component: InventoryComponent,
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

        for (pane in paginatedPanes.getOrElse(page) { mutableObjectListOf<Pane>() }.freeze()) {
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

    override fun clone(): PaginatedPane {
        val paginatedPane = PaginatedPane(
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

    fun deletePage(page: Int) {
        if (paginatedPanes.remove(page) == null) {
            return
        }

        val newPanes = mutableInt2ObjectMapOf<ObjectList<Pane>>()
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