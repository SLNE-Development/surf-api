package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.area.ComponentArea
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import org.bukkit.entity.Player
import kotlin.time.Duration

/**
 * Base interface for all GUI components.
 * Components follow React-like lifecycle principles.
 */
abstract class Component {
    /**
     * The area this component occupies in the GUI.
     * Can be any shape (cuboid, circular, custom).
     */
    abstract val area: ComponentArea

    /**
     * Priority for handling clicks and rendering when components overlap.
     * Higher priority components are rendered on top and handle clicks first.
     */
    abstract val priority: ComponentPriority

    /**
     * Width of the component's bounding box.
     * Delegates to the area's width.
     */
    val width: Int
        get() = area.width

    /**
     * Height of the component's bounding box.
     * Delegates to the area's height.
     */
    val height: Int
        get() = area.height

    /**
     * Check if a slot is within this component's area.
     * Delegates to the area's contains method.
     */
    fun contains(slot: Slot): Boolean = area.contains(slot)

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
    var view: GuiView? = null
        internal set(value) {
            field = value
            // Propagate view to existing children
            _children.forEach { child ->
                child.view = value
            }
        }

    /**
     * Update interval for this component, if any.
     */
    open val updateInterval: Duration? = null

    /**
     * Whether this component is hidden (not rendered at all).
     */
    var hidden: Boolean = false

    /**
     * Whether this component is disabled (rendered but onClick is blocked).
     */
    var disabled: Boolean = false

    /**
     * Props accessible by this component.
     * Children can access parent props.
     */
    protected open val props: MutableList<Prop<*>> = mutableListOf()

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
    fun getAllProps(): List<Prop<*>> {
        val allProps = mutableListOf<Prop<*>>()

        parent?.getAllProps()?.let { allProps.addAll(it) }
        allProps.addAll(props)

        return allProps
    }

    /**
     * Trigger an update of this component.
     */
    fun update(viewer: Player? = null) {
        view?.updateComponent(this, viewer)
    }

    override fun toString(): String {
        return "Component(area=$area, priority=$priority, width=$width, height=$height, children=$children, view=$view, updateInterval=$updateInterval, hidden=$hidden, disabled=$disabled, props=$props, attachedRef=${attachedRef.hashCode()})"
    }
}
