package dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.PaneMarker
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Flippable
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Rotatable
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.*
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.objectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

@OptIn(NmsUseWithCaution::class)
class StaticPane(
    slot: Slot,
    length: Int,
    height: Int,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : Pane(slot, length, height, priority, uuid), Flippable, Rotatable {

    override var flippedHorizontally: Boolean = false
    override var flippedVertically: Boolean = false
    override var rotation: Int = 0
        set(value) {
            if (length != height) {
                throw IllegalArgumentException("Rotation is only allowed for square panes.")
            }

            if (rotation % 90 != 0) {
                throw IllegalArgumentException("Rotation must be a multiple of 90 degrees.")
            }

            field = value % 360
        }

    override val panes: ObjectList<Pane> get() = objectListOf()

    private val paneItems = mutableObject2ObjectMapOf<Slot, GuiItem>()
    override val items: ObjectList<GuiItem> get() = paneItems.values.toObjectList()

    fun item(slot: Slot, itemStack: ItemStack, init: (@PaneMarker GuiItem).() -> Unit) {
        setItem(GuiItem(itemStack).apply(init), slot)
    }

    fun updatableItem(
        slot: Slot,
        itemStack: ItemStack,
        init: (@PaneMarker UpdatableGuiItem).() -> Unit,
    ) {
        setItem(UpdatableGuiItem(itemStack).apply(init), slot)
    }

    override fun display(
        component: InventoryComponent,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    ) {
        val length = minOf(this.length, maxLength)
        val height = minOf(this.height, maxHeight)

        paneItems.entries.asSequence()
            .filter { it.value.visible }
            .forEach { (slot, guiItem) ->
                var x = slot.getX(length)
                var y = slot.getY(length)

                if (flippedHorizontally) {
                    x = length - x - 1
                }

                if (flippedVertically) {
                    y = height - y - 1
                }

                val coordinates = GeometryUtils.processClockwiseRotation(
                    x,
                    y,
                    length,
                    height,
                    rotation
                )

                x = coordinates.intKey
                y = coordinates.intValue

                if (x < 0 || x >= length || y < 0 || y >= height) {
                    return@forEach
                }

                val finalRow = this@StaticPane.slot.getY(maxLength) + y + paneOffsetY
                val finalColumn = this@StaticPane.slot.getX(maxLength) + x + paneOffsetX

                component.setItem(guiItem, slot(finalColumn, finalRow))
            }
    }

    override fun updateItems(): Object2IntMap<GuiItem> {
        val updatedItems = mutableObject2IntMapOf<GuiItem>()

        for ((slot, guiItem) in paneItems.entries) {
            if (guiItem !is UpdatableGuiItem) continue
            if (!guiItem.update(guiItem)) continue

            val index = slot.getX(length) + slot.getY(length) * length
            updatedItems[guiItem] = index
        }

        return updatedItems
    }

    override fun updateItem(item: UpdatableGuiItem): Int? {
        for ((slot, other) in paneItems.entries) {
            if (other != item) continue
            if (!item.update(item)) return null
            return slot.getX(length) + slot.getY(length) * length
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
        val length = minOf(this.length, maxLength)
        val height = minOf(this.height, maxHeight)

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

        val itemStack = event.currentItem ?: return false
        val clickedItem = findMatchingItem(paneItems.values, itemStack) ?: return false

        clickedItem.callAction(event)

        return true
    }

    override fun clone(): StaticPane {
        val clonedPane = StaticPane(slot, length, height, priority, uuid)

        for (entry in paneItems.entries) {
            clonedPane.setItem(entry.value.clone(), entry.key)
        }

        clonedPane.visible = visible
        clonedPane.onClick = onClick
        clonedPane.flippedHorizontally = flippedHorizontally
        clonedPane.flippedVertically = flippedVertically
        clonedPane.rotation = rotation

        return clonedPane
    }

    fun fillWith(itemStack: ItemStack, action: (InventoryClickEvent) -> Unit) {
        val locations = paneItems.keys

        for (y in 0 until height) {
            for (x in 0 until length) {
                var found = false

                for (location in locations) {
                    if (location.getX(length) == x && location.getY(length) == y) {
                        found = true
                        break
                    }
                }

                if (!found) {
                    setItem(GuiItem(itemStack, action), slot(x, y))
                }
            }
        }
    }


    fun fillWith(itemStack: ItemStack) {
        fillWith(itemStack) {}
    }

    fun setItem(item: GuiItem, slot: Slot) {
        paneItems.put(slot, item)
    }

    fun removeItem(item: GuiItem) {
        paneItems.values.removeIf { guiItem -> guiItem == item }
    }

    fun removeItem(slot: Slot) {
        paneItems.remove(slot)
    }

    override fun clear() {
        paneItems.clear()
    }

}