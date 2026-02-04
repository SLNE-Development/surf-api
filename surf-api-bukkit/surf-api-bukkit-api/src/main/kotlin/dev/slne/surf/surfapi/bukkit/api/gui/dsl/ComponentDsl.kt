package dev.slne.surf.surfapi.bukkit.api.gui.dsl

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.component.ComponentPriority
import dev.slne.surf.surfapi.bukkit.api.gui.component.DynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.component.ItemComponent
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.props.ComputedProp
import dev.slne.surf.surfapi.bukkit.api.gui.props.LazyProp
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.props.ViewerProp
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import kotlin.time.Duration

/**
 * DSL marker for component building.
 */
@DslMarker
annotation class ComponentDsl

/**
 * Builder for creating components.
 */
@ComponentDsl
class ComponentBuilder {
    var updateInterval: Duration? = null
    var ref: Ref<Component>? = null
    var priority: ComponentPriority = ComponentPriority.NORMAL
    var onUpdate: (LifecycleContext.() -> Unit)? = null
    var onClick: (ClickContext.() -> Unit)? = null

    private val _props = mutableMapOf<String, Prop<*>>()

    /**
     * Add a prop to this component.
     */
    fun <T> prop(name: String, prop: Prop<T>) {
        _props[name] = prop
    }

    /**
     * Build the component.
     */
    internal fun build(slot: Slot, renderer: (ViewContext) -> GuiItem?): Component {
        return object : DynamicComponent(slot, renderer, priority, onClick) {
            override val updateInterval: Duration? = this@ComponentBuilder.updateInterval
            override val props: Map<String, Prop<*>> = _props

            override fun onUpdate(context: LifecycleContext) {
                super.onUpdate(context)
                this@ComponentBuilder.onUpdate?.invoke(context)
            }

            override fun toString(): String {
                return "DSLGeneratedDynamicComponent(updateInterval=$updateInterval, props=$props) ${super.toString()}"
            }
        }.also { component ->
            ref?.set(component)
            component.attachedRef = ref
        }
    }
}

/**
 * Create a component with a static item at a specific slot.
 */
fun component(
    slot: Slot,
    item: GuiItem,
    priority: ComponentPriority = ComponentPriority.NORMAL,
    builder: ComponentBuilder.() -> Unit = {}
): Component {
    val componentBuilder = ComponentBuilder()
    componentBuilder.priority = priority
    componentBuilder.builder()
    return componentBuilder.build(slot) { item }
}

/**
 * Create a component with a dynamic renderer at a specific slot.
 */
fun dynamicComponent(
    slot: Slot,
    renderer: (ViewContext) -> GuiItem?,
    priority: ComponentPriority = ComponentPriority.NORMAL,
    onUpdate: (LifecycleContext.() -> Unit)? = null,
    builder: ComponentBuilder.() -> Unit = {}
): Component {
    val componentBuilder = ComponentBuilder()
    componentBuilder.priority = priority
    componentBuilder.onUpdate = onUpdate
    componentBuilder.builder()
    return componentBuilder.build(slot, renderer)
}

/**
 * DSL for creating props.
 */
@ComponentDsl
class PropsBuilder {
    private val _props = mutableMapOf<String, Prop<*>>()

    /**
     * Create an immutable prop.
     */
    fun <T> immutable(name: String, value: T): Prop.Immutable<T> {
        return Prop.Immutable(name, value).also { _props[name] = it }
    }

    /**
     * Create a mutable prop (global to view).
     */
    fun <T> mutable(name: String, initialValue: T?): Prop.Mutable<T> {
        return Prop.Mutable(name, initialValue).also { _props[name] = it }
    }

    /**
     * Create a viewer-specific immutable prop.
     */
    fun <T> viewerImmutable(name: String, initialValue: T): ViewerProp<T> {
        return ViewerProp(name, initialValue).also { _props[name] = it }
    }

    /**
     * Create a viewer-specific mutable prop.
     */
    fun <T> viewerMutable(name: String, initialValue: T?): ViewerProp.Mutable<T> {
        return ViewerProp.Mutable(name, initialValue).also { _props[name] = it }
    }

    /**
     * Create a computed prop.
     */
    fun <T> computed(name: String, compute: suspend () -> T): ComputedProp<T> {
        return ComputedProp(name, compute).also { _props[name] = it }
    }

    /**
     * Create an immutable lazy prop.
     */
    fun <T> immutableLazy(name: String, initializer: () -> T): LazyProp<T> {
        return LazyProp(name, initializer).also { _props[name] = it }
    }

    /**
     * Create a mutable lazy prop.
     */
    fun <T> mutableLazy(name: String, initializer: () -> T?): LazyProp.Mutable<T> {
        return LazyProp.Mutable(name, initializer).also { _props[name] = it }
    }

    /**
     * Get all props.
     */
    internal fun build(): Map<String, Prop<*>> = _props.toMap()
}

/**
 * Create props using DSL.
 */
fun props(builder: PropsBuilder.() -> Unit): Map<String, Prop<*>> {
    val propsBuilder = PropsBuilder()
    propsBuilder.builder()
    return propsBuilder.build()
}

/**
 * DSL for rendering components in a view.
 */
@ComponentDsl
fun RenderContext.slot(component: Component) {
    renderComponent(component)
}

/**
 * DSL for rendering components in a view with item at a specific slot.
 */
@ComponentDsl
fun RenderContext.slot(
    slot: Slot,
    item: GuiItem,
    priority: ComponentPriority = ComponentPriority.NORMAL,
    onClick: (ClickContext.() -> Unit)? = null
) {
    val component = ItemComponent(slot, item, priority, onClick)
    renderComponent(component)
}
