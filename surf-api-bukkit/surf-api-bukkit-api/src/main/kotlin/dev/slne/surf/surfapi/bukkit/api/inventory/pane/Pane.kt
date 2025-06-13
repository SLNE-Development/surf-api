package dev.slne.surf.surfapi.bukkit.api.inventory.pane

import com.jeff_media.morepersistentdatatypes.DataType
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.InventoryComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Priority
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.view.InventoryViewUtil
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.*

@OptIn(NmsUseWithCaution::class)
abstract class Pane(
    var slot: Slot,
    open var length: Int,
    open var height: Int,
    var priority: Priority = Priority.NORMAL,
    val uuid: UUID = UUID.randomUUID(),
) : Cloneable {

    var onClick: ((InventoryClickEvent) -> Unit)? = null
    var visible: Boolean = true

    internal abstract fun display(
        component: InventoryComponent,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    )

    internal abstract fun updateItems(): Object2IntMap<GuiItem>
    internal abstract fun updateItem(item: UpdatableGuiItem): Int?

    internal abstract fun click(
        gui: Gui,
        component: InventoryComponent,
        event: InventoryClickEvent,
        slot: Int,
        paneOffsetX: Int,
        paneOffsetY: Int,
        maxLength: Int,
        maxHeight: Int,
    ): Boolean

    abstract val items: ObjectList<GuiItem>
    abstract val panes: ObjectList<Pane>

    abstract fun clear()

    protected fun callOnClick(event: InventoryClickEvent) {
        if (onClick == null) {
            return
        }

        try {
            onClick?.invoke(event)
        } catch (exception: Exception) {
            throw RuntimeException(buildString {
                append("Exception while handling click event in inventory '")
                append(InventoryViewUtil.getTitle(event.view))
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
            val meta = item.itemMeta ?: return false

            return guiItem.uuid == meta.persistentDataContainer.get(
                guiItem.key,
                DataType.UUID
            )
        }

        @JvmStatic
        protected fun <T : GuiItem> findMatchingItem(
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

    public override fun clone(): Pane {
        throw UnsupportedOperationException("The implementing pane has not overridden the clone method.")
    }

    override fun toString(): String {
        return "Pane(slot=$slot, length=$length, height=$height, priority=$priority, uuid=$uuid, onClick=$onClick, visible=$visible, items=$items, panes=$panes)"
    }

    init {
        if (length <= 0 || height <= 0) {
            throw IllegalArgumentException("Length and height must be greater than 0 (length=$length, height=$height)")
        }
    }
}