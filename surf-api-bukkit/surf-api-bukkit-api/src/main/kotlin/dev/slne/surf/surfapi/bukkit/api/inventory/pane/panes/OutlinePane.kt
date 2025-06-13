package dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Flippable
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Mask
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Orientable
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.utils.Rotatable
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.*
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.mutableObject2IntMapOf
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.objectListOf
import it.unimi.dsi.fastutil.objects.Object2IntMap
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.*

@OptIn(NmsUseWithCaution::class)
class OutlinePane(
    slot: Slot,
    length: Int,
    height: Int,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : Pane(slot, length, height, priority, uuid), Orientable, Flippable, Rotatable {

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
    private var mask: Mask

    override val panes = objectListOf<Pane>()
    override val items = mutableObjectListOf<GuiItem>(length * height)

    init {
        val mask = Array(height) { "" }
        val maskString = buildString {
            for (i in 0 until length) {
                append("1")
            }
        }

        Arrays.fill(mask, maskString)

        this.mask = Mask(*mask)
    }

    override fun display(
        component: InventoryComponent,
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

            val displayItems: Array<GuiItem?> = if (repeat) {
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
                        component.setItem(item, slot(finalColumn, finalRow))
                    }
                }

                index++
            }
        }
    }

    override fun updateItem(item: UpdatableGuiItem): Int? {
        for ((index, guiItem) in items.withIndex()) {
            if (guiItem != item) continue
            if (!item.update(item)) return null

            return index + slot.getX(length) + slot.getY(length) * length
        }

        return null
    }

    override fun updateItems(): Object2IntMap<GuiItem> {
        val updatedItems = mutableObject2IntMapOf<GuiItem>()

        for ((index, guiItem) in items.withIndex()) {
            if (guiItem !is UpdatableGuiItem) continue
            if (!guiItem.update(guiItem)) continue

            updatedItems[guiItem] = index + slot.getX(length) + slot.getY(length) * length
        }

        return updatedItems
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

        val itemStack = event.currentItem ?: return false
        val item = findMatchingItem(items, itemStack) ?: return false

        item.callAction(event)

        return true
    }

    override fun clone(): OutlinePane {
        val outlinePane = OutlinePane(
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

    fun insertItem(item: GuiItem, index: Int) {
        items.add(index, item)
    }

    fun addItem(item: GuiItem) {
        items.add(item)
    }

    fun removeItem(item: GuiItem) {
        items.remove(item)
    }

    override fun clear() {
        items.clear()
    }

    fun applyMask(mask: Mask) {
        if (length != mask.length || height != mask.height) {
            throw IllegalArgumentException("Mask dimensions must match pane dimensions.")
        }

        this.mask = mask
    }

    enum class Alignment {
        BEGIN, CENTER
    }

}