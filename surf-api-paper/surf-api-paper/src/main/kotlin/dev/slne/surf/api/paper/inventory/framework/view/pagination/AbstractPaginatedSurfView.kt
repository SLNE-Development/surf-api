package dev.slne.surf.api.paper.inventory.framework.view.pagination

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import com.github.shynixn.mccoroutine.folia.ticks
import dev.slne.surf.api.core.util.prepend
import dev.slne.surf.api.paper.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.addChild
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.blockCell
import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.removeChildrenOfType
import dev.slne.surf.api.paper.inventory.framework.view.settings.PaginatedViewSettings
import kotlinx.coroutines.delay
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.context.*
import me.devnatan.inventoryframework.state.State
import me.devnatan.inventoryframework.state.StateValue
import me.devnatan.inventoryframework.state.StateValueHost
import me.devnatan.inventoryframework.state.StateWatcher
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Abstract base class for paginated Surf inventory views.
 *
 * [AbstractPaginatedSurfView] extends [AbstractSurfView] with pagination support provided
 * by the inventory framework. It manages a [Pagination] state that is automatically
 * initialized from [createPagination] and wires up:
 * - A layout containing a configurable [layoutTarget] character that marks paginated item slots.
 * - Left/right navigation buttons in the designated button row.
 * - A [PaginationButtonGlyphComponent] overlay that reflects the current pagination state
 *   (both-disabled, left-disabled, right-disabled, or both-enabled).
 *
 * The initial pagination glyph is updated asynchronously one tick after the pagination state
 * first resolves (via [InitialPaginationStateWatcher]) to work around Folia scheduling constraints.
 *
 * Subclasses are not created directly — use [paginatedSurfView][dev.slne.surf.api.paper.api.inventory.framework.view.paginatedSurfView] instead.
 *
 * @param header the plain-text title rendered in the inventory header
 * @see dev.slne.surf.api.paper.api.inventory.framework.view.paginatedSurfView
 * @see PaginatedViewSettings
 */
@Suppress("UnstableApiUsage")
abstract class AbstractPaginatedSurfView(header: String) : AbstractSurfView(header) {
    /**
     * The layout character that identifies pagination item slots in the inventory layout.
     * Must match the character used in the layout pattern passed to [ViewConfigBuilder.layout].
     */
    protected abstract val layoutTarget: Char
    override val settings: PaginatedViewSettings = PaginatedViewSettings()

    private val paginationState: State<Pagination> by lazy {
        createPagination()
            .layoutTarget(layoutTarget)
            .apply {
                onPageSwitch(pageSwitchHandler.prepend { context, _ ->
                    updatePaginationGlyph(context)
                })
            }.build()
    }

    private val paginationRow: String by lazy { " " + layoutTarget.toString().repeat(7) + " " }

    /**
     * Creates and returns the [PaginationStateBuilder] that configures the pagination data source
     * and item factory. Called once lazily the first time [paginationState] is accessed.
     *
     * @return a configured [PaginationStateBuilder]
     */
    protected abstract fun createPagination(): PaginationStateBuilder<Context, BukkitItemComponentBuilder, *>

    /**
     * Called during [onViewInit] after the container defaults are applied.
     * Override to perform additional [ViewConfigBuilder] configuration.
     *
     * @param config the [ViewConfigBuilder] from the inventory framework
     */
    protected open fun onPaginatedInit(config: ViewConfigBuilder) = Unit

    /**
     * Called during [onViewOpen].
     * Override to react to the view being opened for a player.
     *
     * @param open the [OpenContext] from the inventory framework
     */
    protected open fun onPaginatedOpen(open: OpenContext) = Unit

    /**
     * Called during [onViewRender].
     * Override to place additional items in the inventory.
     *
     * @param render the [RenderContext] from the inventory framework
     */
    protected open fun onPaginatedRender(render: RenderContext) = Unit

    /**
     * Called during [onViewClick].
     * Override to handle slot click events within the paginated view.
     *
     * @param click the [SlotClickContext] from the inventory framework
     */
    protected open fun onPaginatedClick(click: SlotClickContext) = Unit

    /**
     * Called during [onViewClose].
     * Override to react to the view being closed.
     *
     * @param close the [CloseContext] from the inventory framework
     */
    protected open fun onPaginatedClose(close: CloseContext) = Unit

    /**
     * Called during [onViewUpdate].
     * Override to update the view's contents on state changes.
     *
     * @param update the [Context] from the inventory framework
     */
    protected open fun onPaginatedUpdate(update: Context) = Unit


    private fun updatePaginationGlyph(context: Context) {
        val pagination = paginationState.get(context) ?: return
        val buttonGlyph = PaginationButtonGlyphComponent.getByPaginationState(
            row = settings.paginationButtonRow,
            pagination = pagination
        )

        modifyContainer(context) {
            removeChildrenOfType<PaginationButtonGlyphComponent>()
            addChild(buttonGlyph)
        }
    }

    /**
     * Applies the paginated container defaults: blocks all border cells and all cells outside
     * the pagination content rows, then calls [applyContainerDefaults] for subclass customisation.
     *
     * This override is `final` — subclasses should override [applyContainerDefaults] instead.
     */
    context(_: ViewContainerModificationContext)
    final override fun containerDefaults() {
        val paginationContentRows = settings.paginationViewRows.paginationContentRows

        for (y in 1..settings.rows.rows) {
            for (x in 0 until 9) {
                if (y in paginationContentRows && x in 1..7) continue
                blockCell(x, y)
            }
        }

        applyContainerDefaults()
    }

    /**
     * Override this hook in [PaginatedSurfViewDSLImpl] (or subclasses) to add additional container
     * components after the standard block cells have been applied.
     *
     * Called from [containerDefaults] after the block cells are placed.
     */
    context(_: ViewContainerModificationContext)
    protected open fun applyContainerDefaults() {
    }

    final override fun onViewInit(config: ViewConfigBuilder) {
        paginationState // initialize pagination state
        onPaginatedInit(config)
        config.layout(*createLayout())
    }

    private fun createLayout(): Array<String> {
        val layout = arrayOfNulls<String>(settings.rows.rows)
        layout[0] = EMPTY_ROW
        repeat(settings.rows.rows - 2) { i ->
            layout[i + 1] = paginationRow
        }
        layout[layout.lastIndex] = EMPTY_ROW

        return layout.requireNoNulls()
    }

    final override fun onViewOpen(open: OpenContext) {
        onPaginatedOpen(open)
    }

    final override fun onViewRender(render: RenderContext) {
        val pagination = paginationState.get(render) ?: return
        val paginationButtonRow = settings.paginationButtonRow

        render.watchState(pagination.id, InitialPaginationStateWatcher())

        render.slot(PaginationButton.LEFT.clickSlot(paginationButtonRow))
            .withItem(ItemStack.empty())
            .updateOnStateChange(paginationState)
            .displayIf(pagination::canBack)
            .onClick(pagination::back)

        render.slot(PaginationButton.RIGHT.clickSlot(paginationButtonRow))
            .withItem(ItemStack.empty())
            .updateOnStateChange(paginationState)
            .displayIf(pagination::canAdvance)
            .onClick(pagination::advance)

        onPaginatedRender(render)
    }

    final override fun onViewClick(click: SlotClickContext) {
        onPaginatedClick(click)
    }

    final override fun onViewClose(close: CloseContext) {
        onPaginatedClose(close)
    }


    final override fun onViewUpdate(update: Context) {
        val pagination = paginationState.get(update)
        pagination.switchTo(pagination.currentPage()) // trigger pagination state update to refresh dynamic elements

        onPaginatedUpdate(update)
    }

    private inner class InitialPaginationStateWatcher : StateWatcher {
        private val initialHandled = AtomicBoolean(false)

        override fun stateRegistered(state: State<*>, caller: Any?) {}
        override fun stateUnregistered(state: State<*>, caller: Any?) {}
        override fun stateValueGet(
            state: State<*>,
            host: StateValueHost,
            internalValue: StateValue,
            rawValue: Any?
        ) {
        }

        override fun stateValueSet(
            host: StateValueHost,
            value: StateValue,
            rawOldValue: Any?,
            rawNewValue: Any?
        ) {
            val pagination = value as? Pagination ?: return
            if (pagination.isLoading) return
            if (!initialHandled.compareAndSet(false, true)) return
            if (host !is Context) return

            val plugin = JavaPlugin.getProvidingPlugin(javaClass)
            plugin.launch(plugin.entityDispatcher(host.player)) {
                delay(1.ticks)
                updatePaginationGlyph(host)
            }
        }
    }

    companion object {
        private val EMPTY_ROW = " ".repeat(9)
    }
}