package dev.slne.surf.surfapi.bukkit.api.gui.component

import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import org.bukkit.inventory.ItemStack
import kotlin.time.Duration

/**
 * Base interface for all GUI components.
 * Components follow React-like lifecycle principles.
 */
abstract class Component {
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
     * Whether this component is currently mounted.
     */
    var isMounted: Boolean = false
        internal set
    
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
     * Called when the component is mounted.
     */
    open fun onMount(context: LifecycleContext) {}
    
    /**
     * Called when the component is unmounted.
     */
    open fun onUnmount(context: LifecycleContext) {}
    
    /**
     * Called when the component is updated.
     */
    open fun onUpdate(context: LifecycleContext) {}
    
    /**
     * Called when the component is clicked.
     */
    open fun onClick(context: ClickContext) {}
    
    /**
     * Renders the component to an ItemStack.
     */
    abstract fun render(context: ViewContext): ItemStack
    
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
        if (isMounted) {
            view.updateComponent(this)
        }
    }
    
    /**
     * Trigger an update of this component and all children.
     */
    fun updateWithChildren() {
        update()
        children.forEach { it.updateWithChildren() }
    }
}

/**
 * Simple item component that renders a static item.
 */
open class ItemComponent(
    private val item: ItemStack,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    
    override fun render(context: ViewContext): ItemStack = item
    
    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}

/**
 * Dynamic component that renders based on a callback.
 */
open class DynamicComponent(
    private val renderer: (ViewContext) -> ItemStack,
    private val clickHandler: (ClickContext.() -> Unit)? = null
) : Component() {
    
    override fun render(context: ViewContext): ItemStack = renderer(context)
    
    override fun onClick(context: ClickContext) {
        clickHandler?.invoke(context)
    }
}

/**
 * Container component that can hold multiple child components.
 */
abstract class ContainerComponent : Component() {
    /**
     * Layout children within the container.
     * Returns a map of slot positions to components.
     */
    abstract fun layout(context: ViewContext): Map<Int, Component>
    
    override fun render(context: ViewContext): ItemStack {
        // Container doesn't render itself, only lays out children
        return ItemStack.empty()
    }
}
