package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.props.PropContext
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * Context representing a snapshot of the current GUI state and props.
 * This is passed to all user action handlers and provides access to view, props, and player.
 */
interface ViewContext {
    /**
     * The view this context belongs to.
     */
    val view: GuiView
    
    /**
     * The player interacting with the GUI.
     */
    val player: Player
    
    /**
     * The unique ID of the viewer.
     */
    val viewerId: UUID
        get() = player.uniqueId
    
    /**
     * Props context for accessing prop values.
     */
    val propContext: PropContext
        get() = PropContext(viewerId, player)
    
    /**
     * Get a prop value.
     */
    fun <T> getProp(prop: Prop<T>): T = prop.get(propContext)
    
    /**
     * Navigate to another view.
     */
    fun navigateTo(view: GuiView, passProps: Boolean = false)
    
    /**
     * Navigate back to parent view.
     */
    fun navigateBack()
    
    /**
     * Close the GUI.
     */
    fun close()
    
    /**
     * Update the current view.
     */
    fun update()
}

/**
 * Context for click events.
 */
interface ClickContext : ViewContext {
    /**
     * The click event.
     */
    val event: InventoryClickEvent
    
    /**
     * The clicked item.
     */
    val item: ItemStack?
        get() = event.currentItem
    
    /**
     * The slot that was clicked.
     */
    val slot: Int
        get() = event.slot
    
    /**
     * The component that was clicked, if any.
     */
    val component: Component?
}

/**
 * Context for render operations.
 */
interface RenderContext : ViewContext {
    /**
     * Render a component at the specified slot.
     */
    fun renderComponent(slot: Int, component: Component)
    
    /**
     * Clear a slot.
     */
    fun clearSlot(slot: Int)
    
    /**
     * Set an item at a slot without a component.
     */
    fun setItem(slot: Int, item: ItemStack)
}

/**
 * Context for lifecycle events.
 */
interface LifecycleContext : ViewContext {
    /**
     * The type of lifecycle event.
     */
    val eventType: LifecycleEventType
}

/**
 * Types of lifecycle events.
 */
enum class LifecycleEventType {
    MOUNT,
    UNMOUNT,
    UPDATE,
    FIRST_RENDER,
    OPEN,
    CLOSE,
    RESUME
}

/**
 * Context for resume events (navigation back).
 */
interface ResumeContext : LifecycleContext {
    /**
     * The view we're navigating from (origin).
     */
    val origin: GuiView?
    
    /**
     * The view we're navigating to (target, this view).
     */
    val target: GuiView
        get() = view
}
