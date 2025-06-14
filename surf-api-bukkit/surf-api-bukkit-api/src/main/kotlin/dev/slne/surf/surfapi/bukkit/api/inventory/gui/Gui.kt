package dev.slne.surf.surfapi.bukkit.api.inventory.gui

import dev.slne.surf.surfapi.bukkit.api.inventory.InventoryBridge
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.GuiDsl
import dev.slne.surf.surfapi.bukkit.api.inventory.gui.handlers.*
import dev.slne.surf.surfapi.bukkit.api.inventory.item.UpdatableGuiItem
import it.unimi.dsi.fastutil.objects.ObjectSet
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.jetbrains.annotations.Unmodifiable

@GuiDsl
interface Gui : Cloneable {
    val backingInventory: Inventory
    val viewers: @Unmodifiable ObjectSet<Player>
    val parent: Gui?

    // region Handlers
    fun onTopClick(handler: ClickHandler)

    fun onTopClick(handler: ClickHandlerDsl)
    fun onBottomClick(handler: ClickHandler)

    fun onBottomClick(handler: ClickHandlerDsl)
    fun onGlobalClick(handler: ClickHandler)

    fun onGlobalClick(handler: ClickHandlerDsl)
    fun onOutsideClick(handler: ClickHandler)

    fun onOutsideClick(handler: ClickHandlerDsl)
    fun onTopDrag(handler: DragHandler)

    fun onTopDrag(handler: DragHandlerDsl)
    fun onBottomDrag(handler: DragHandler)

    fun onBottomDrag(handler: DragHandlerDsl)
    fun onGlobalDrag(handler: DragHandler)

    fun onGlobalDrag(handler: DragHandlerDsl)
    fun onClose(handler: CloseHandler)
    fun onClose(handler: CloseHandlerDsl)
    // endregion

    fun navigateToParentOnClose(enabled: Boolean = true)
    fun navigateToParent(player: Player): Boolean
    fun walkParents(): Sequence<Gui>

    fun show(player: Player)
    fun update()
    fun updateItem(item: UpdatableGuiItem)

    public override fun clone(): Gui

    companion object {
        fun getGui(inventory: Inventory): Gui? =
            InventoryBridge.instance.getGuiByInventory(inventory)
    }
}