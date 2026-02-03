package dev.slne.surf.surfapi.bukkit.server.gui.view

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.server.gui.context.*
import dev.slne.surf.surfapi.bukkit.server.plugin
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Bukkit-specific implementation of GuiView.
 * Folia-compatible using entity dispatchers.
 */
abstract class BukkitGuiView : GuiView() {
    private val inventories = ConcurrentHashMap<UUID, Inventory>()
    private val updateJobs = ConcurrentHashMap<UUID, Job>()
    private val componentJobs = ConcurrentHashMap<UUID, MutableList<Job>>()

    override fun open(player: Player) {
        super.open(player)

        // Create inventory based on type
        val inventory = when (config.type) {
            InventoryType.CHEST -> {
                Bukkit.createInventory(null, config.size, config.title)
            }

            else -> {
                Bukkit.createInventory(null, config.type, config.title)
            }
        }

        inventories[player.uniqueId] = inventory

        // Register with listener
        GuiViewListener.registerView(inventory, this)

        // Render components
        components.forEach { (slot, component) ->
            val context = createViewContext(player)

            // Handle container components (render multiple slots)
            val slotsToRender = component.renderSlots(context)
            if (slotsToRender.isNotEmpty()) {
                slotsToRender.forEach { (slotObj, guiItem) ->
                    if (slotObj.index < inventory.size) {
                        inventory.setItem(slotObj.index, guiItem.toItemStack())
                    }
                }
            } else {
                // Regular component (single item)
                val guiItem = component.render(context)
                if (guiItem != null && slot.index < inventory.size) {
                    inventory.setItem(slot.index, guiItem.toItemStack())
                }
            }
        }

        // Open inventory
        player.openInventory(inventory)

        // Start update task if configured (Folia-compatible)
        updateInterval?.let { interval ->
            val job = plugin.launch(plugin.entityDispatcher(player)) {
                while (true) {
                    delay(interval)
                    update()
                }
            }
            updateJobs[player.uniqueId] = job
        }

        // Start component update tasks (Folia-compatible)
        val playerComponentJobs = mutableListOf<Job>()
        components.values.forEach { component ->
            component.updateInterval?.let { interval ->
                val job = plugin.launch(plugin.entityDispatcher(player)) {
                    while (true) {
                        delay(interval)
                        val lifecycleContext =
                            createLifecycleContext(player, LifecycleEventType.UPDATE)
                        component.onUpdate(lifecycleContext)
                        refreshComponentSlot(player, component)
                    }
                }
                playerComponentJobs.add(job)
            }
        }
        if (playerComponentJobs.isNotEmpty()) {
            componentJobs[player.uniqueId] = playerComponentJobs
        }
    }

    override fun close(player: Player) {
        // Cancel all update jobs
        updateJobs.remove(player.uniqueId)?.cancel()
        componentJobs.remove(player.uniqueId)?.forEach { it.cancel() }

        // Unregister inventory
        inventories.remove(player.uniqueId)?.let { inventory ->
            GuiViewListener.unregisterView(inventory)
        }

        super.close(player)
    }

    /**
     * Refresh the inventory for a player.
     */
    internal fun refreshInventory(player: Player) {
        val inventory = inventories[player.uniqueId] ?: return

        components.forEach { (slot, component) ->
            val context = createViewContext(player)

            // Handle container components
            val slotsToRender = component.renderSlots(context)
            if (slotsToRender.isNotEmpty()) {
                slotsToRender.forEach { (slotObj, guiItem) ->
                    if (slotObj.index < inventory.size) {
                        inventory.setItem(slotObj.index, guiItem.toItemStack())
                    }
                }
            } else {
                // Regular component
                val guiItem = component.render(context)
                if (guiItem != null && slot.index < inventory.size) {
                    inventory.setItem(slot.index, guiItem.toItemStack())
                }
            }
        }
    }

    /**
     * Refresh a specific component slot.
     */
    private fun refreshComponentSlot(player: Player, component: Component) {
        val inventory = inventories[player.uniqueId] ?: return
        val slot = components.entries.find { it.value == component }?.key ?: return

        val context = createViewContext(player)

        // Handle container components
        val slotsToRender = component.renderSlots(context)
        if (slotsToRender.isNotEmpty()) {
            slotsToRender.forEach { (slotObj, guiItem) ->
                if (slotObj.index < inventory.size) {
                    inventory.setItem(slotObj.index, guiItem.toItemStack())
                }
            }
        } else {
            // Regular component
            val guiItem = component.render(context)
            if (guiItem != null && slot.index < inventory.size) {
                inventory.setItem(slot.index, guiItem.toItemStack())
            }
        }
    }

    /**
     * Handle click event.
     */
    internal fun handleClick(player: Player, event: InventoryClickEvent) {
        if (config.cancelOnClick) {
            event.isCancelled = true
        }

        val slot = Slot.of(event.slot)
        val component = components[slot]

        val clickContext = BukkitClickContext(this, player, event, component)
        component?.onClick(clickContext)
    }

    override fun createViewContext(player: Player): ViewContext {
        return BukkitViewContext(this, player)
    }

    override fun createRenderContext(player: Player): RenderContext {
        return BukkitRenderContext(this, player, this)
    }

    override fun createLifecycleContext(
        player: Player,
        eventType: LifecycleEventType
    ): LifecycleContext {
        return BukkitLifecycleContext(this, player, eventType)
    }

    override fun createResumeContext(player: Player, origin: GuiView?): ResumeContext {
        return BukkitResumeContext(this, player, origin)
    }
}

/**
 * Global event listener for all GUI views.
 */
object GuiViewListener : Listener {
    private val viewsByInventory = ConcurrentHashMap<Inventory, BukkitGuiView>()

    fun registerView(inventory: Inventory, view: BukkitGuiView) {
        viewsByInventory[inventory] = view
    }

    fun unregisterView(inventory: Inventory) {
        viewsByInventory.remove(inventory)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val inventory = event.clickedInventory ?: return
        val view = viewsByInventory[inventory] ?: return
        val player = event.whoClicked as? Player ?: return

        view.handleClick(player, event)
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val inventory = event.inventory
        val view = viewsByInventory[inventory] ?: return
        val player = event.player as? Player ?: return

        // Close the view when inventory is closed
        view.close(player)
    }
}
