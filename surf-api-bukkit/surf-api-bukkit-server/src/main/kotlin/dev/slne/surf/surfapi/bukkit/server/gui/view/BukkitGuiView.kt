package dev.slne.surf.surfapi.bukkit.server.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.bukkit.server.gui.context.*
import dev.slne.surf.surfapi.bukkit.server.plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Bukkit-specific implementation of GuiView.
 */
abstract class BukkitGuiView : GuiView() {
    
    private val inventories = ConcurrentHashMap<UUID, Inventory>()
    private val updateTasks = ConcurrentHashMap<UUID, Int>()
    
    override fun open(player: Player) {
        super.open(player)
        
        // Create inventory
        val inventory = Bukkit.createInventory(null, config.size, config.title)
        inventories[player.uniqueId] = inventory
        
        // Render components
        components.forEach { (slot, component) ->
            val context = createViewContext(player)
            val item = component.render(context)
            inventory.setItem(slot, item)
        }
        
        // Open inventory
        player.openInventory(inventory)
        
        // Start update task if configured
        updateInterval?.let { interval ->
            val taskId = Bukkit.getScheduler().runTaskTimer(
                plugin,
                Runnable { update() },
                interval.inWholeSeconds * 20L,
                interval.inWholeSeconds * 20L
            ).taskId
            updateTasks[player.uniqueId] = taskId
        }
        
        // Start component update tasks
        components.values.forEach { component ->
            component.updateInterval?.let { interval ->
                Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    Runnable { 
                        val lifecycleContext = createLifecycleContext(player, LifecycleEventType.UPDATE)
                        component.onUpdate(lifecycleContext)
                        refreshComponentSlot(player, component)
                    },
                    interval.inWholeSeconds * 20L,
                    interval.inWholeSeconds * 20L
                )
            }
        }
    }
    
    override fun close(player: Player) {
        // Cancel update tasks
        updateTasks.remove(player.uniqueId)?.let { taskId ->
            Bukkit.getScheduler().cancelTask(taskId)
        }
        
        // Clean up
        inventories.remove(player.uniqueId)
        
        super.close(player)
    }
    
    /**
     * Refresh the inventory for a player.
     */
    internal fun refreshInventory(player: Player) {
        val inventory = inventories[player.uniqueId] ?: return
        
        components.forEach { (slot, component) ->
            val context = createViewContext(player)
            val item = component.render(context)
            inventory.setItem(slot, item)
        }
    }
    
    /**
     * Refresh a specific component slot.
     */
    private fun refreshComponentSlot(player: Player, component: Component) {
        val inventory = inventories[player.uniqueId] ?: return
        val slot = components.entries.find { it.value == component }?.key ?: return
        
        val context = createViewContext(player)
        val item = component.render(context)
        inventory.setItem(slot, item)
    }
    
    /**
     * Handle click event.
     */
    internal fun handleClick(player: Player, event: InventoryClickEvent) {
        if (config.cancelOnClick) {
            event.isCancelled = true
        }
        
        val slot = event.slot
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
    
    override fun createLifecycleContext(player: Player, eventType: LifecycleEventType): LifecycleContext {
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
        
        // Don't close if navigating or resuming
        // This will be handled by the navigation logic
    }
}
