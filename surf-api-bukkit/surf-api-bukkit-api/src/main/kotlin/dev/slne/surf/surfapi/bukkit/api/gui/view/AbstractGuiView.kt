package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.context.abstract.*
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
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
    }

    override fun close(player: Player) {
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

        val resolved = resolveSlot(player, slot)

        if (firstRender && resolved != null) {
            resolved.component.onFirstRender(
                createLifecycleContext(player, LifecycleEventType.FIRST_RENDER)
            )
        }

        inventory.setItem(
            slot.index,
            resolved?.guiItem?.toItemStack()
        )
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

    private fun resolveSlot(
        player: Player,
        slot: Slot
    ): ResolvedSlot? {
        val componentsAtSlot = findComponentsBySlot(slot)
        if (componentsAtSlot.isEmpty()) return null

        val highestPriority = componentsAtSlot.first().priority.value
        val candidates = componentsAtSlot
            .takeWhile { it.priority.value == highestPriority }

        val viewContext = createViewContext(player)

        for (component in candidates) {
            component.initComponent(createLifecycleContext(player, LifecycleEventType.INIT_COMPONENT))
            if (component.hidden) continue

            val slots = component.renderSlots(viewContext)

            if (slots.isNotEmpty()) {
                slots[slot]?.let { guiItem ->
                    return ResolvedSlot(component, guiItem)
                }
            } else if (slot == component.area.first()) {
                component.render(viewContext)?.let { guiItem ->
                    return ResolvedSlot(component, guiItem)
                }
            }
        }

        return null
    }

    /**
     * Handle click event.
     */
    fun handleClick(player: Player, event: InventoryClickEvent) {
        if (config.cancelOnClick) {
            event.isCancelled = true
        }

        val slot = Slot.of(event.slot)
        val resolved = resolveSlot(player, slot) ?: return

        if (resolved.component.disabled) return

        resolved.component.onClick(createClickContext(player, event, resolved.component))
    }

    override fun createClickContext(
        player: Player,
        event: InventoryClickEvent,
        component: Component
    ): ClickContext {
        return AbstractClickContext(this, player, event, component)
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
        return "AbstractGuiView(inventories=$inventories)"
    }
}