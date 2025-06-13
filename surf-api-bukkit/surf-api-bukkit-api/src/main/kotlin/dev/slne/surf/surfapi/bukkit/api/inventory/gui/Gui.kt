package dev.slne.surf.surfapi.bukkit.api.inventory.gui

import dev.slne.surf.surfapi.bukkit.api.inventory.item.GuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import dev.slne.surf.surfapi.bukkit.api.inventory.utils.PlayerInventoryCache
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class Gui internal constructor(
    val parent: Gui? = null,
) : Cloneable {

    private val log = logger()

    lateinit var backingInventory: Inventory

    internal val cache = PlayerInventoryCache()

    private var onTopClick: (InventoryClickEvent) -> Unit = {}
    fun onTopClick(block: (InventoryClickEvent) -> Unit) {
        onTopClick = block
    }

    private var onBottomClick: (InventoryClickEvent) -> Unit = {}
    fun onBottomClick(block: (InventoryClickEvent) -> Unit) {
        onBottomClick = block
    }

    private var onGlobalClick: (InventoryClickEvent) -> Unit = {}
    fun onGlobalClick(block: (InventoryClickEvent) -> Unit) {
        onGlobalClick = block
    }

    private var onOutsideClick: (InventoryClickEvent) -> Unit = {}
    fun onOutsideClick(block: (InventoryClickEvent) -> Unit) {
        onOutsideClick = block
    }

    private var onTopDrag: (InventoryDragEvent) -> Unit = {}
    fun onTopDrag(block: (InventoryDragEvent) -> Unit) {
        onTopDrag = block
    }

    private var onBottomDrag: (InventoryDragEvent) -> Unit = {}
    fun onBottomDrag(block: (InventoryDragEvent) -> Unit) {
        onBottomDrag = block
    }

    private var onGlobalDrag: (InventoryDragEvent) -> Unit = {}
    fun onGlobalDrag(block: (InventoryDragEvent) -> Unit) {
        onGlobalDrag = block
    }

    var onClose: (InventoryCloseEvent) -> Unit = {}

    var shouldNavigateParentOnClose: Boolean = false

    internal var updating: Boolean = false
        private set

    abstract fun show(player: Player)
    abstract fun click(event: InventoryClickEvent)
    abstract fun isPlayerInventoryUsed(): Boolean
    abstract val viewers: ObjectSet<Player>
    protected abstract fun updateAllItems(): Object2IntMap<GuiItem>
    protected abstract fun updateItem0(item: UpdatableGuiItem): Int?

    fun update() {
        updating = true

        val updatedItems = updateAllItems()
        for ((item, slot) in updatedItems) {
            if (item.visible) {
                backingInventory.setItem(slot, item.itemStack)
            } else {
                backingInventory.setItem(slot, ItemStack.empty())
            }
        }

        for (viewer in viewers) {
            val cursor = viewer.itemOnCursor

            viewer.setItemOnCursor(ItemStack.empty())
            show(viewer)
            viewer.setItemOnCursor(cursor)
        }

        updating = false
    }

    fun updateItem(item: UpdatableGuiItem) {
        if (updating) return

        val slot = updateItem0(item) ?: return
        if (item.visible) {
            backingInventory.setItem(slot, item.itemStack)
        } else {
            backingInventory.setItem(slot, ItemStack.empty())
        }

        updating = false
    }

    protected fun addInventory(inventory: Inventory, gui: Gui) {
        GUI_INVENTORIES[inventory] = gui
    }

    internal fun callOnTopClick(event: InventoryClickEvent) {
        callCallback(onTopClick, event, "onTopClick")
    }

    internal fun callOnBottomClick(event: InventoryClickEvent) {
        callCallback(onBottomClick, event, "onBottomClick")
    }

    internal fun callOnGlobalClick(event: InventoryClickEvent) {
        callCallback(onGlobalClick, event, "onGlobalClick")
    }

    internal fun callOnOutsideClick(event: InventoryClickEvent) {
        callCallback(onOutsideClick, event, "onOutsideClick")
    }

    internal fun callOnTopDrag(event: InventoryDragEvent) {
        callCallback(onTopDrag, event, "onTopDrag")
    }

    internal fun callOnBottomDrag(event: InventoryDragEvent) {
        callCallback(onBottomDrag, event, "onBottomDrag")
    }

    internal fun callOnGlobalDrag(event: InventoryDragEvent) {
        callCallback(onGlobalDrag, event, "onGlobalDrag")
    }

    internal fun callOnClose(event: InventoryCloseEvent) {
        callCallback(onClose, event, "onClose")
    }

    private fun <T : InventoryEvent> callCallback(
        callback: (T).() -> Unit,
        event: T,
        name: String,
    ) {
        try {
            callback.invoke(event)
        } catch (exception: Exception) {
            log.atSevere().withCause(exception).log(buildString {
                append("Exception while handling $name")

                if (event is InventoryClickEvent) {
                    append(", slot=${event.slot}")
                }
            })
        }
    }

    fun navigateToParent(player: Player) {
        if (parent == null) return

        parent.show(player)
        parent.update()
    }

    fun walkParents(): Sequence<Gui> = generateSequence(this) { it.parent }

    companion object {
        private val GUI_INVENTORIES = WeakHashMap<Inventory, Gui>()

        fun getGui(inventory: Inventory) = GUI_INVENTORIES[inventory]
    }
}