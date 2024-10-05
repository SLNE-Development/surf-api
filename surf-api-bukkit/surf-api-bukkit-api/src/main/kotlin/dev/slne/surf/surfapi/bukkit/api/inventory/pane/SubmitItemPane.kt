package dev.slne.surf.surfapi.bukkit.api.inventory.pane

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.Range
import kotlin.math.min


class SubmitItemPane @JvmOverloads constructor(
    slot: Slot,
    length: @Range(from = 1, to = 6) Int,
    height: @Range(from = 1, to = 6) Int,
    private val filter: ItemStack.() -> Boolean,
    priority: Priority = Priority.NORMAL
) : Pane(slot, length, height, priority) {

    constructor(
        slot: Slot,
        length: @Range(from = 1, to = 6) Int,
        height: @Range(from = 1, to = 6) Int,
        filter: List<Material>,
        priority: Priority = Priority.NORMAL
    ) : this(slot, length, height, { filter.contains(type) }, priority)

    private val _items = mutableMapOf<Slot, GuiItem>()
    val items get() = _items.toMap()

    override fun display(
        inventoryComponent: InventoryComponent,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int
    ) {
        val length = min(length, maxLength)
        val height = min(height, maxHeight)

        for ((location, item) in _items.filter { (_, item) -> item.isVisible }) {
            val x = location.getX(getLength())
            val y = location.getY(getHeight())

            if (x < 0 || x >= length || y < 0 || y >= height) {
                continue
            }

            val slot = getSlot()
            val finalRow = slot.getY(maxLength) + y + paneOffsetY
            val finalColumn = slot.getX(maxLength) + x + paneOffsetX

            inventoryComponent.setItem(item, finalColumn, finalRow)
        }
    }

    override fun click(
        gui: Gui,
        inventoryComponent: InventoryComponent,
        event: InventoryClickEvent,
        slot: Int,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int
    ): Boolean {
        val length = min(length, maxLength)
        val height = min(height, maxHeight)
        val paneSlot = getSlot()

        val xPosition = paneSlot.getX(maxLength)
        val yPosition = paneSlot.getY(maxLength)
        val totalLength = inventoryComponent.length

        val adjustedSlot =
            slot - (xPosition + paneOffsetX) - totalLength * (yPosition + paneOffsetY)
        val x = adjustedSlot % totalLength
        val y = adjustedSlot / totalLength


        //this isn't our item
        if (x < 0 || x >= length || y < 0 || y >= height) {
            return false
        }

        callOnClick(event)

        val currentItem = event.currentItem ?: return false
        val item = findMatchingItem(_items.values, currentItem) ?: return false

        if (!filter(currentItem)) {
            event.isCancelled = true
            return false
        }

        item.callAction(event)
        return true
    }

    override fun copy(): Pane {
        val pane = SubmitItemPane(slot, length, height, filter, priority)

        pane._items.putAll(_items.map { (slot, item) -> slot to item.copy() }.toMap(pane._items))
        pane.isVisible = isVisible
        pane.onClick = onClick
        pane.uuid = uuid

        return pane
    }

    override fun getItems(): MutableCollection<GuiItem> = _items.values.toMutableList()
    override fun getPanes(): MutableCollection<Pane> = mutableListOf()
    override fun clear() {
        _items.clear()
    }
}