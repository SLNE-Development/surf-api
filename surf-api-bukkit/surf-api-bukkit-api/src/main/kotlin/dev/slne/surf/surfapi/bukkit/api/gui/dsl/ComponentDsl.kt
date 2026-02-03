package dev.slne.surf.surfapi.bukkit.api.gui.dsl

import dev.slne.surf.surfapi.bukkit.api.gui.GuiItem
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.component.DynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.component.ItemComponent
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.props.*
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
    internal fun build(renderer: (ViewContext) -> GuiItem?): Component {
        return object : DynamicComponent(renderer, onClick) {
            override val updateInterval: Duration? = this@ComponentBuilder.updateInterval
            override val props: Map<String, Prop<*>> = _props

            override fun onUpdate(context: LifecycleContext) {
                super.onUpdate(context)
                this@ComponentBuilder.onUpdate?.invoke(context)
            }
        }.also { component ->
            ref?.set(component)
            component.attachedRef = ref as? Ref<Component>
        }
    }
}

/**
 * Create a component with a static item.
 */
fun component(
    item: GuiItem,
    builder: ComponentBuilder.() -> Unit = {}
): Component {
    val componentBuilder = ComponentBuilder()
    componentBuilder.builder()
    return componentBuilder.build { item }
}

/**
 * Create a component with a dynamic renderer.
 */
fun dynamicComponent(
    renderer: (ViewContext) -> GuiItem?,
    builder: ComponentBuilder.() -> Unit = {}
): Component {
    val componentBuilder = ComponentBuilder()
    componentBuilder.builder()
    return componentBuilder.build(renderer)
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
    fun <T> immutable(name: String, value: T): ImmutableProp<T> {
        return ImmutableProp(name, value).also { _props[name] = it }
    }

    /**
     * Create a mutable prop (global to view).
     */
    fun <T> mutable(name: String, initialValue: T): MutableProp<T> {
        return MutableProp(name, initialValue).also { _props[name] = it }
    }

    /**
     * Create a viewer-specific mutable prop.
     */
    fun <T> viewerMutable(name: String, initialValue: T): ViewerMutableProp<T> {
        return ViewerMutableProp(name, initialValue).also { _props[name] = it }
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
    fun <T> immutableLazy(name: String, initializer: () -> T): ImmutableLazyProp<T> {
        return ImmutableLazyProp(name, initializer).also { _props[name] = it }
    }

    /**
     * Create a mutable lazy prop.
     */
    fun <T> mutableLazy(name: String, initializer: () -> T): MutableLazyProp<T> {
        return MutableLazyProp(name, initializer).also { _props[name] = it }
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
fun RenderContext.slot(slot: Slot, component: Component) {
    renderComponent(slot, component)
}

/**
 * DSL for rendering components in a view with item.
 */
@ComponentDsl
fun RenderContext.slot(slot: Slot, item: GuiItem, onClick: (ClickContext.() -> Unit)? = null) {
    val component = ItemComponent(item, onClick)
    renderComponent(slot, component)
}
