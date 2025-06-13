package dev.slne.surf.surfapi.bukkit.api.inventory.item

import com.jeff_media.morepersistentdatatypes.DataType
import dev.slne.surf.surfapi.bukkit.api.inventory.view.InventoryViewUtil
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

internal val GUI_ITEM_UUID_KEY = NamespacedKey(
    JavaPlugin.getProvidingPlugin(GuiItem::class.java),
    "sapi-uuid"
)

@OptIn(NmsUseWithCaution::class)
open class GuiItem(
    var itemStack: ItemStack,
    internal var action: (InventoryClickEvent) -> Unit = {},
    val key: NamespacedKey = GUI_ITEM_UUID_KEY,
    val uuid: UUID = UUID.randomUUID(),
) : Cloneable {

    var visible: Boolean = true

    init {
        applyUuid()
    }

    fun onClick(action: (InventoryClickEvent) -> Unit) {
        this.action = action
    }

    public override fun clone(): GuiItem {
        val guiItem = GuiItem(itemStack.clone(), action, key, uuid)

        guiItem.visible = visible
        val meta = guiItem.itemStack.itemMeta

        if (meta != null) {
            meta.persistentDataContainer.set(key, DataType.UUID, guiItem.uuid)
            guiItem.itemStack.itemMeta = meta
        }

        return guiItem
    }

    fun callAction(event: InventoryClickEvent) {
        try {
            action.invoke(event)
        } catch (exception: Exception) {
            log.atSevere().withCause(exception).log(
                "Exception while handling click event in inventory '${
                    InventoryViewUtil.getTitle(event.view)
                }', slot=${event.slot}, item=${itemStack.type}"
            )
        }
    }

    fun applyUuid() {
        itemStack.editMeta {
            it.persistentDataContainer.set(key, DataType.UUID, uuid)
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GuiItem

        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun toString(): String {
        return "GuiItem(itemStack=$itemStack, action=$action, key=$key, uuid=$uuid, visible=$visible)"
    }

    companion object {
        private val log = logger()
    }
}