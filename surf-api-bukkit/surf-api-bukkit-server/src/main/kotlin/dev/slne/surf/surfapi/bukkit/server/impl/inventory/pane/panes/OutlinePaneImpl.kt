package dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.OutlinePane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Mask
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Orientable
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.AbstractPane
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.utils.MaskImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.GeometryUtils
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.InventoryComponentImpl
import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.objectListOf
import it.unimi.dsi.fastutil.objects.Object2IntMap
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.*

class OutlinePaneImpl(
    slot: Slot,
    length: Int,
    height: Int,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : AbstractPane(slot, length, height, priority, uuid), OutlinePane {

    override var length: Int = super.length
        get() = super.length
        set(value) {
            field = value

            applyMask(mask.setLength(value))
        }

    override var height: Int = super.height
        get() = super.height
        set(value) {
            field = value

            applyMask(mask.setHeight(value))
        }

    override var orientation = Orientable.Orientation.HORIZONTAL
    override var flippedHorizontally = false
    override var flippedVertically = false
    override var rotation = 0
        set(value) {
            if (length != height) {
                throw IllegalArgumentException("Rotation is only allowed for square panes.")
            }

            if (rotation % 90 != 0) {
                throw IllegalArgumentException("Rotation must be a multiple of 90 degrees.")
            }

            field = value % 360
        }

    private var gap = 0
    private var repeat = false

    private var alignment = Alignment.BEGIN
    private var mask: MaskImpl

    override val panes = objectListOf<AbstractPane>()
    override val items = mutableObjectListOf<GuiItemImpl>(length * height)

    init {
        val mask = Array(height) { "" }
        val maskString = buildString {
            for (i in 0 until length) {
                append("1")
            }
        }

        mask.fill(maskString)
        this.mask = MaskImpl(*mask)
    }

    override fun display(
        component: InventoryComponentImpl,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    ) {
        val length = minOf(length, maxLength)
        val height = minOf(height, maxHeight)

        var itemIndex = 0
        var gapCount = 0

        val size = when (orientation) {
            Orientable.Orientation.HORIZONTAL -> {
                height
            }

            Orientable.Orientation.VERTICAL -> {
                length
            }
        }

        for (vectorIndex in 0 until size) {
            if (items.size <= itemIndex) break

            val maskLine = when (orientation) {
                Orientable.Orientation.HORIZONTAL -> {
                    mask.getRow(vectorIndex)
                }

                Orientable.Orientation.VERTICAL -> {
                    mask.getColumn(vectorIndex)
                }
            }

            var enabled = 0
            for (bool in maskLine) {
                if (bool) enabled++
            }

            val displayItems: Array<GuiItemImpl?> = if (repeat) {
                arrayOfNulls(enabled)
            } else {
                val remaining = gapCount + (items.size - itemIndex - 1) * (gap + 1) + 1

                arrayOfNulls(minOf(enabled, remaining))
            }

            for (index in 0 until displayItems.size) {
                if (gapCount == 0) {
                    displayItems[index] = items[itemIndex]

                    itemIndex++

                    if (repeat && itemIndex >= items.size) {
                        itemIndex = 0
                    }

                    gapCount = gap
                } else {
                    displayItems[index] = null

                    gapCount--
                }
            }

            var index = if (alignment == Alignment.BEGIN) {
                0
            } else {
                -((enabled - displayItems.size) / 2)
            }
            for (opposingVectorIndex in 0 until maskLine.size) {
                if (!maskLine[opposingVectorIndex]) {
                    continue
                }

                if (index >= 0 && index < displayItems.size && displayItems[index] != null) {
                    var x: Int
                    var y: Int

                    when (orientation) {
                        Orientable.Orientation.HORIZONTAL -> {
                            x = opposingVectorIndex
                            y = vectorIndex
                        }

                        Orientable.Orientation.VERTICAL -> {
                            x = vectorIndex
                            y = opposingVectorIndex
                        }
                    }

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
                        continue
                    }

                    val finalRow = slot.getY(maxLength) + y + paneOffsetY
                    val finalColumn = slot.getX(maxLength) + x + paneOffsetX

                    val item = displayItems[index] ?: continue
                    if (item.visible) {
                        component.setItem(item, Slot.fromXY(finalColumn, finalRow))
                    }
                }

                index++
            }
        }
    }

    override fun updateItem(item: UpdatableGuiItemImpl): Int? {
        for ((index, guiItem) in items.withIndex()) {
            if (guiItem != item) continue
            if (!item.update()) return null

            return index + slot.getX(length) + slot.getY(length) * length
        }

        return null
    }

    override fun updateItems(): Object2IntMap<GuiItemImpl> {
        val updatedItems = mutableObject2IntMapOf<GuiItemImpl>()

        for ((index, guiItem) in items.withIndex()) {
            if (guiItem !is UpdatableGuiItemImpl) continue
            if (!guiItem.update()) continue

            updatedItems[guiItem] = index + slot.getX(length) + slot.getY(length) * length
        }

        return updatedItems
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

        val itemStack = event.currentItem ?: return false
        val item = findMatchingItem(items, itemStack) ?: return false

        item.callAction(event)

        return true
    }

    override fun clone(): OutlinePaneImpl {
        val outlinePane = OutlinePaneImpl(
            slot,
            length,
            height,
            priority,
            uuid
        )

        for (item in items) {
            outlinePane.addItem(item.clone())
        }

        outlinePane.visible = visible
        outlinePane.onClick = onClick

        outlinePane.orientation = orientation
        outlinePane.flippedHorizontally = flippedHorizontally
        outlinePane.flippedVertically = flippedVertically
        outlinePane.rotation = rotation
        outlinePane.gap = gap
        outlinePane.repeat = repeat
        outlinePane.alignment = alignment
        outlinePane.mask = mask

        return outlinePane
    }

    override fun setItem(index: Int, item: GuiItem) {
        require(item is GuiItemImpl) { "Item must be of type GuiItemImpl" }
        items.add(index, item)
    }

    override fun addItem(item: GuiItem) {
        require(item is GuiItemImpl) { "Item must be of type GuiItemImpl" }
        items.add(item)
    }

    override fun removeItem(item: GuiItem) {
        require(item is GuiItemImpl) { "Item must be of type GuiItemImpl" }
        items.remove(item)
    }

    override fun clear() {
        items.clear()
    }

    override fun applyMask(mask: Mask) {
        require(mask is MaskImpl) { "Mask must be of type MaskImpl" }
        if (length != mask.length || height != mask.height) {
            throw IllegalArgumentException("Mask dimensions must match pane dimensions.")
        }

        this.mask = mask
    }

    enum class Alignment {
        BEGIN, CENTER
    }

}