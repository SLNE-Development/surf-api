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

        renderAllSlots(player, inventory, true)

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
        // TODO: Fix, this currently can cause issues if multiple inventories are open
        inventories.remove(player.uniqueId)?.let { inventory ->
            ViewManager.unregisterView(inventory)
        }

        super.close(player)
    }

    private fun renderAllSlots(
        player: Player,
        inventory: Inventory,
        firstRender: Boolean
    ) {
        val allSlots = (0 until inventory.size).map { Slot.of(it) }

        allSlots.forEach { slot ->
            renderSlot(player, inventory, slot, firstRender)
        }
    }

    private fun renderSlot(
        player: Player,
        inventory: Inventory,
        slot: Slot,
        firstRender: Boolean = false
    ) {
        if (slot.index >= inventory.size) return

        val componentsAtSlot = findComponentsBySlot(slot)
        var rendered = false

        if (componentsAtSlot.isNotEmpty()) {
            // Get highest priority component
            val component = componentsAtSlot.first()
            val context = createViewContext(player)

            if (firstRender) {
                component.onFirstRender(
                    createLifecycleContext(
                        player,
                        LifecycleEventType.FIRST_RENDER
                    )
                )
            }

            // Check if it's a container component
            val slotsToRender = component.renderSlots(context)

            if (slotsToRender.isNotEmpty()) {
                // Only render this slot if it's in the container's output
                slotsToRender[slot]?.let { guiItem ->
                    if (slot.index < inventory.size) {
                        inventory.setItem(slot.index, guiItem.toItemStack())
                        rendered = true
                    }
                }
            } else {
                // Regular component - only render at its start slot
                if (slot == component.area.first()) {
                    val guiItem = component.render(context)
                    if (guiItem != null && slot.index < inventory.size) {
                        inventory.setItem(slot.index, guiItem.toItemStack())
                        rendered = true
                    }
                }
            }
        }

        if (!rendered) {
            inventory.setItem(slot.index, null)
        }
    }

    /**
     * Refresh the inventory for a player.
     */
    internal fun refreshInventory(player: Player) {
        val inventory = inventories[player.uniqueId] ?: return

        // Clear inventory first
        inventory.clear()

        // Re-render all slots
        renderAllSlots(player, inventory, false)
    }

    /**
     * Refresh all slots occupied by a specific component and its children.
     */
    internal fun refreshComponentSlots(player: Player, component: Component) {
        val inventory = inventories[player.uniqueId] ?: return

        // Collect all slots from this component and all its children recursively
        // Only refresh this component's own slots, not children's
        // Children will refresh their own slots when updateChildrenRecursively calls them
        fun collectAllSlots(comp: Component): Set<Slot> {
            val slots = mutableSetOf(*comp.area.slots().toTypedArray())

            comp.children.forEach { child ->
                slots.addAll(collectAllSlots(child))
            }

            return slots
        }

        collectAllSlots(component).forEach { slot ->
            renderSlot(player, inventory, slot, false)
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

    override fun toString(): String {
        return "AbstractGuiView(inventories=$inventories, updateJobs=$updateJobs, componentJobs=$componentJobs)"
    }
}