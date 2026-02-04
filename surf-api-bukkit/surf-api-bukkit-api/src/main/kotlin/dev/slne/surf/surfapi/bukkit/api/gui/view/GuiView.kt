package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import org.bukkit.entity.Player
import java.util.*
import kotlin.time.Duration

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
     * Update interval for the entire view.
     */
    open val updateInterval: Duration? = null

    /**
     * Components in this view.
     * Components can overlap, and priority determines rendering and click handling order.
     */
    private val _components = mutableListOf<Component>()
    val components: List<Component> get() = _components.toList()

    /**
     * Find all components that contain the given slot, sorted by priority (highest first).
     * Includes children recursively.
     */
    fun findComponentsBySlot(slot: Slot): List<Component> {
        val allComponents = mutableListOf<Component>()
        
        // Helper function to recursively collect components and their children
        fun collectComponents(component: Component) {
            allComponents.add(component)
            component.children.forEach { child ->
                collectComponents(child)
            }
        }
        
        // Collect all components including children
        _components.forEach { collectComponents(it) }
        
        // Filter by slot, exclude hidden components, and sort by priority
        return allComponents
            .filter { it.contains(slot) && !it.hidden }
            .sortedByDescending { it.priority.value }
    }

    /**
     * Whether this view has been initialized.
     */
    private var initialized = false

    /**
     * Whether this view has been rendered for the first time.
     */
    private val firstRenderPerViewer = mutableSetOf<UUID>()

    /**
     * Current viewers of this view.
     */
    private val viewers = mutableMapOf<UUID, Player>()

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
     * Calls the onUpdate lifecycle hook and refreshes the component's slots in the inventory.
     */
    internal fun updateComponent(component: Component) {
        viewers.values.forEach { player ->
            val lifecycleContext = createLifecycleContext(player, LifecycleEventType.UPDATE)

            component.onUpdate(lifecycleContext)
            
            // Refresh the component's slots to update the visual display
            refreshComponentSlotsInternal(player, component)
        }
    }
    
    /**
     * Internal method to refresh component slots.
     * Must be implemented by concrete view implementations.
     */
    protected abstract fun refreshComponentSlotsInternal(player: Player, component: Component)

    /**
     * Add a component to the view.
     */
    fun addComponent(component: Component) {
        component.view = this
        _components.add(component)
    }

    /**
     * Remove a component from the view.
     */
    fun removeComponent(component: Component) {
        _components.remove(component)
    }

    /**
     * Set the parent view for navigation.
     */
    fun withParent(parentView: GuiView): GuiView {
        this.parent = parentView

        return this
    }

    /**
     * Create a view context for a player.
     */
    abstract fun createViewContext(player: Player): ViewContext

    /**
     * Create a render context for a player.
     */
    abstract fun createRenderContext(player: Player): RenderContext

    /**
     * Create a lifecycle context for a player.
     */
    abstract fun createLifecycleContext(
        player: Player,
        eventType: LifecycleEventType
    ): LifecycleContext

    /**
     * Create a resume context for a player.
     */
    abstract fun createResumeContext(
        player: Player,
        origin: GuiView?
    ): ResumeContext
}

/**
 * Creates a child view with this view as parent.
 */
fun <T : GuiView> GuiView.childView(view: T): T {
    view.parent = this
    return view
}
