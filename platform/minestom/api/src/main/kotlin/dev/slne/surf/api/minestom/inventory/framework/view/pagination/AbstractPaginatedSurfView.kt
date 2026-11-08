package dev.slne.surf.api.minestom.inventory.framework.view.pagination

import dev.slne.surf.api.core.util.prepend
import dev.slne.surf.api.minestom.inventory.framework.view.AbstractSurfView
import dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.addChild
import dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.blockCell
import dev.slne.surf.api.minestom.inventory.framework.view.container.dsl.removeChildrenOfType
import dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginatedViewSettings
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.MinestomItemComponentBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.context.*
import me.devnatan.inventoryframework.state.State
import me.devnatan.inventoryframework.state.StateValue
import me.devnatan.inventoryframework.state.StateValueHost
import me.devnatan.inventoryframework.state.StateWatcher
import net.minestom.server.item.ItemStack
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
 * - A [PaginationPageIndicatorComponent] between the two buttons showing the current page and the
 *   page count, as rendered by
 *   [paginationPageIndicator][dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginatedViewSettings.paginationPageIndicator].
 *
 * The initial pagination glyph is updated asynchronously one tick after the pagination state
 * first resolves (via [InitialPaginationStateWatcher]) to work around scheduling constraints.
 *
 * Subclasses are not created directly — use [paginatedSurfView][dev.slne.surf.api.minestom.inventory.framework.view.paginatedSurfView] instead.
 *
 * @param header the plain-text title rendered in the inventory header
 * @see dev.slne.surf.api.minestom.inventory.framework.view.paginatedSurfView
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
                    updatePaginationOverlay(context)
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
    protected abstract fun createPagination(): PaginationStateBuilder<Context, MinestomItemComponentBuilder, *>

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


    /**
     * Re-renders the pagination overlay in the header: the navigation button glyph for the current
     * state, plus the page counter between the two buttons.
     */
    private fun updatePaginationOverlay(context: Context) {
        val pagination = paginationState.get(context) ?: return
        val row = settings.paginationButtonRow
        val geometry = settings.headerGeometry

        val buttonGlyph = PaginationButtonGlyphComponent.getByPaginationState(
            row = row,
            pagination = pagination,
            geometry = geometry
        )

        val totalPages = pagination.lastPage().coerceAtLeast(1)
        val currentPage = pagination.currentPage().coerceIn(1, totalPages)

        val pageText = settings.paginationPageIndicator
            ?.render(currentPage, totalPages)

        modifyContainer(context) {
            removeChildrenOfType<PaginationButtonGlyphComponent>()
            addChild(buttonGlyph)

            removeChildrenOfType<PaginationPageIndicatorComponent>()
            if (pageText != null) {
                addChild(
                    PaginationPageIndicatorComponent(
                        row = row,
                        text = pageText,
                        font = settings.rowFont(row),
                        geometry = geometry,
                        defaultColor = settings.headerTextColor,
                        metrics = settings.rowFontMetrics
                    )
                )
            }
        }
    }

    /**
     * Applies the paginated container defaults: blocks all border cells, the button row and every
     * empty row, then calls [applyContainerDefaults] for subclass customisation.
     *
     * This override is `final` — subclasses should override [applyContainerDefaults] instead.
     */
    context(_: ViewContainerModificationContext)
    final override fun containerDefaults() {
        val paginationContentRows = settings.paginationContentRows

        for (y in 1..settings.rows.rows) {
            for (x in 0 until 9) {
                if (y in paginationContentRows && x in 1..7) continue
                blockCell(x, y, settings.headerGeometry)
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

    /**
     * Plays the configured
     * [paginationSwitchSound][dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginatedViewSettings.paginationSwitchSound]
     * to the viewer that just navigated to another page. Does nothing if no sound is configured.
     */
    private fun playPageSwitchSound(click: SlotClickContext) {
        val sound = settings.paginationSwitchSound ?: return
        click.player.playSound(sound)
    }

    final override fun onViewInit(config: ViewConfigBuilder) {
        paginationState // initialize pagination state
        onPaginatedInit(config)
        config.layout(*createLayout())
    }

    /**
     * Builds the layout: only the
     * [paginationContentRows][dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginatedViewSettings.paginationContentRows]
     * are filled with [layoutTarget] slots. The button row and the
     * [paginationEmptyRows][dev.slne.surf.api.minestom.inventory.framework.view.settings.PaginatedViewSettings.paginationEmptyRows]
     * rows at the opposite inventory edge stay empty.
     */
    private fun createLayout(): Array<String> {
        val contentRows = settings.paginationContentRows

        return Array(settings.rows.rows) { index ->
            if (index + 1 in contentRows) paginationRow else EMPTY_ROW
        }
    }

    final override fun onViewOpen(open: OpenContext) {
        onPaginatedOpen(open)
    }

    final override fun onViewRender(render: RenderContext) {
        val pagination = paginationState.get(render) ?: return
        val paginationButtonRow = settings.paginationButtonRow

        if (pagination.isStatic) {
            nextTick {
                updatePaginationOverlay(render)
            }
        } else {
            render.watchState(pagination.id, InitialPaginationStateWatcher())
        }

        render.slot(PaginationButton.LEFT.clickSlot(paginationButtonRow))
            .withItem(ItemStack.AIR)
            .updateOnStateChange(paginationState)
            .displayIf(pagination::canBack)
            .onClick { click: SlotClickContext ->
                if (!pagination.canBack()) return@onClick
                pagination.back()
                playPageSwitchSound(click)
            }

        render.slot(PaginationButton.RIGHT.clickSlot(paginationButtonRow))
            .withItem(ItemStack.AIR)
            .updateOnStateChange(paginationState)
            .displayIf(pagination::canAdvance)
            .onClick { click: SlotClickContext ->
                if (!pagination.canAdvance()) return@onClick
                pagination.advance()
                playPageSwitchSound(click)
            }

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
        if (pagination != null && pagination.hasPage(pagination.currentPageIndex())) {
            pagination.switchTo(pagination.currentPageIndex()) // trigger pagination state update to refresh dynamic elements
        }

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

            nextTick {
                updatePaginationOverlay(host)
            }
        }
    }

    companion object {
        private val EMPTY_ROW = " ".repeat(9)
    }
}
