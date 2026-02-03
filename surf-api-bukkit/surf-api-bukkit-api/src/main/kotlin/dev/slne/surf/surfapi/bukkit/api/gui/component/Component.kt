package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import kotlin.time.Duration

/**
 * Base interface for all GUI components.
 * Components follow React-like lifecycle principles.
 */
abstract class Component {
    /**
     * The start slot (top-left corner) of this component's area.
     */
    abstract val startSlot: Slot
    
    /**
     * The end slot (bottom-right corner) of this component's area.
     */
    abstract val endSlot: Slot
    
    /**
     * Priority for handling clicks and rendering when components overlap.
     * Higher priority components are rendered on top and handle clicks first.
     * Default is NORMAL.
     */
    open val priority: ComponentPriority = ComponentPriority.NORMAL
    
    /**
     * Width of the component in columns (derived from start and end slots).
     */
    val width: Int
        get() = endSlot.column - startSlot.column + 1
    
    /**
     * Height of the component in rows (derived from start and end slots).
     */
    val height: Int
        get() = endSlot.row - startSlot.row + 1
    
    /**
     * Check if a slot is within this component's area.
     */
    fun contains(slot: Slot): Boolean {
        return slot.column >= startSlot.column &&
                slot.column <= endSlot.column &&
                slot.row >= startSlot.row &&
                slot.row <= endSlot.row
    }
    
    /**
     * The parent component, if any.
     */
    var parent: Component? = null
        internal set
    
    /**
     * The children of this component.
     */
    private val _children = mutableListOf<Component>()
    val children: List<Component> get() = _children.toList()
    
    /**
     * The view this component belongs to.
     */
    lateinit var view: GuiView
        internal set
    
    /**
     * Update interval for this component, if any.
     */
    open val updateInterval: Duration? = null
    
    /**
     * Props accessible by this component.
     * Children can access parent props.
     */
    protected open val props: Map<String, Prop<*>> = emptyMap()
    
    /**
     * Ref attached to this component, if any.
     */
    internal var attachedRef: Ref<Component>? = null
    
    /**
     * Called when the component is updated.
     */
    open fun onUpdate(context: LifecycleContext) {}
    
    /**
     * Called when the component is clicked.
     */
    open fun onClick(context: ClickContext) {}
    
    /**
     * Renders the component to a GuiItem.
     * For container components, return null.
     */
    open fun render(context: ViewContext): GuiItem? = null
    
    /**
     * For container components, render multiple items at specific slots.
     * Returns a map of Slot to GuiItem.
     */
    open fun renderSlots(context: ViewContext): Map<Slot, GuiItem> = emptyMap()
    
    /**
     * Add a child component.
     */
    fun addChild(child: Component) {
        child.parent = this
        child.view = view
        _children.add(child)
    }
    
    /**
     * Remove a child component.
     */
    fun removeChild(child: Component) {
        _children.remove(child)
        child.parent = null
    }
    
    /**
     * Get all props including parent props.
     */
    fun getAllProps(): Map<String, Prop<*>> {
        val allProps = mutableMapOf<String, Prop<*>>()
        parent?.getAllProps()?.let { allProps.putAll(it) }
        allProps.putAll(props)
        return allProps
    }
    
    /**
     * Trigger an update of this component.
     */
    fun update() {
        view.updateComponent(this)
    }
    
    /**
     * Trigger an update of this component and all children.
     */
    fun updateWithChildren() {
        update()
        children.forEach { it.updateWithChildren() }
    }
}
