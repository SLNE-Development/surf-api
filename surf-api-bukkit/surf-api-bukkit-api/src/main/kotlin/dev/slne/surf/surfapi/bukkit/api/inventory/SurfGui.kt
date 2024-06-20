package dev.slne.surf.surfapi.bukkit.api.inventory

import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui
import com.github.stefvanschie.inventoryframework.pane.StaticPane
import com.github.stefvanschie.inventoryframework.pane.util.Slot
import dev.slne.surf.surfapi.bukkit.api.inventory.dsl.PaneMarker
import dev.slne.surf.surfapi.bukkit.api.inventory.item.SurfGuiItem
import org.bukkit.entity.HumanEntity
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

interface SurfGui {
    val parent: SurfGui?
    val gui: NamedGui

    fun HumanEntity.backToParent() {
        server.scheduler.runTask(JavaPlugin.getProvidingPlugin(SurfGui::class.java), Runnable {
            if (parent != null) {
                val gui = parent!!.gui
                gui.show(this)
                gui.update()
            } else {
                closeInventory(InventoryCloseEvent.Reason.PLUGIN)
            }
        })
    }

    fun walkParents(): List<SurfGui> = generateSequence(this) { it.parent }.toList()

    fun StaticPane.item(
        slot: Slot,
        item: ItemStack? = null,
        init: (@PaneMarker SurfGuiItem).() -> Unit
    ) {
        val guiItem = SurfGuiItem(item)
        guiItem.init()

        if (!guiItem.condition()) {
            return
        }

        if (this@SurfGui is SinglePlayerGui) {
            if (guiItem.itemPermission?.let { player.hasPermission(it) } == false) {
                return
            }
        }

        addItem(guiItem, slot)
    }
}