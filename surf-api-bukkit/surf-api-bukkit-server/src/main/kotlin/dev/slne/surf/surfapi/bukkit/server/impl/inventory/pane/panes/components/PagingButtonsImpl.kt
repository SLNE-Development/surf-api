package dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes.components

import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.components.PagingButtons
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.slot
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.AbstractPane
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane.panes.PaginatedPaneImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.InventoryComponentImpl
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.object2IntMapOf
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import java.util.*

class PagingButtonsImpl(
    slot: Slot,
    val paginatedPane: PaginatedPaneImpl,
    length: Int = 9,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : AbstractPane(slot, length, 1, priority, uuid), PagingButtons {

    override val items: ObjectList<GuiItemImpl>
        get() = mutableObjectListOf(backwardsButton, forwardsButton)

    override val panes: ObjectList<AbstractPane>
        get() = mutableObjectListOf()

    override fun clear() {
        throw UnsupportedOperationException("PagingButtons cannot be cleared.")
    }

    var backwardsButton = GuiItemImpl().apply {
        item(ItemType.ARROW) {
            displayName {
                primary("Backwards")
            }
        }
    }

    override fun setBackwardsButton(item: GuiItem) {
        require(item is GuiItemImpl) { "Backwards button must be an instance of GuiItemImpl." }
        this.backwardsButton = item
    }

    override fun setBackwardsButton(item: ItemStack, action: ClickHandlerDsl) {
        setBackwardsButton(GuiItem {
            item(item)
            onClick(action)
        })
    }

    var forwardsButton = GuiItemImpl().apply {
        item(ItemType.ARROW) {
            displayName {
                primary("Forwards")
            }
        }
    }

    override fun setForwardsButton(item: GuiItem) {
        require(item is GuiItemImpl) { "Forwards button must be an instance of GuiItemImpl." }
        this.forwardsButton = item
    }

    override fun setForwardsButton(item: ItemStack, action: ClickHandlerDsl) {
        setForwardsButton(GuiItem {
            item(item)
            onClick(action)
        })
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

        if (matchesItem(backwardsButton, itemStack)) {
            paginatedPane.previousPage()
            backwardsButton.callAction(event)
            gui.update()

            return true
        }

        if (matchesItem(forwardsButton, itemStack)) {
            paginatedPane.nextPage()
            forwardsButton.callAction(event)
            gui.update()

            return true
        }

        return false
    }

    override fun display(
        component: InventoryComponentImpl,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    ) {
        val length = length.coerceAtMost(maxLength)

        val x = super.slot.getX(length) + paneOffsetX
        val y = super.slot.getY(length) + paneOffsetY

        if (paginatedPane.page > 0) {
            component.setItem(backwardsButton, slot(x, y))
        }

        if (paginatedPane.page < paginatedPane.totalPages - 1) {
            component.setItem(forwardsButton, slot(x + length - 1, y))
        }
    }

    override fun updateItems(): Object2IntMap<GuiItemImpl> = object2IntMapOf()

    override fun updateItem(item: UpdatableGuiItemImpl): Int? {
        return null
    }

    override fun clone(): PagingButtonsImpl {
        val pagingButtons = PagingButtonsImpl(
            slot,
            paginatedPane,
            length,
            priority,
            uuid
        )

        pagingButtons.visible = visible
        pagingButtons.onClick = onClick
        pagingButtons.backwardsButton = backwardsButton.clone()
        pagingButtons.forwardsButton = forwardsButton.clone()

        return pagingButtons
    }

    init {
        if (length < 2) {
            throw IllegalArgumentException("Length must be at least 2 to accommodate paging buttons.")
        }
    }

}