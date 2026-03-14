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

@InventoryFramworkDSL
class SurfViewContext @PublishedApi internal constructor() : AbstractSurfViewContext<SurfViewRef>() {
    @PublishedApi
    internal var settings: SurfViewSettings = SimpleViewSettings()
}

@InventoryFramworkDSL
class PaginatedSurfViewContext @PublishedApi internal constructor() : AbstractSurfViewContext<PaginatedSurfViewRef>() {
    @PublishedApi
    internal var settings: PaginatedViewSettings = PaginatedViewSettings()

    @PublishedApi
    internal var layoutTarget: Char? = null

    @PublishedApi
    internal var paginationStateBuilder: ((AbstractPaginatedSurfView) -> PaginationStateBuilder<Context, BukkitItemComponentBuilder, *>)? =
        null

    fun getRegisteredLayoutTarget(): Char {
        check(layoutTarget != null) {
            "Missing layout target for paginated view. " +
                    "Please specify a layout target using the 'layoutTarget' function in the view configuration."
        }
        return layoutTarget!!
    }

    fun getRegisteredPaginationStateBuilder(): (AbstractPaginatedSurfView) -> PaginationStateBuilder<Context, BukkitItemComponentBuilder, *> {
        check(paginationStateBuilder != null) {
            "Missing pagination configuration for paginated view. " +
                    "Please specify pagination using one of the pagination functions (pagination, computedPagination, etc.) in the view configuration."
        }
        return paginationStateBuilder!!
    }
}
