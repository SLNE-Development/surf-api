package dev.slne.surf.surfapi.bukkit.api.gui.view

import com.github.shynixn.mccoroutine.folia.SuspendingJavaPlugin
import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.surfapi.bukkit.api.event.cancel
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.context.abstract.*
import dev.slne.surf.surfapi.bukkit.api.gui.toItemStack
import dev.slne.surf.surfapi.core.api.util.freeze
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import dev.slne.surf.surfapi.core.api.util.toObjectList
import it.unimi.dsi.fastutil.objects.ObjectList
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.plugin.java.JavaPlugin
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

    val title by config::title
    val size by config::size
    val type by config::type
    val cancelOnClick by config::cancelOnClick

    protected fun init() {
        onInit(createInitializeContext())
        recreateInventory()
    }

    protected fun recreateInventory() {
        this.inventory = when (config.type) {
            InventoryType.CHEST -> {
                Bukkit.createInventory(null, config.size, config.title)
            }

            else -> {
                Bukkit.createInventory(null, config.type, config.title)
            }
        }
    }

    fun modifyConfig(modifier: (config: ViewConfig) -> Unit) {
        modifier(config)

        initialized = false

        ensureInitialized()

        val plugin = JavaPlugin.getProvidingPlugin(GuiView::class.java) as SuspendingJavaPlugin

        plugin.launch {
            viewerPlayers().forEach { viewer ->
                withContext(plugin.entityDispatcher(viewer)) {
                    viewer.closeInventory()
                    open(viewer)
                }
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

        renderAllSlots(player)

        // Open inventory
        player.openInventory(inventory)
    }

    /**
     * Close this view for a player.
     */
    open fun close(player: Player) {
        val context = createViewContext(player)
        onClose(context)
        viewers.remove(player.uniqueId)
        ViewManager.removeActiveView(player)
    }

    /**
     * Update the view for all viewers.
     */
    fun update() {
        viewerPlayers().forEach { player ->
            onUpdate(createViewContext(player))
        }
    }

    private fun renderAllSlots(player: Player) {
        val allSlots = (0 until inventory.size).map { Slot.of(it) }

        allSlots.forEach { slot ->
            renderSlot(player, slot)
        }
    }

    private fun renderSlot(
        player: Player,
        slot: Slot
    ) {
        if (slot.index >= inventory.size) return

        val resolved = resolveSlot(player, slot)
        resolved?.component?.renderFirstRenderPerPlayer(player)

        inventory.setItem(
            slot.index,
            resolved?.guiItem?.toItemStack()
        )
    }


    /**
     * Refresh the inventory for a player.
     */
    internal fun refreshInventory(player: Player) {
        // Clear inventory first
        inventory.clear()

        // Re-render all slots
        renderAllSlots(player)
    }

    /**
     * Refresh all slots occupied by a specific component and its children.
     */
    internal fun refreshComponentSlots(player: Player, component: Component) {
        // Collect all slots from this component and all its children recursively
        // Only refresh this component's own slots, not children's
        // Children will refresh their own slots when updateChildrenRecursively calls them
        fun collectAllSlots(comp: Component): Set<Slot> {
            val slots = mutableSetOf(*comp.area.slots().toTypedArray())

            comp.children.forEach { child ->
                slots.addAll(collectAllSlots(child))
            }

            return slots
        }

        collectAllSlots(component).forEach { slot ->
            renderSlot(player, slot)
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
            refreshComponentSlots(player, component)
        }
    }

    private fun resolveSlot(
        player: Player,
        slot: Slot
    ): ResolvedSlot? {
        val componentsAtSlot = findComponentsBySlot(slot)
        if (componentsAtSlot.isEmpty()) return null

        val highestPriority = componentsAtSlot.first().priority.value
        val candidates = componentsAtSlot
            .takeWhile { it.priority.value == highestPriority }

        val viewContext = createViewContext(player)

        for (component in candidates) {
            component.initComponent(
                createLifecycleContext(
                    player,
                    LifecycleEventType.INIT_COMPONENT
                )
            )
            if (component.hidden) continue

            val slots = component.renderSlots(viewContext)

            if (slots.isNotEmpty()) {
                slots[slot]?.let { guiItem ->
                    return ResolvedSlot(component, guiItem)
                }
            } else if (slot == component.area.first()) {
                component.render(viewContext)?.let { guiItem ->
                    return ResolvedSlot(component, guiItem)
                }
            }
        }

        return null
    }

    /**
     * Handle click event.
     */
    fun handleClick(player: Player, event: InventoryClickEvent) {
        if (config.cancelOnClick) {
            event.isCancelled = true
        }

        val slot = Slot.of(event.rawSlot)
        val resolved = resolveSlot(player, slot) ?: return

        if (resolved.component.disabled) return event.cancel()
        if (resolved.component.cancelOnClick) event.cancel()

        resolved.component.onClick(createClickContext(player, event, resolved.component))
    }

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
    fun createClickContext(
        player: Player,
        event: InventoryClickEvent,
        component: Component,
    ): ClickContext = AbstractClickContext(this, player, event, component)

    /**
     * Create a view context for a player.
     */
    fun createViewContext(player: Player): ViewContext = AbstractViewContext(this, player)

    /**
     * Create a render context for a player.
     */
    fun createInitializeContext(): InitializeContext = AbstractInitializeContext(config, this)

    /**
     * Create a lifecycle context for a player.
     */
    fun createLifecycleContext(
        player: Player,
        eventType: LifecycleEventType
    ): LifecycleContext = AbstractLifecycleContext(this, player, eventType)

    /**
     * Create a resume context for a player.
     */
    fun createResumeContext(
        player: Player,
        origin: GuiView?
    ): ResumeContext = AbstractResumeContext(this, player, origin)

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
