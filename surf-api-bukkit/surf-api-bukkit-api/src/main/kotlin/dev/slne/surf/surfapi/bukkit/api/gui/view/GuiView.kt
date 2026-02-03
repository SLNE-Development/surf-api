package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import net.kyori.adventure.text.Component as AdventureComponent
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import kotlin.time.Duration

/**
 * Configuration for a GUI view.
 */
data class ViewConfig(
    var title: AdventureComponent = AdventureComponent.text("GUI"),
    var size: Int = 54, // 6 rows by default for CHEST
    var type: InventoryType = InventoryType.CHEST,
    var cancelOnClick: Boolean = true,
    var closeOnClickOutside: Boolean = false
) {
    /**
     * Set rows (only for CHEST type).
     * Automatically adjusts size.
     */
    var rows: Int
        get() = size / 9
        set(value) {
            require(type == InventoryType.CHEST) { 
                "Rows can only be set for CHEST type inventories. For other types, the size is determined by the inventory type." 
            }
            require(value in 1..6) { "Rows must be between 1 and 6" }
            size = value * 9
        }
}

/**
 * Base class for all GUI views.
 * Views manage the overall GUI lifecycle and component rendering.
 */
abstract class GuiView {
    /**
     * Parent view for navigation.
     */
    var parent: GuiView? = null
        internal set
    
    /**
     * Props for this view.
     */
    protected open val props: Map<String, Prop<*>> = emptyMap()
    
    /**
     * Update interval for the entire view.
     */
    open val updateInterval: Duration? = null
    
    /**
     * Components in this view, mapped by slot.
     */
    private val _components = mutableMapOf<Int, Component>()
    val components: Map<Int, Component> get() = _components.toMap()
    
    /**
     * Whether this view has been initialized.
     */
    private var initialized = false
    
    /**
     * Whether this view has been rendered for the first time.
     */
    private val firstRenderPerViewer = mutableSetOf<java.util.UUID>()
    
    /**
     * Current viewers of this view.
     */
    private val viewers = mutableMapOf<java.util.UUID, Player>()
    
    /**
     * Configuration for this view.
     */
    val config = ViewConfig()
    
    /**
     * Initialize the view configuration.
     * Called once when the view is first created.
     */
    open fun onInit(config: ViewConfig) {}
    
    /**
     * Called when the view is opened for a player.
     */
    open fun onOpen(context: ViewContext) {}
    
    /**
     * Called the first time the view is rendered for a player.
     */
    open fun onFirstRender(context: RenderContext) {}
    
    /**
     * Called when the view is updated.
     */
    open fun onUpdate(context: ViewContext) {}
    
    /**
     * Called when navigating back to this view from a child view.
     */
    open fun onResume(context: ResumeContext) {}
    
    /**
     * Called when the view is closed.
     */
    open fun onClose(context: ViewContext) {}
    
    /**
     * Initialize the view if not already initialized.
     */
    internal fun ensureInitialized() {
        if (!initialized) {
            onInit(config)
            initialized = true
        }
    }
    
    /**
     * Open this view for a player.
     */
    open fun open(player: Player) {
        ensureInitialized()
        viewers[player.uniqueId] = player
        
        val context = createViewContext(player)
        onOpen(context)
        
        if (player.uniqueId !in firstRenderPerViewer) {
            val renderContext = createRenderContext(player)
            onFirstRender(renderContext)
            firstRenderPerViewer.add(player.uniqueId)
        }
    }
    
    /**
     * Close this view for a player.
     */
    open fun close(player: Player) {
        val context = createViewContext(player)
        onClose(context)
        viewers.remove(player.uniqueId)
    }
    
    /**
     * Update the view for all viewers.
     */
    fun update() {
        viewers.values.forEach { player ->
            val context = createViewContext(player)
            onUpdate(context)
        }
    }
    
    /**
     * Update a specific component.
     */
    internal fun updateComponent(component: Component) {
        viewers.values.forEach { player ->
            val context = createViewContext(player)
            val lifecycleContext = createLifecycleContext(player, LifecycleEventType.UPDATE)
            component.onUpdate(lifecycleContext)
        }
    }
    
    /**
     * Add a component at a slot.
     */
    fun addComponent(slot: Int, component: Component) {
        component.view = this
        _components[slot] = component
        
        if (!component.isMounted) {
            viewers.values.forEach { player ->
                val lifecycleContext = createLifecycleContext(player, LifecycleEventType.MOUNT)
                component.onMount(lifecycleContext)
            }
            component.isMounted = true
        }
    }
    
    /**
     * Remove a component from a slot.
     */
    fun removeComponent(slot: Int) {
        _components[slot]?.let { component ->
            viewers.values.forEach { player ->
                val lifecycleContext = createLifecycleContext(player, LifecycleEventType.UNMOUNT)
                component.onUnmount(lifecycleContext)
            }
            component.isMounted = false
        }
        _components.remove(slot)
    }
    
    /**
     * Get a prop value for a specific player.
     */
    fun <T> getProp(player: Player, prop: Prop<T>): T {
        val context = PropContext(player.uniqueId, player)
        return prop.get(context)
    }
    
    /**
     * Create a view context for a player.
     */
    protected abstract fun createViewContext(player: Player): ViewContext
    
    /**
     * Create a render context for a player.
     */
    protected abstract fun createRenderContext(player: Player): RenderContext
    
    /**
     * Create a lifecycle context for a player.
     */
    protected abstract fun createLifecycleContext(player: Player, eventType: LifecycleEventType): LifecycleContext
    
    /**
     * Create a resume context for a player.
     */
    protected abstract fun createResumeContext(player: Player, origin: GuiView?): ResumeContext
}

/**
 * Creates a child view with this view as parent.
 */
fun <T : GuiView> GuiView.childView(view: T): T {
    view.parent = this
    return view
}
