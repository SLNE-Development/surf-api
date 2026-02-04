package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import it.unimi.dsi.fastutil.objects.ObjectList
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates

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

    var inventory: Inventory by Delegates.notNull()
        private set

    /**
     * Components in this view.
     * Components can overlap, and priority determines rendering and click handling order.
     */
    private val _components = mutableObjectListOf<Component>()
    val components: ObjectList<Component> get() = _components.freeze()

    /**
     * Find all components that contain the given slot, sorted by priority (highest first).
     * Includes children recursively.
     */
    fun findComponentsBySlot(slot: Slot): ObjectList<Component> {
        val allComponents = mutableObjectListOf<Component>()

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
            .toObjectList()
    }

    /**
     * Whether this view has been initialized.
     */
    @Volatile
    private var initialized = false

    /**
     * Current viewers of this view.
     */
    private val viewers = ConcurrentHashMap.newKeySet<UUID>()

    /**
     * Configuration for this view.
     */
    internal val config = ViewConfig()

    protected fun init() {
        onInit(createInitializeContext())

        this.inventory = when (config.type) {
            InventoryType.CHEST -> {
                Bukkit.createInventory(null, config.size, config.title)
            }

            else -> {
                Bukkit.createInventory(null, config.type, config.title)
            }
        }
    }

    /**
     * Initialize the view configuration.
     * Called once when the view is first created.
     */
    open fun onInit(context: InitializeContext) {}

    /**
     * Called when the view is opened for a player.
     */
    open fun onOpen(context: ViewContext) {}

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
            synchronized(this) {
                if (!initialized) {
                    init()
                    initialized = true
                }
            }
        }
    }

    fun viewerPlayers(): List<Player> {
        val players = mutableObjectListOf<Player>()
        val it = viewers.iterator()

        while (it.hasNext()) {
            val id = it.next()
            val player = server.getPlayer(id)
            if (player == null) {
                it.remove()
            } else {
                players.add(player)
            }
        }

        return players
    }

    /**
     * Open this view for a player.
     */
    open fun open(player: Player) {
        ensureInitialized()

        ViewManager.setActiveView(player, this)
        viewers.add(player.uniqueId)

        onOpen(createViewContext(player))
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
        viewerPlayers().forEach { player ->
            onUpdate(createViewContext(player))
        }
    }

    /**
     * Update a specific component.
     * Calls the onUpdate lifecycle hook and refreshes the component's slots in the inventory.
     * @param component The component to update
     * @param viewer The specific viewer to update for, or null to update for all viewers
     */
    internal fun updateComponent(component: Component, viewer: Player? = null) {
        val viewersToUpdate = if (viewer != null) {
            listOf(viewer)
        } else {
            viewerPlayers()
        }

        viewersToUpdate.forEach { player ->
            val lifecycleContext = createLifecycleContext(player, LifecycleEventType.UPDATE)

            // Call onUpdate on this component
            component.onUpdate(lifecycleContext)

            // Recursively call onUpdate on all children with the same viewer context
            // This allows children to update their state (like hidden/disabled properties)
            fun updateChildrenRecursively(comp: Component) {
                comp.children.forEach { child ->
                    child.onUpdate(lifecycleContext)
                    updateChildrenRecursively(child)
                }
            }

            updateChildrenRecursively(component)

            // Refresh the component's slots to update the visual display
            // This now includes all children's slots, so everything updates together
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
     * Create a click context for a player and click event.
     */
    abstract fun createClickContext(
        player: Player,
        event: InventoryClickEvent,
        component: Component,
    ): ClickContext

    /**
     * Create a view context for a player.
     */
    abstract fun createViewContext(player: Player): ViewContext

    /**
     * Create a render context for a player.
     */
    abstract fun createInitializeContext(): InitializeContext

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

    override fun toString(): String {
        return "GuiView(parent=$parent, components=$components, initialized=$initialized, viewers=$viewers, config=$config)"
    }
}

/**
 * Creates a child view with this view as parent.
 */
fun <T : GuiView> GuiView.childView(view: T): T {
    view.parent = this
    return view
}
