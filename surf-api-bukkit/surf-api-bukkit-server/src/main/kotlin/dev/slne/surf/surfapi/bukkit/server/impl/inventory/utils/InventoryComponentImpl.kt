package dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.slot
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.AbstractPane
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

class InventoryComponentImpl(
    override val length: Int,
    override val height: Int,
) : InventoryComponent {

    val panes = mutableObjectListOf<AbstractPane>()
    val items = Array<Array<ItemStack?>>(length) { Array(height) { null } }

    init {
        require(length > 0) { "Length must be greater than 0" }
        require(height > 0) { "Height must be greater than 0" }
    }

    override fun addPane(pane: Pane) {
        require(pane is AbstractPane) { "Pane must be an instance of AbstractPane" }

        val size = panes.size

        if (size == 0) {
            panes.add(pane)
            return
        }

        val priority = pane.priority
        var left = 0
        var right = size - 1

        while (left <= right) {
            val middle = (left + right) / 2
            val middlePriority = getPane(middle).priority

            if (middlePriority == priority) {
                panes.add(middle, pane)
                return
            }

            if (middlePriority.isLessThan(priority)) {
                left = middle + 1
            } else if (middlePriority.isGreaterThan(priority)) {
                right = middle - 1
            }
        }

        panes.add(right + 1, pane)
    }

    override fun display(inventory: Inventory, offset: Int) {
        display()

        placeItems(inventory, offset)
    }

    override fun display(playerInventory: PlayerInventory, offset: Int) {
        display()

        placeItems(playerInventory, offset)
    }

    override fun placeItems(playerInventory: PlayerInventory, offset: Int) {
        for (x in 0 until length) {
            for (y in 0 until height) {
                val slot = if (y == height - 1) {
                    x + offset
                } else {
                    (y + 1) * length + x + offset
                }

                playerInventory.setItem(slot, getItem(slot(x, y)))
            }
        }
    }

    override fun placeItems(inventory: Inventory, offset: Int) {
        for (x in 0 until length) {
            for (y in 0 until height) {
                inventory.setItem(y * length + x + offset, getItem(slot(x, y)))
            }
        }
    }

    @OptIn(NmsUseWithCaution::class)
    fun click(gui: AbstractGui, event: InventoryClickEvent, slot: Int) {
        val panes = this.panes.freeze()

        for (i in panes.indices.reversed()) {
            val pane = panes[i]
            val result = pane.click(
                gui,
                component = this,
                event,
                slot,
                paneOffsetX = 0,
                paneOffsetY = 0,
                maxLength = length,
                maxHeight = height
            )

            if (result) {
                break
            }
        }
    }

    public override fun clone(): InventoryComponentImpl {
        val component = InventoryComponentImpl(length, height)

        for (x in 0 until length) {
            for (y in 0 until height) {
                val item = getItem(slot(x, y)) ?: continue

                component.setItem(item.clone(), slot(x, y))
            }
        }

        for (pane in panes) {
            component.addPane(pane.clone())
        }

        return component
    }

    override fun excludeRows(from: Int, end: Int): InventoryComponentImpl {
        require(from >= 0) { "From index must be non-negative" }
        require(end < height) { "End index must be less than height" }

        val newHeight = height - (end - from + 1)
        val newComponent = InventoryComponentImpl(length, newHeight)

        for (pane in panes) {
            newComponent.addPane(pane)
        }

        for (x in 0 until length) {
            var newY = 0

            for (y in 0 until height) {
                val item = getItem(slot(x, y))

                if (y >= from && y <= end) {
                    continue
                }

                if (item != null) {
                    newComponent.setItem(item, slot(x, newY))
                }

                newY++
            }
        }

        return newComponent
    }

    override fun hasItem(): Boolean {
        for (x in 0 until length) {
            for (y in 0 until height) {
                if (getItem(slot(x, y)) != null) {
                    return true
                }
            }
        }

        return false
    }

    override fun display() {
        clearItems()

        for (pane in panes) {
            if (!pane.visible) {
                continue
            }

            pane.display(this, 0, 0, length, height)
        }
    }

    override fun hasItem(slot: Slot) = getItem(slot) != null

    override fun getItem(slot: Slot): ItemStack? {
        val x = slot.getX(length)
        val y = slot.getY(length)

        require(inBounds(slot)) { "Coordinates ($x, $y) are out of bounds for inventory size $length x $height" }

        return items[x][y]
    }

    override fun setItem(item: GuiItem, slot: Slot) {
        val x = slot.getX(length)
        val y = slot.getY(length)

        require(inBounds(slot)) { "Coordinates ($x, $y) are out of bounds for inventory size $length x $height" }

        items[x][y] = item.clone().apply { applyUuid() }.itemStack
    }

    override fun setItem(itemStack: ItemStack, slot: Slot) {
        val x = slot.getX(length)
        val y = slot.getY(length)

        require(inBounds(slot)) { "Coordinates ($x, $y) are out of bounds for inventory size $length x $height" }

        items[x][y] = itemStack
    }

    val size get() = length * height

    override fun clearItems() {
        for (item in items) {
            item.fill(null)
        }
    }

    override fun inBounds(slot: Slot): Boolean {
        val x = slot.getX(length)
        val y = slot.getY(length)

        val xBounds = inBounds(0, length - 1, x)
        val yBounds = inBounds(0, height - 1, y)

        return xBounds && yBounds
    }

    override fun inBounds(lowerBound: Int, upperBound: Int, value: Int) =
        value in lowerBound..upperBound

    override fun getPane(index: Int): AbstractPane {
        if (!inBounds(0, panes.size - 1, index)) {
            throw IndexOutOfBoundsException("Pane index $index is out of bounds for size ${panes.size}")
        }

        return panes[index]
    }

}