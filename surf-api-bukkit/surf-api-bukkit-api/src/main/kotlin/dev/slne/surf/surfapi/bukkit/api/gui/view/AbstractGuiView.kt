package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.context.abstract.*
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.bukkit.api.surfBukkitApi
import dev.slne.surf.surfapi.core.api.util.InternalSurfApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@InternalSurfApi
open class AbstractGuiView : GuiView() {
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
        ViewManager.registerView(inventory, this)

        // Render all components, respecting priority
        // For each slot, render only the highest priority component
        val allSlots = (0 until inventory.size).map { Slot.of(it) }
        allSlots.forEach { slot ->
            val componentsAtSlot = findComponentsBySlot(slot)
            if (componentsAtSlot.isNotEmpty()) {
                // Get highest priority component
                val component = componentsAtSlot.first()
                val context = createViewContext(player)

                // Check if it's a container component
                val slotsToRender = component.renderSlots(context)
                if (slotsToRender.isNotEmpty()) {
                    // Only render this slot if it's in the container's output
                    slotsToRender[slot]?.let { guiItem ->
                        if (slot.index < inventory.size) {
                            inventory.setItem(slot.index, guiItem.toItemStack())
                        }
                    }
                } else {
                    // Regular component - only render at its start slot
                    if (slot == component.area.first()) {
                        val guiItem = component.render(context)
                        if (guiItem != null && slot.index < inventory.size) {
                            inventory.setItem(slot.index, guiItem.toItemStack())
                        }
                    }
                }
            }
        }

        // Open inventory
        player.openInventory(inventory)

        // Start update task if configured (Folia-compatible)
        updateInterval?.let { interval ->
            val job = surfBukkitApi.launch(surfBukkitApi.entityDispatcher(player)) {
                while (true) {
                    delay(interval)
                    update()
                }
            }
            updateJobs[player.uniqueId] = job
        }

        // Start component update tasks (Folia-compatible)
        val playerComponentJobs = mutableListOf<Job>()
        components.forEach { component ->
            component.updateInterval?.let { interval ->
                val job = surfBukkitApi.launch(surfBukkitApi.entityDispatcher(player)) {
                    while (true) {
                        delay(interval)
                        val lifecycleContext =
                            createLifecycleContext(player, LifecycleEventType.UPDATE)
                        component.onUpdate(lifecycleContext)
                        refreshComponentSlots(player, component)
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
            ViewManager.unregisterView(inventory)
        }

        super.close(player)
    }

    /**
     * Refresh the inventory for a player.
     */
    internal fun refreshInventory(player: Player) {
        val inventory = inventories[player.uniqueId] ?: return

        // Clear inventory first
        inventory.clear()

        // Render all slots, respecting priority
        val allSlots = (0 until inventory.size).map { Slot.of(it) }
        allSlots.forEach { slot ->
            val componentsAtSlot = findComponentsBySlot(slot)
            if (componentsAtSlot.isNotEmpty()) {
                // Get highest priority component
                val component = componentsAtSlot.first()
                val context = createViewContext(player)

                // Check if it's a container component
                val slotsToRender = component.renderSlots(context)
                if (slotsToRender.isNotEmpty()) {
                    // Only render this slot if it's in the container's output
                    slotsToRender[slot]?.let { guiItem ->
                        if (slot.index < inventory.size) {
                            inventory.setItem(slot.index, guiItem.toItemStack())
                        }
                    }
                } else {
                    // Regular component - only render at its start slot
                    if (slot == component.area.first()) {
                        val guiItem = component.render(context)
                        if (guiItem != null && slot.index < inventory.size) {
                            inventory.setItem(slot.index, guiItem.toItemStack())
                        }
                    }
                }
            }
        }
    }

    /**
     * Refresh all slots occupied by a specific component and its children.
     */
    internal fun refreshComponentSlots(player: Player, component: Component) {
        val inventory = inventories[player.uniqueId] ?: return
        val context = createViewContext(player)

        // Collect all slots from this component and all its children recursively
        fun collectAllSlots(comp: Component): Set<Slot> {
            val slots = mutableSetOf<Slot>()
            slots.addAll(comp.area.slots())
            comp.children.forEach { child ->
                slots.addAll(collectAllSlots(child))
            }
            return slots
        }

        // Get all slots this component and its children could occupy
        collectAllSlots(component).forEach { slot ->
            if (slot.index >= inventory.size) return@forEach
            
            // Find all components at this slot, sorted by priority
            val componentsAtSlot = findComponentsBySlot(slot)
            
            // Try each component from highest to lowest priority until one renders something
            var rendered = false
            for (comp in componentsAtSlot) {
                val renderedItems = comp.renderSlots(context)
                val item = renderedItems[slot]
                
                if (item != null) {
                    // This component renders something at this slot
                    inventory.setItem(slot.index, item.toItemStack())
                    rendered = true
                    break
                }
            }
            
            // If no component rendered at this slot, clear it
            // This ensures slots are properly cleared when components become hidden
            if (!rendered) {
                inventory.setItem(slot.index, null)
            }
        }
    }
    
    /**
     * Implementation of refreshComponentSlotsInternal for GuiView.
     */
    override fun refreshComponentSlotsInternal(player: Player, component: Component) {
        refreshComponentSlots(player, component)
    }

    /**
     * Handle click event.
     */
    fun handleClick(player: Player, event: InventoryClickEvent) {
        if (config.cancelOnClick) {
            event.isCancelled = true
        }

        val slot = Slot.of(event.slot)

        // Find all components at this slot, sorted by priority (highest first)
        val componentsAtSlot = findComponentsBySlot(slot)

        // Only the highest priority component handles the click, and only if not disabled
        val component = componentsAtSlot.firstOrNull()

        if (component != null && !component.disabled) {
            val clickContext = AbstractClickContext(this, player, event, component)
            component.onClick(clickContext)
        }
    }

    override fun createViewContext(player: Player): ViewContext {
        return AbstractViewContext(this, player)
    }

    override fun createRenderContext(player: Player): RenderContext {
        return AbstractRenderContext(this, player, this)
    }

    override fun createLifecycleContext(
        player: Player,
        eventType: LifecycleEventType
    ): LifecycleContext {
        return AbstractLifecycleContext(this, player, eventType)
    }

    override fun createResumeContext(player: Player, origin: GuiView?): ResumeContext {
        return AbstractResumeContext(this, player, origin)
    }
}