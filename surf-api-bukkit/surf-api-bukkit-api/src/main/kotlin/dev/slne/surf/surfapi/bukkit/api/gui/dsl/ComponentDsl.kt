package dev.slne.surf.surfapi.bukkit.api.gui.dsl

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.component.DynamicComponent
import dev.slne.surf.surfapi.bukkit.api.gui.component.ItemComponent
import dev.slne.surf.surfapi.bukkit.api.gui.context.ClickContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.RenderContext
import dev.slne.surf.surfapi.bukkit.api.gui.context.ViewContext
import dev.slne.surf.surfapi.bukkit.api.gui.props.*
import dev.slne.surf.surfapi.bukkit.api.gui.ref.Ref
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import org.bukkit.inventory.ItemStack
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
    var onMount: (LifecycleContext.() -> Unit)? = null
    var onUnmount: (LifecycleContext.() -> Unit)? = null
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
    internal fun build(renderer: (ViewContext) -> ItemStack): Component {
        return object : DynamicComponent(renderer, onClick) {
            override val updateInterval: Duration? = this@ComponentBuilder.updateInterval
            override val props: Map<String, Prop<*>> = _props
            
            override fun onMount(context: LifecycleContext) {
                super.onMount(context)
                this@ComponentBuilder.onMount?.invoke(context)
            }
            
            override fun onUnmount(context: LifecycleContext) {
                super.onUnmount(context)
                this@ComponentBuilder.onUnmount?.invoke(context)
            }
            
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
    item: ItemStack,
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
    renderer: (ViewContext) -> ItemStack,
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
    fun <T> immutable(name: String, value: T, scope: PropScope = PropScope.VIEWER): ImmutableProp<T> {
        return ImmutableProp(name, value, scope).also { _props[name] = it }
    }
    
    /**
     * Create a mutable prop.
     */
    fun <T> mutable(name: String, initialValue: T, scope: PropScope = PropScope.VIEWER): MutableProp<T> {
        return MutableProp(name, initialValue, scope).also { _props[name] = it }
    }
    
    /**
     * Create a computed prop.
     */
    fun <T> computed(name: String, scope: PropScope = PropScope.VIEWER, compute: (PropContext) -> T): ComputedProp<T> {
        return ComputedProp(name, compute, scope).also { _props[name] = it }
    }
    
    /**
     * Create a lazy prop.
     */
    fun <T> lazy(name: String, mutable: Boolean = false, scope: PropScope = PropScope.VIEWER, initializer: (PropContext) -> T): LazyProp<T> {
        return LazyProp(name, initializer, mutable, scope).also { _props[name] = it }
    }
    
    /**
     * Create a pagination prop.
     */
    fun <T> pagination(name: String = "pagination", pageSize: Int = 9, scope: PropScope = PropScope.VIEWER, items: () -> List<T>): PaginationProp<T> {
        return PaginationProp(name, items, pageSize, scope).also { _props[name] = it }
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
fun RenderContext.slot(slot: Int, component: Component) {
    renderComponent(slot, component)
}

/**
 * DSL for rendering components in a view with item.
 */
@ComponentDsl
fun RenderContext.slot(slot: Int, item: ItemStack, onClick: (ClickContext.() -> Unit)? = null) {
    val component = ItemComponent(item, onClick)
    renderComponent(slot, component)
}
