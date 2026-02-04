package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.context.abstract.*
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

@InternalSurfApi
open class AbstractGuiView : GuiView() {
    override fun open(player: Player) {
        super.open(player)

        ViewManager.setActiveView(player, this)
        renderAllSlots(player)

        // Open inventory
        player.openInventory(inventory)
    }

    override fun close(player: Player) {
        ViewManager.removeActiveView(player)
        super.close(player)
    }

    private fun renderAllSlots(player: Player) {
        val allSlots = (0 until inventory.size).map { Slot.of(it) }

        allSlots.forEach { slot ->
            renderSlot(player, slot)
        }
    }

    private fun renderSlot(
        player: Player,
        slot: Slot
    ) {
        if (slot.index >= inventory.size) return

        val resolved = resolveSlot(player, slot)
        resolved?.component?.renderFirstRenderPerPlayer(player)

        inventory.setItem(
            slot.index,
            resolved?.guiItem?.toItemStack()
        )
    }

    /**
     * Refresh the inventory for a player.
     */
    internal fun refreshInventory(player: Player) {
        // Clear inventory first
        inventory.clear()

        // Re-render all slots
        renderAllSlots(player)
    }

    /**
     * Refresh all slots occupied by a specific component and its children.
     */
    internal fun refreshComponentSlots(player: Player, component: Component) {
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
            renderSlot(player, slot)
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

    override fun createInitializeContext(): InitializeContext {
        return AbstractInitializeContext(config, this)
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
        return "AbstractGuiView() ${super.toString()}"
    }
}