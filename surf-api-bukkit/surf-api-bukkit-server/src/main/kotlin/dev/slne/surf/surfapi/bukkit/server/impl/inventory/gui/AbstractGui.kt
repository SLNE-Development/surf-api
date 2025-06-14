package dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui

import dev.slne.surf.surfapi.bukkit.api.inventory.gui.Gui
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.*
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.dsl.ClickHandlerScopeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.dsl.CloseHandlerScopeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.dsl.DragHandlerScopeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.GuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.item.UpdatableGuiItemImpl
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.utils.PlayerInventoryCache
import dev.slne.surf.surfapi.core.api.util.logger
import it.unimi.dsi.fastutil.objects.Object2IntMap
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import java.util.*

abstract class AbstractGui(override val parent: AbstractGui? = null) : Gui {
    val cache = PlayerInventoryCache()

    override lateinit var backingInventory: Inventory

    private var onTopClick: ClickHandler? = null
    private var onBottomClick: ClickHandler? = null
    private var onGlobalClick: ClickHandler? = null
    private var onOutsideClick: ClickHandler? = null
    private var onTopDrag: DragHandler? = null
    private var onBottomDrag: DragHandler? = null
    private var onGlobalDrag: DragHandler? = null
    private var onClose: CloseHandler? = null

    var shouldNavigateToParentOnClose: Boolean = false

    @Volatile
    var updating: Boolean = false
        private set

    abstract fun click(event: InventoryClickEvent)
    abstract fun isPlayerInventoryUsed(): Boolean
    protected abstract fun updateAllItems(): Object2IntMap<GuiItemImpl>
    protected abstract fun updateItem0(item: UpdatableGuiItemImpl): Int?

    // region Handlers
    override fun onTopClick(handler: ClickHandler) {
        onTopClick = handler
    }

    override fun onTopClick(handler: ClickHandlerDsl) = onTopClick(ClickHandlerScopeImpl(handler))

    override fun onBottomClick(handler: ClickHandler) {
        onBottomClick = handler
    }

    override fun onBottomClick(handler: ClickHandlerDsl) =
        onBottomClick(ClickHandlerScopeImpl(handler))

    override fun onGlobalClick(handler: ClickHandler) {
        onGlobalClick = handler
    }

    override fun onGlobalClick(handler: ClickHandlerDsl) =
        onGlobalClick(ClickHandlerScopeImpl(handler))

    override fun onOutsideClick(handler: ClickHandler) {
        onOutsideClick = handler
    }

    override fun onOutsideClick(handler: ClickHandlerDsl) =
        onOutsideClick(ClickHandlerScopeImpl(handler))

    override fun onTopDrag(handler: DragHandler) {
        onTopDrag = handler
    }

    override fun onTopDrag(handler: DragHandlerDsl) = onTopDrag(DragHandlerScopeImpl(handler))

    override fun onBottomDrag(handler: DragHandler) {
        onBottomDrag = handler
    }

    override fun onBottomDrag(handler: DragHandlerDsl) = onBottomDrag(DragHandlerScopeImpl(handler))

    override fun onGlobalDrag(handler: DragHandler) {
        onGlobalDrag = handler
    }

    override fun onGlobalDrag(handler: DragHandlerDsl) = onGlobalDrag(DragHandlerScopeImpl(handler))

    override fun onClose(handler: CloseHandler) {
        onClose = handler
    }

    override fun onClose(handler: CloseHandlerDsl) = onClose(CloseHandlerScopeImpl(handler))
    // endregion

    override fun navigateToParentOnClose(enabled: Boolean) {
        shouldNavigateToParentOnClose = enabled
    }

    override fun update() {
        if (updating) return
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

    override fun updateItem(item: UpdatableGuiItemImpl) {
        if (updating) return

        val slot = updateItem0(item) ?: return
        if (item.visible) {
            backingInventory.setItem(slot, item.itemStack)
        } else {
            backingInventory.setItem(slot, ItemStack.empty())
        }

        updating = false
    }

    protected fun addInventory(inventory: Inventory, gui: AbstractGui) {
        GUI_INVENTORIES[inventory] = gui
    }

    fun callOnTopClick(event: InventoryClickEvent) {
        callCallback(onTopClick, event)
    }

    fun callOnBottomClick(event: InventoryClickEvent) {
        callCallback(onBottomClick, event)
    }

    fun callOnGlobalClick(event: InventoryClickEvent) {
        callCallback(onGlobalClick, event)
    }

    fun callOnOutsideClick(event: InventoryClickEvent) {
        callCallback(onOutsideClick, event)
    }

    fun callOnTopDrag(event: InventoryDragEvent) {
        callCallback(onTopDrag, event)
    }

    fun callOnBottomDrag(event: InventoryDragEvent) {
        callCallback(onBottomDrag, event)
    }

    fun callOnGlobalDrag(event: InventoryDragEvent) {
        callCallback(onGlobalDrag, event)
    }

    fun callOnClose(event: InventoryCloseEvent) {
        callCallback(onClose, event)
    }

    private fun <E : InventoryEvent> callCallback(
        callback: GuiHandler<E>?,
        event: E,
    ) {
        if (callback == null) return

        try {
            callback.handle(event)
        } catch (exception: Throwable) {
            log.atSevere()
                .withCause(exception)
                .log(buildString {
                    append("Exception while handling $callback")

                    if (event is InventoryClickEvent) {
                        append(", slot=${event.slot}")
                    }
                })
        }
    }

    override fun navigateToParent(player: Player): Boolean {
        val parent = parent
        if (parent == null) return false

        parent.show(player)
        parent.update()
        return true
    }

    override fun walkParents(): Sequence<AbstractGui> = generateSequence(this) { it.parent }

    companion object {
        private val log = logger()
        private val GUI_INVENTORIES = WeakHashMap<Inventory, AbstractGui>()

        fun getGui(inventory: Inventory) = GUI_INVENTORIES[inventory]
    }
}