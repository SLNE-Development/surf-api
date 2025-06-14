package dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.StaticPane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.slot
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.AbstractPane
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.GeometryUtils
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.InventoryComponentImpl
import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import dev.slne.surf.surfapi.core.api.util.objectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

class StaticPaneImpl(
    slot: Slot,
    length: Int,
    height: Int,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : AbstractPane(slot, length, height, priority, uuid), StaticPane {

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

    override val panes: ObjectList<AbstractPane> = objectListOf()

    private val paneItems = mutableObject2ObjectMapOf<Slot, GuiItemImpl>()
    override val items: ObjectList<GuiItemImpl> get() = paneItems.values.toObjectList()

    override fun display(
        component: InventoryComponentImpl,
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

                val finalRow = this@StaticPaneImpl.slot.getY(maxLength) + y + paneOffsetY
                val finalColumn = this@StaticPaneImpl.slot.getX(maxLength) + x + paneOffsetX

                component.setItem(guiItem, slot(finalColumn, finalRow))
            }
    }

    override fun updateItems(): Object2IntMap<GuiItemImpl> {
        val updatedItems = mutableObject2IntMapOf<GuiItemImpl>()

        for ((slot, guiItem) in paneItems.entries) {
            if (guiItem !is UpdatableGuiItemImpl) continue
            if (!guiItem.update()) continue

            val index = slot.getX(length) + slot.getY(length) * length
            updatedItems[guiItem] = index
        }

        return updatedItems
    }

    override fun updateItem(item: UpdatableGuiItemImpl): Int? {
        for ((slot, other) in paneItems.entries) {
            if (other != item) continue
            if (!item.update()) return null
            return slot.getX(length) + slot.getY(length) * length
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

    override fun clone(): StaticPaneImpl {
        val clonedPane = StaticPaneImpl(slot, length, height, priority, uuid)

        for (entry in paneItems.entries) {
            clonedPane.setItem(entry.key, entry.value.clone())
        }

        clonedPane.visible = visible
        clonedPane.onClick = onClick
        clonedPane.flippedHorizontally = flippedHorizontally
        clonedPane.flippedVertically = flippedVertically
        clonedPane.rotation = rotation

        return clonedPane
    }

    override fun fillWith(item: ItemStack, handler: ClickHandlerDsl) {
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
                    setItem(slot(x, y), GuiItemImpl().apply {
                        item(item)
                        onClick(handler)
                    })
                }
            }
        }
    }

    override fun setItem(slot: Slot, item: GuiItem) {
        require(item is GuiItemImpl) { "Item must be an instance of GuiItemImpl" }
        paneItems.put(slot, item)
    }

    override fun setItem(slot: Slot, init: GuiItem.() -> Unit) {
        setItem(slot, GuiItemImpl().apply(init))
    }

    override fun removeItem(item: GuiItem) {
        paneItems.values.removeIf { guiItem -> guiItem == item }
    }

    override fun removeItem(slot: Slot) {
        paneItems.remove(slot)
    }

    override fun clear() {
        paneItems.clear()
    }
}