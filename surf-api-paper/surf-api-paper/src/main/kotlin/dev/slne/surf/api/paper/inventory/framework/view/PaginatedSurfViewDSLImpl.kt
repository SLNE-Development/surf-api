package dev.slne.surf.api.paper.inventory.framework.view

import dev.slne.surf.api.paper.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.context.*

/**
 * The concrete implementation of a DSL-configured paginated Surf view.
 *
 * [PaginatedSurfViewDSLImpl] is created by [paginatedSurfView] and wires the lifecycle
 * callbacks from [PaginatedSurfViewContext] to the corresponding hooks in
 * [AbstractPaginatedSurfView]. It also resolves all [DeferredState] entries registered
 * during the DSL block into actual IF state objects at construction time.
 *
 * This class is not intended to be used or subclassed directly; use [paginatedSurfView] or [AbstractPaginatedSurfView] instead.
 *
 * @param header the plain-text inventory title
 * @param ctx the [PaginatedSurfViewContext] holding lifecycle callbacks and pagination config
 * @param ref the [PaginatedSurfViewRef] that will be resolved once the view is built
 * @see paginatedSurfView
 * @see AbstractPaginatedSurfView
 * @see PaginatedSurfViewContext
 */
abstract class PaginatedSurfViewDSLImpl @PublishedApi internal constructor(
    header: String,
    private val ctx: PaginatedSurfViewContext,
    private val ref: PaginatedSurfViewRef,
) : AbstractPaginatedSurfView(header) {
    override val layoutTarget: Char = ctx.getRegisteredLayoutTarget()
    override val settings get() = ctx.settings

    init {
        ctx.stateRegistry.resolveStates(this)
    }

    override fun createPagination(): PaginationStateBuilder<Context, BukkitItemComponentBuilder, *> {
        return ctx.getRegisteredPaginationStateBuilder()(this)
    }

    override fun onPaginatedInit(config: ViewConfigBuilder) {
        ctx.onInit?.invoke(ref, config)
    }

    override fun onPaginatedUpdate(update: Context) {
        ctx.onUpdate?.invoke(ref, update)
    }

    override fun onPaginatedOpen(open: OpenContext) {
        ctx.onOpen?.invoke(ref, open)
    }

    override fun onPaginatedRender(render: RenderContext) {
        ctx.onFirstRender?.invoke(ref, render)
    }

    override fun onPaginatedClick(click: SlotClickContext) {
        ctx.onClick?.invoke(ref, click)
    }

    override fun onPaginatedClose(close: CloseContext) {
        ctx.onClose?.invoke(ref, close)
    }

    context(modificationCtx: ViewContainerModificationContext)
    override fun applyContainerDefaults() {
        ctx.containerDefaults?.invoke(modificationCtx, ref)
    }

    internal fun modifyContainer0(
        updateContext: Context,
        block: context(ViewContainerModificationContext) () -> Unit
    ) = modifyContainer(updateContext, block)
}
