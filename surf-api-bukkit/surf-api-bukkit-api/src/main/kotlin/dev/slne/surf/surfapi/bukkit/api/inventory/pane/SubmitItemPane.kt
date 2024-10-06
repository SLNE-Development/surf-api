package dev.slne.surf.surfapi.bukkit.api.inventory.pane

import com.github.stefvanschie.inventoryframework.gui.GuiItem
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui
import com.github.stefvanschie.inventoryframework.pane.Pane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.slot
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryAction
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

    private val _items = mutableMapOf<Slot, ItemStack>()
    val submittedItems get() = _items.toMap()

    override fun display(
        inventoryComponent: InventoryComponent,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int
    ) {
//        val length = min(length, maxLength)
//        val height = min(height, maxHeight)
//
//        for ((location, item) in _items.filter { (_, item) -> item.isVisible }) {
//            val x = location.getX(getLength())
//            val y = location.getY(getHeight())
//
//            if (x < 0 || x >= length || y < 0 || y >= height) {
//                continue
//            }
//
//            val slot = getSlot()
//            val finalRow = slot.getY(maxLength) + y + paneOffsetY
//            val finalColumn = slot.getX(maxLength) + x + paneOffsetX
//
//            inventoryComponent.setItem(item, finalColumn, finalRow)
//        }
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
        event.apply {
            when (action) {
                InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE -> {
                    val item = currentItem

                    if (item == null || filter(item)) {
                        if (item == null) {
                            _items.remove(slot(slot))
                        } else {
                            if (isInPane(slot, paneOffsetX, paneOffsetY, maxLength, maxHeight, inventoryComponent)) {
                                _items[slot(slot)] = item
                            }
                        }

                        isCancelled = false
                    } else {
                        isCancelled = true
                    }
                }

                InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME, InventoryAction.PLACE_ONE -> {
                    val item = cursor

                    if (filter(item)) {
                        val previous = _items[slot(slot)]
                        if (previous == null || !previous.isSimilar(item)) {
                            if (isInPane(slot, paneOffsetX, paneOffsetY, maxLength, maxHeight, inventoryComponent)) {
                                _items[slot(slot)] = item
                            }
                        } else {
                            if (isInPane(slot, paneOffsetX, paneOffsetY, maxLength, maxHeight, inventoryComponent)) {
                                _items[slot(slot)] = previous.clone().apply { amount += item.amount }
                            }
                        }

                        isCancelled = false
                    } else {
                        isCancelled = true
                    }
                }

                InventoryAction.SWAP_WITH_CURSOR -> {
                    val cursorItem = cursor
                    val currentItem = currentItem

                    if (currentItem == null || filter(currentItem)) {
                        if (currentItem == null) {
                            _items.remove(slot(slot))
                        } else {
                            if (isInPane(slot, paneOffsetX, paneOffsetY, maxLength, maxHeight, inventoryComponent)) {
                                _items[slot(slot)] = currentItem
                            }
                        }

                        isCancelled = false
                    }

                    if (filter(cursorItem)) {
                        val previous = _items[slot(slot)]
                        if (previous == null || !previous.isSimilar(cursorItem)) {
                            if (isInPane(slot, paneOffsetX, paneOffsetY, maxLength, maxHeight, inventoryComponent)) {
                                _items[slot(slot)] = cursorItem
                            }
                        } else {
                            if (isInPane(slot, paneOffsetX, paneOffsetY, maxLength, maxHeight, inventoryComponent)) {
                                _items[slot(slot)] =
                                    previous.clone().apply { amount += cursorItem.amount }
                            }
                        }

                        isCancelled = false
                    } else {
                        isCancelled = true
                    }
                }

                else -> {
                    isCancelled = true
                    return false
                }
            }
        }

        println("current items: $_items")


//        val itemToCheck = currentItem ?: cursor
//
//        if (!filter(itemToCheck)) {
//            event.isCancelled = true // Item wird blockiert
//            return false
//        }


        // Wenn das geklickte Inventar unser Pane ist, fügen wir das Item hinzu oder entfernen es
//        if (event.clickedInventory == event.inventory) {
//            // Wenn das Slot-Item Luft ist, löschen wir es aus der Map
//            if (itemToCheck.type == Material.AIR) {
//                println("Removing item from slot $slot")
////                _items.remove(slot(slot)) // Stack wird entfernt
//            } else {
//                // Andernfalls fügen wir den Stack hinzu oder aktualisieren ihn
//                println("Adding item to slot $slot with amount ${itemToCheck.amount}")
////                _items[slot(slot)] = itemToCheck // Gesamter Stack wird gespeichert
//            }
//        } else {
//            // Wenn der Klick im Player-Inventar stattfindet, erlauben wir die Bewegung
//            println("Clicked inventory is player inventory")
//        }
//        event.isCancelled = false // Erlaubt das Bewegen des Items

//        event.isCancelled = false // allow the user to move the item

        // if item was moved to our pane add it to the submitted items if it was removed from our pane remove it from the submitted items
//        if (event.clickedInventory == event.inventory) {
//        val length = min(length, maxLength)
//        val height = min(height, maxHeight)
//        val paneSlot = getSlot()
//
//        val xPosition = paneSlot.getX(maxLength)
//        val yPosition = paneSlot.getY(maxLength)
//        val totalLength = inventoryComponent.length
//
//        val adjustedSlot =
//            slot - (xPosition + paneOffsetX) - totalLength * (yPosition + paneOffsetY)
//        val x = adjustedSlot % totalLength
//        val y = adjustedSlot / totalLength


        //this isn't our item
//        if (x < 0 || x >= length || y < 0 || y >= height) {
//            return false
//        }

//        for (y in 0 until height) {
//            for (x in 0 until length) {
//                val adjustedX = x + paneOffsetX
//                val adjustedY = y + paneOffsetY
//
//                val adjustedSlot1 = slot - adjustedX - totalLength * adjustedY
//                val adjustedX1 = adjustedSlot1 % totalLength
//                val adjustedY1 = adjustedSlot1 / totalLength
//
//                if ((adjustedX1 < 0) || (adjustedX1 >= length) || (adjustedY1 < 0) || (adjustedY1 >= height)) {
//                    continue
//                }
//
//                val item = inventoryComponent.getItem(adjustedX1, adjustedY1)
//
//                // update the item in the map
//                if (item != null) {
//                    _items[slot(adjustedX1, adjustedY1)] = item
//                } else {
//                    _items.remove(slot(adjustedX1, adjustedY1))
//                }
//            }
//
//        }

        // walk through all slots in the pane
//        for (i in 0 until length * height) {
//
//            val x1 = i % length
//            val y1 = i / length
//            val adjustedSlot1 = slot - (x1 + paneOffsetX) - totalLength * (y1 + paneOffsetY)
//            val adjustedX = adjustedSlot1 % totalLength
//            val adjustedY = adjustedSlot1 / totalLength
//
//            if ((x1 < 0) || (x1 >= length) || (y1 < 0) || (y1 >= height)) {
//                continue
//            }
//
//            val item = inventoryComponent.getItem(adjustedX, adjustedY)
//
//            // update the item in the map
//            if (item != null) {
//                _items[slot(x1, y1)] = item
//            } else {
//                _items.remove(slot(x1, y1))
//            }
//        }

//            println("clicked inventory is inventory")
//            _items.remove(slot(slot))
//        } else {
//            println("clicked inventory is not inventory")
//            _items[slot(slot)] = currentItem ?: cursor
//        }

        return true
    }


    private fun isInPane(
        slot: Int,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
        inventoryComponent: InventoryComponent
    ): Boolean {
        val length = min(length, maxLength)
        val height = min(height, maxHeight)

        val paneSlot = getSlot()

        val xPosition = paneSlot.getX(maxLength)
        val yPosition = paneSlot.getY(maxLength)

        val totalLength: Int = inventoryComponent.length

        val adjustedSlot =
            slot - (xPosition + paneOffsetX) - totalLength * (yPosition + paneOffsetY)

        val x = adjustedSlot % totalLength
        val y = adjustedSlot / totalLength

        return !(x < 0 || x >= length || y < 0 || y >= height)
    }


    override fun copy(): Pane {
        val pane = SubmitItemPane(slot, length, height, filter, priority)

        pane._items.putAll(_items.map { (slot, item) -> slot to item.clone() }.toMap(pane._items))
        pane.isVisible = isVisible
        pane.onClick = onClick
        pane.uuid = uuid

        return pane
    }

    override fun getItems(): MutableCollection<GuiItem> = mutableListOf()
    override fun getPanes(): MutableCollection<Pane> = mutableListOf()
    override fun clear() {
    }
}