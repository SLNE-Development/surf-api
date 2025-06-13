package dev.slne.surf.surfapi.bukkit.api.inventory.utils

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import java.util.*

class InventoryComponent internal constructor(
    val length: Int,
    val height: Int,
) : Cloneable {

    internal val panes = mutableObjectListOf<Pane>()
    internal val items = Array<Array<ItemStack?>>(length) { Array(height) { null } }

    init {
        if (length < 0 || height < 0) {
            throw IllegalArgumentException("Length and height must be non-negative")
        }
    }

    fun addPane(pane: Pane) {
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

    fun display(inventory: Inventory, offset: Int) {
        display()

        placeItems(inventory, offset)
    }

    fun display(playerInventory: PlayerInventory, offset: Int) {
        display()

        placeItems(playerInventory, offset)
    }

    fun placeItems(playerInventory: PlayerInventory, offset: Int) {
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

    fun placeItems(inventory: Inventory, offset: Int) {
        for (x in 0 until length) {
            for (y in 0 until height) {
                inventory.setItem(y * length + x + offset, getItem(slot(x, y)))
            }
        }
    }

    @OptIn(NmsUseWithCaution::class)
    fun click(gui: Gui, event: InventoryClickEvent, slot: Int) {
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

    public override fun clone(): InventoryComponent {
        val component = InventoryComponent(length, height)

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

    fun excludeRows(from: Int, end: Int): InventoryComponent {
        if (from < 0 || end >= height) {
            throw IllegalArgumentException("Invalid row range: $from to $end")
        }

        val newHeight = height - (end - from + 1)
        val newComponent = InventoryComponent(length, newHeight)

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

    fun hasItem(): Boolean {
        for (x in 0 until length) {
            for (y in 0 until height) {
                if (getItem(slot(x, y)) != null) {
                    return true
                }
            }
        }

        return false
    }

    fun display() {
        clearItems()

        for (pane in panes) {
            if (!pane.visible) {
                continue
            }

            pane.display(this, 0, 0, length, height)
        }
    }

    fun hasItem(slot: Slot) = getItem(slot) != null

    fun getItem(slot: Slot): ItemStack? {
        val x = slot.getX(length)
        val y = slot.getY(length)

        if (!inBounds(slot)) {
            throw IllegalArgumentException("Coordinates ($x, $y) are out of bounds for inventory size $length x $height")
        }

        return items[x][y]
    }

    fun setItem(item: GuiItem, slot: Slot) {
        val x = slot.getX(length)
        val y = slot.getY(length)

        if (!inBounds(slot)) {
            throw IllegalArgumentException("Coordinates ($x, $y) are out of bounds for inventory size $length x $height")
        }

        items[x][y] = item.clone().apply { applyUuid() }.itemStack
    }

    fun setItem(itemStack: ItemStack, slot: Slot) {
        val x = slot.getX(length)
        val y = slot.getY(length)

        if (!inBounds(slot)) {
            throw IllegalArgumentException("Coordinates ($x, $y) are out of bounds for inventory size $length x $height")
        }

        items[x][y] = itemStack
    }

    val size get() = length * height

    fun clearItems() {
        for (item in items) {
            Arrays.fill(item, null)
        }
    }

    fun inBounds(slot: Slot): Boolean {
        val x = slot.getX(length)
        val y = slot.getY(length)

        val xBounds = inBounds(0, length - 1, x)
        val yBounds = inBounds(0, height - 1, y)

        return xBounds && yBounds
    }

    fun inBounds(lowerBound: Int, upperBound: Int, value: Int) = value in lowerBound..upperBound

    fun getPane(index: Int): Pane {
        if (!inBounds(0, panes.size - 1, index)) {
            throw IndexOutOfBoundsException("Pane index $index is out of bounds for size ${panes.size}")
        }

        return panes[index]
    }

}