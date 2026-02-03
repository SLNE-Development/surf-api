package dev.slne.surf.surfapi.bukkit.api.gui.view

import dev.slne.surf.surfapi.bukkit.api.gui.Slot
import dev.slne.surf.surfapi.bukkit.api.gui.component.Component
import dev.slne.surf.surfapi.bukkit.api.gui.context.*
import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import org.bukkit.entity.Player
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
    private val _components = mutableMapOf<Slot, Component>()
    val components: Map<Slot, Component> get() = _components.toMap()

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
    fun addComponent(slot: Slot, component: Component) {
        component.view = this
        _components[slot] = component
    }

    /**
     * Remove a component from a slot.
     */
    fun removeComponent(slot: Slot) {
        _components.remove(slot)
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
    protected abstract fun createLifecycleContext(
        player: Player,
        eventType: LifecycleEventType
    ): LifecycleContext

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
