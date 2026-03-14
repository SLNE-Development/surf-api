package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.PaginatedViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SimpleViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.StateRegistry
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.context.*

/**
 * Internal context object that accumulates the lifecycle callbacks and state registry for
 * a DSL-configured Surf view.
 *
 * An instance of this class is created by [surfView] or [paginatedSurfView] and acts as
 * a DSL receiver for the top-level `onInit`, `onOpen`, `onFirstRender`, `onClick`, `onClose`,
 * `onUpdate`, and `containerDefaults` DSL functions defined in `SurfViewLifecycleDsl.kt`.
 *
 * This class is not meant to be used directly — use the DSL builder functions instead.
 *
 * @param ViewRef the concrete view reference type (e.g. [SurfViewRef] or [PaginatedSurfViewRef])
 * @see surfView
 * @see paginatedSurfView
 * @see SurfViewRef
 * @see AbstractSurfViewRef
 */
@InventoryFramworkDSL
abstract class AbstractSurfViewContext<ViewRef : AbstractSurfViewRef> @PublishedApi internal constructor() {
    @PublishedApi
    internal val stateRegistry = StateRegistry()

    @PublishedApi
    internal var onInit: (context(ViewRef) ViewConfigBuilder.() -> Unit)? = null

    @PublishedApi
    internal var onUpdate: (context(ViewRef) Context.() -> Unit)? = null

    @PublishedApi
    internal var onOpen: (context(ViewRef) OpenContext.() -> Unit)? = null

    @PublishedApi
    internal var onFirstRender: (context(ViewRef) (RenderContext).() -> Unit)? = null

    @PublishedApi
    internal var onClick: (context(ViewRef) SlotClickContext.() -> Unit)? = null

    @PublishedApi
    internal var onClose: (context(ViewRef) CloseContext.() -> Unit)? = null

    @PublishedApi
    internal var containerDefaults: (context (ViewContainerModificationContext, ViewRef) () -> Unit)? = null
}

/**
 * Context object for a simple (non-paginated) Surf view.
 *
 * Holds the [SurfViewSettings] used to configure the view's layout and behaviour.
 * Created internally by [surfView] and populated by the `settings { }` DSL function.
 *
 * @see surfView
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SimpleViewSettings
 */
@InventoryFramworkDSL
class SurfViewContext @PublishedApi internal constructor() : AbstractSurfViewContext<SurfViewRef>() {
    @PublishedApi
    internal var settings: SurfViewSettings = SimpleViewSettings()
}

/**
 * Context object for a paginated Surf view.
 *
 * Extends [AbstractSurfViewContext] with pagination-specific fields:
 * - [layoutTarget]: the layout character that marks pagination slots (e.g. `'I'`)
 * - [paginationStateBuilder]: the factory that creates the [PaginationStateBuilder]
 * - [settings]: the [PaginatedViewSettings] controlling layout rows and button positions
 *
 * Created internally by [paginatedSurfView] and populated by the `settings`, `layoutTarget`,
 * and `pagination` DSL functions.
 *
 * @see paginatedSurfView
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.PaginatedViewSettings
 */
@InventoryFramworkDSL
class PaginatedSurfViewContext @PublishedApi internal constructor() : AbstractSurfViewContext<PaginatedSurfViewRef>() {
    @PublishedApi
    internal var settings: PaginatedViewSettings = PaginatedViewSettings()

    @PublishedApi
    internal var layoutTarget: Char? = null

    @PublishedApi
    internal var paginationStateBuilder: ((AbstractPaginatedSurfView) -> PaginationStateBuilder<Context, BukkitItemComponentBuilder, *>)? =
        null

    /**
     * Returns the registered layout target character.
     *
     * @throws IllegalStateException if no layout target has been configured via [layoutTarget]
     */
    fun getRegisteredLayoutTarget(): Char {
        check(layoutTarget != null) {
            "Missing layout target for paginated view. " +
                    "Please specify a layout target using the 'layoutTarget' function in the view configuration."
        }
        return layoutTarget!!
    }

    /**
     * Returns the registered pagination state builder factory.
     *
     * @throws IllegalStateException if no pagination has been configured via [pagination][dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.pagination]
     */
    fun getRegisteredPaginationStateBuilder(): (AbstractPaginatedSurfView) -> PaginationStateBuilder<Context, BukkitItemComponentBuilder, *> {
        check(paginationStateBuilder != null) {
            "Missing pagination configuration for paginated view. " +
                    "Please specify pagination using one of the pagination functions (pagination, computedPagination, etc.) in the view configuration."
        }
        return paginationStateBuilder!!
    }
}
