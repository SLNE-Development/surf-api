package dev.slne.surf.surfapi.bukkit.server.impl.inventory.pane

import com.jeff_media.morepersistentdatatypes.DataType
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandler
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.pane.Pane
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.dsl.ClickHandlerScopeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.InventoryComponentImpl
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class AbstractPane(
    override var slot: Slot,
    override var length: Int,
    override var height: Int,
    override var priority: Priority = Priority.NORMAL,
    override val uuid: UUID = UUID.randomUUID(),
) : Pane {
    var onClick: ClickHandler? = null
    override var visible: Boolean = true

    init {
        require(length >= 0) { "Length must be greater than or equal to 0 (length=$length)" }
        require(height >= 0) { "Height must be greater than or equal to 0 (height=$height)" }
    }

    override fun onClick(handler: ClickHandler) {
        onClick = handler
    }

    override fun onClick(handler: ClickHandlerDsl) {
        onClick(ClickHandlerScopeImpl(handler))
    }

    abstract fun display(
        component: InventoryComponentImpl,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    )

    abstract fun updateItems(): Object2IntMap<GuiItemImpl>
    abstract fun updateItem(item: UpdatableGuiItemImpl): Int?

    abstract fun click(
        gui: AbstractGui,
        component: InventoryComponentImpl,
        event: InventoryClickEvent,
        slot: Int,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    ): Boolean

    abstract override val items: ObjectList<GuiItemImpl>
    abstract override val panes: ObjectList<AbstractPane>

    protected fun callOnClick(event: InventoryClickEvent) {
        try {
            onClick?.handle(event)
        } catch (exception: Exception) {
            throw RuntimeException(buildString {
                append("Exception while handling click event in inventory '")
                append(event.view.title())
                append(
                    "slot=${event.slot}, for ${javaClass.simpleName}, x=${slot.getX(length)}, y=${
                        slot.getY(
                            length
                        )
                    }, length=$length, height=$height"
                )
            }, exception)
        }
    }

    companion object {
        @JvmStatic
        protected fun matchesItem(
            guiItem: GuiItem,
            item: ItemStack,
        ): Boolean {
            return guiItem.uuid == item.persistentDataContainer.get(guiItem.key, DataType.UUID)
        }

        @JvmStatic
        protected fun <T : GuiItemImpl> findMatchingItem(
            items: Collection<T>,
            item: ItemStack,
        ): T? {
            for (guiItem in items) {
                if (matchesItem(guiItem, item)) {
                    return guiItem
                }
            }

            return null
        }
    }

    public override fun clone(): AbstractPane {
        throw UnsupportedOperationException("The implementing pane has not overridden the clone method.")
    }

    override fun toString(): String {
        return "Pane(slot=$slot, length=$length, height=$height, priority=$priority, uuid=$uuid, onClick=$onClick, visible=$visible, items=$items, panes=$panes)"
    }
}