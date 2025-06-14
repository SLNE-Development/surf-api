package dev.slne.surf.surfapi.bukkit.server.impl.inventory.listener

import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticks
import dev.slne.surf.surfapi.bukkit.server.impl.inventory.gui.AbstractGui
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.mutableObjectSetOf
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.*
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.inventory.Inventory
import java.util.*

object GuiListener : Listener {
    private val log = logger()
    private val activeGuiInstances = mutableObjectSetOf<AbstractGui>()

    @EventHandler(ignoreCancelled = true)
    fun InventoryClickEvent.onInventoryClick() {
        val gui = getGui(inventory) ?: return

        val inventory = view.getInventory(rawSlot) ?: run {
            gui.callOnOutsideClick(this)
            return
        }

        gui.callOnGlobalClick(this)

        if (inventory == view.topInventory) {
            gui.callOnTopClick(this)
        } else {
            gui.callOnBottomClick(this)
        }

        gui.click(this)

        if (isCancelled) {
            plugin.launch {
                delay(1.ticks)

                whoClicked.inventory.setItemInOffHand(whoClicked.inventory.itemInOffHand)
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun EntityPickupItemEvent.onEntityPickupItem() {
        val player = entity as? Player ?: return

        val gui = getGui(player.openInventory.topInventory)
        if (gui == null || !gui.isPlayerInventoryUsed()) {
            return
        }

        val leftOver = gui.cache.add(player, item.itemStack)

        if (leftOver == 0) {
            item.remove()
        } else {
            val itemStack = item.itemStack
            itemStack.amount = leftOver
            item.itemStack = itemStack
        }

        isCancelled = true
    }

    @EventHandler
    fun InventoryDragEvent.onInventoryDrag() {
        val gui = getGui(inventory) ?: return

        if (rawSlots.size > 1) {
            var top = false
            var bottom = false

            for (slot in rawSlots) {
                val inventory = view.getInventory(slot) ?: continue

                if (inventory == view.topInventory) {
                    top = true
                } else if (inventory == view.bottomInventory) {
                    bottom = true
                }

                if (top && bottom) break
            }

            gui.callOnGlobalDrag(this)

            if (top) {
                gui.callOnTopDrag(this)
            } else if (bottom) {
                gui.callOnBottomDrag(this)
            }
        } else {
            val index = rawSlots.toTypedArray().first()
            val slotType = view.getSlotType(index)
            val even = type == DragType.EVEN
            val clickType = if (even) ClickType.LEFT else ClickType.RIGHT
            val action = if (even) InventoryAction.PLACE_SOME else InventoryAction.PLACE_ONE

            val previousCursor = view.cursor
            view.setCursor(oldCursor)

            val clickEvent = InventoryClickEvent(
                view,
                slotType,
                index,
                clickType,
                action
            )

            clickEvent.onInventoryClick()

            if (Objects.equals(view.cursor, oldCursor)) {
                view.setCursor(previousCursor)
            }

            isCancelled = clickEvent.isCancelled
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun InventoryCloseEvent.onInventoryClose() {
        val player = player as? Player ?: return
        val gui = getGui(inventory) ?: return

        val playerInventory = player.inventory
        playerInventory.setItemInOffHand(playerInventory.itemInOffHand)

        if (!gui.updating) {
            gui.callOnClose(this)
            gui.cache.restoreAndForget(player)

            if (gui.viewers.size == 1) {
                activeGuiInstances.remove(gui)
            }

            if (gui.shouldNavigateToParentOnClose) {
                plugin.launch {
                    delay(1.ticks)

                    gui.navigateToParent(player)
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun InventoryOpenEvent.onInventoryOpen() {
        val gui = getGui(inventory) ?: return

        activeGuiInstances.add(gui)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onPluginDisable(event: PluginDisableEvent) {
        if (plugin != event.plugin) return

        var counter = 0
        val maxCount = 10

        while (!activeGuiInstances.isEmpty() && counter++ < maxCount) {
            for (gui in activeGuiInstances.freeze()) {
                for (viewer in gui.viewers.freeze()) {
                    viewer.closeInventory()
                }
            }
        }

        if (counter == maxCount) {
            log.atWarning().log(
                "Unable to close GUIs on plugin disable. GUIs keep getting opened (tried: $maxCount times). " +
                        "This may lead to memory leaks. Please check your code for any issues."
            )
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun TradeSelectEvent.onTradeSelect() {
        val gui = getGui(inventory) ?: return

        TODO("Implement TradeSelectEvent handling in GUI")
    }

    private fun getGui(inventory: Inventory): AbstractGui? {
        val gui = AbstractGui.getGui(inventory)

        if (gui != null) return gui

        val holder = inventory.holder

        if (holder is AbstractGui) {
            return holder
        }

        return null
    }


}