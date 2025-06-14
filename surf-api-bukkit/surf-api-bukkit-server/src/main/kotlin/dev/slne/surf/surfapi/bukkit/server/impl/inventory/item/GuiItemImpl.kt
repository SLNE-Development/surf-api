package dev.slne.surf.surfapi.bukkit.server.impl.inventory.item

import com.jeff_media.morepersistentdatatypes.DataType
import dev.slne.surf.surfapi.bukkit.api.builder.meta
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandler
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.ClickHandlerDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.util.key
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.dsl.ClickHandlerScopeImpl
import dev.slne.surf.surfapi.core.api.util.logger
import org.bukkit.NamespacedKey
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ItemType
import java.util.*

open class GuiItemImpl(
    override val key: NamespacedKey = DEFAULT_KEY,
    override val uuid: UUID = UUID.randomUUID(),
) : GuiItem, Cloneable {
    private var _itemStack: ItemStack? = null
        set(value) {
            field = value
            applyUuid()
        }

    override val itemStack: ItemStack
        get() = _itemStack ?: error("ItemStack is not initialized.")

    override var visible: Boolean = true
    var action: ClickHandler? = null
        private set

    override fun onClick(handler: ClickHandler) {
        action = handler
    }

    override fun onClick(handler: ClickHandlerDsl) {
        onClick(ClickHandlerScopeImpl(handler))
    }

    override fun item(itemStack: ItemStack) {
        _itemStack = itemStack
    }

    override fun item(
        type: ItemType,
        block: ItemStack.() -> Unit,
    ) {
        item(type.createItemStack().apply { block() })
    }

    fun callAction(event: InventoryClickEvent) {
        try {
            action?.handle(event)
        } catch (exception: Exception) {
            log.atSevere()
                .withCause(exception)
                .log(
                    "Exception while handling click event in inventory '${event.view.title()}', slot=${event.slot}, item=${itemStack.type}"
                )
        }
    }

    fun applyUuid() {
        itemStack.meta {
            persistentDataContainer.set(key, DataType.UUID, uuid)
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GuiItemImpl

        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    override fun toString(): String {
        return "GuiItem(itemStack=$_itemStack, action=$action, key=$key, uuid=$uuid, visible=$visible)"
    }

    companion object {
        private val log = logger()
        val DEFAULT_KEY = key("surf-api-uuid")
    }
}