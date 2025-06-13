package dev.slne.surf.surfapi.bukkit.api.inventory.pane.components

import dev.slne.surf.surfapi.bukkit.api.builder.ItemStack
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.panes.PaginatedPane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.slot
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.object2IntMapOf
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

@OptIn(NmsUseWithCaution::class)
class PagingButtons(
    slot: Slot,
    val paginatedPane: PaginatedPane,
    length: Int = 9,
    priority: Priority = Priority.NORMAL,
    uuid: UUID = UUID.randomUUID(),
) : Pane(slot, length, 1, priority, uuid) {

    override val items: ObjectList<GuiItem>
        get() = mutableObjectListOf(backwardsButton, forwardsButton)

    override val panes: ObjectList<Pane>
        get() = mutableObjectListOf()

    override fun clear() {
        throw UnsupportedOperationException("PagingButtons cannot be cleared.")
    }

    internal var backwardsButton = GuiItem(ItemStack(Material.ARROW) {
        displayName {
            primary("Backwards")
        }
    })

    fun setBackwardsButton(
        item: GuiItem,
        action: (InventoryClickEvent) -> Unit = {},
    ): PagingButtons {
        this.backwardsButton = item.apply {
            onClick = action
        }

        return this
    }

    fun setBackwardsButton(
        item: ItemStack,
        action: (InventoryClickEvent) -> Unit = {},
    ) = setBackwardsButton(GuiItem(item), action)

    internal var forwardsButton = GuiItem(ItemStack(Material.ARROW) {
        displayName {
            primary("Forwards")
        }
    })

    fun setForwardsButton(
        item: GuiItem,
        action: (InventoryClickEvent) -> Unit = {},
    ): PagingButtons {
        this.forwardsButton = item.apply {
            onClick = action
        }

        return this
    }

    fun setForwardsButton(
        item: ItemStack,
        action: (InventoryClickEvent) -> Unit = {},
    ) = setForwardsButton(GuiItem(item), action)

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
        component: InventoryComponent,
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

        if (paginatedPane.page < paginatedPane.getPages() - 1) {
            component.setItem(forwardsButton, slot(x + length - 1, y))
        }
    }

    override fun updateItems(): Object2IntMap<GuiItem> = object2IntMapOf()

    override fun updateItem(item: UpdatableGuiItem): Int? {
        return null
    }

    override fun clone(): PagingButtons {
        val pagingButtons = PagingButtons(
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